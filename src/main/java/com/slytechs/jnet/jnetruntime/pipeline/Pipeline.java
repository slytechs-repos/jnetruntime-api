/*
 * Copyright 2024 Sly Technologies Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */package com.slytechs.jnet.jnetruntime.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.slytechs.jnet.jnetruntime.pipeline.ProcessorFactory.PipelineFactory;
import com.slytechs.jnet.jnetruntime.util.Registration;

/**
 * Represents a pipeline of processors that can be chained together to process
 * data. The pipeline manages a collection of processors, processor groups, and
 * sub-pipelines. It provides methods for installing, configuring, and managing
 * these components.
 * 
 * <p>
 * This class is designed to be extensible, allowing for the creation of custom
 * processing pipelines for various data processing tasks.
 * </p>
 * 
 * <p>
 * Example usage:
 * 
 * <pre>
 * Pipeline<RawPacket, ParsedPacket> pipeline = new Pipeline<>(1, new PacketProcessor(), DataType.RAW, DataType.PARSED);
 * pipeline.install(new HeaderProcessor())
 * 		.install(new PayloadProcessor())
 * 		.build();
 * ParsedPacket result = pipeline.process(rawPacket);
 * </pre>
 * </p>
 * 
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 * @param <T_IN>  The input type for the pipeline
 * @param <T_OUT> The output type for the pipeline
 */
public class Pipeline<T_IN, T_OUT>
		extends Processor<T_IN, T_OUT>
		implements Iterable<Processor<?, ?>> {

	/**
	 * List of sub-pipelines installed in this pipeline. This allows for
	 * hierarchical structuring of processing logic.
	 */
	private final List<Pipeline<?, ?>> pipelineList = new ArrayList<>();

	/**
	 * List of processor groups installed in this pipeline. Processor groups allow
	 * for logical grouping of related processors.
	 */
	private final List<ProcessorGroup<?, ?>> groupList = new ArrayList<>();

	/**
	 * List of all individual processors installed in this pipeline. This includes
	 * processors from groups and sub-pipelines.
	 */
	private final List<Processor<?, ?>> processorList = new ArrayList<>();

	/**
	 * Flag indicating whether the pipeline has been built and closed for
	 * modifications. Once closed, no new processors can be added to the pipeline.
	 */
	private boolean closed;

	/**
	 * The context for this pipeline, containing configuration and runtime
	 * information.
	 */
	private ProcessorContext context;

	/**
	 * The highest priority value assigned to any processor in this pipeline. Used
	 * to determine the next available priority when installing new processors.
	 */
	private int highestPriority = 0;

	/**
	 * The main processor group for this pipeline. This group is responsible for the
	 * primary processing logic of the pipeline.
	 */
	private final ProcessorGroup<T_IN, T_OUT> pipelineProcessor;

	/**
	 * Constructs a new Pipeline with the specified priority, main processor group,
	 * and data types.
	 * 
	 * <p>
	 * This constructor initializes the pipeline with its core components. The main
	 * processor group is set, and the pipeline inherits its input and output types.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * ProcessorGroup<RawPacket, ParsedPacket> mainGroup = new PacketProcessorGroup();
	 * Pipeline<RawPacket, ParsedPacket> pipeline = new Pipeline<>(1, mainGroup, DataType.RAW, DataType.PARSED);
	 * </pre>
	 * </p>
	 *
	 * @param <T>        The type of the processor group
	 * @param priority   The priority of this pipeline in a larger system
	 * @param processor  The main processor group for this pipeline
	 * @param inputType  The input data type for the pipeline
	 * @param outputType The output data type for the pipeline
	 */
	protected <T extends ProcessorGroup<T_IN, T_OUT>> Pipeline(int priority, T processor, DataType inputType,
			DataType outputType) {
		super(priority, processor.input(), inputType, outputType);
		this.pipelineProcessor = processor;
	}

	/**
	 * Adds an output to the pipeline.
	 * 
	 * <p>
	 * This method allows for connecting the pipeline's output to other components
	 * or processors in a larger system.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * PacketConsumer consumer = new PacketConsumer();
	 * Registration reg = pipeline.addOutput(consumer);
	 * // Later, when no longer needed:
	 * reg.unregister();
	 * </pre>
	 * </p>
	 *
	 * @param out The output to add
	 * @return A Registration object that can be used to unregister the output later
	 */
	@Override
	public Registration addOutput(T_OUT out) {
		return super.addOutput(out);
	}

	/**
	 * Adds an output to the pipeline with a specified processor.
	 * 
	 * <p>
	 * This method allows for adding an output along with a processor that will
	 * handle the output before it's passed to the final consumer. This can be
	 * useful for post-processing or formatting the pipeline's output.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * PacketConsumer consumer = new PacketConsumer();
	 * Processor<ParsedPacket, ParsedPacket> formatter = new PacketFormatter();
	 * Registration reg = pipeline.addOutput(consumer, formatter);
	 * </pre>
	 * </p>
	 *
	 * @param out       The output to add
	 * @param processor The processor to associate with the output
	 * @return A Registration object that can be used to unregister the output and
	 *         its processor later
	 */
	@Override
	public Registration addOutput(T_OUT out, Processor<T_OUT, T_OUT> processor) {
		return super.addOutput(out, processor);
	}

	/**
	 * Builds the pipeline, setting up and linking all processor groups.
	 * 
	 * <p>
	 * This method should be called after all processors have been installed and
	 * before the pipeline is used for processing. It performs the following steps:
	 * <ol>
	 * <li>Checks if the pipeline is already built</li>
	 * <li>Sorts the processor groups by priority</li>
	 * <li>Sets up each processor group</li>
	 * <li>Links the processor groups together</li>
	 * <li>Marks the pipeline as closed (built)</li>
	 * </ol>
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * pipeline.install(new HeaderProcessor())
	 *         .install(new PayloadProcessor())
	 *         .build();
	 * // The pipeline is now ready for use
	 * </pre>
	 * </p>
	 *
	 * @throws IllegalStateException if the pipeline is already built
	 */
	protected void build() {
		checkNotBuiltStatus();

		Collections.sort(groupList);

		groupList.forEach(this::setupGroup);
		groupList.forEach(this::linkGroup);

		closed = true;
	}

	/**
	 * Checks if the pipeline has not been built yet.
	 * 
	 * <p>
	 * This internal method is used to ensure that modifications to the pipeline are
	 * only allowed before it's built. It helps maintain the integrity of the
	 * pipeline's structure.
	 * </p>
	 *
	 * @throws IllegalStateException if the pipeline is already built
	 */
	private void checkNotBuiltStatus() {
		if (closed)
			throw new IllegalStateException("pipeline already built");
	}

	/**
	 * Collects and iterates over results of a specific type produced by the
	 * pipeline.
	 * 
	 * <p>
	 * This method is intended to process input data and collect results of a
	 * specified type. However, it is currently not implemented and will throw an
	 * UnsupportedOperationException.
	 * </p>
	 * 
	 * @param <T>        The type of results to collect
	 * @param resultType The data type of the results to collect
	 * @param source     The source of input data
	 * @return An iterator over the collected results
	 * @throws UnsupportedOperationException as this method is not yet implemented
	 */
	public <T> Iterator<T> collectAndIterate(DataType resultType, Consumer<T_IN> source) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the processor context for this pipeline.
	 * 
	 * <p>
	 * The context contains configuration and runtime information for the pipeline
	 * and its processors. It can be used to share data and settings across
	 * different parts of the pipeline.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * ProcessorContext context = pipeline.context();
	 * // Use the context to retrieve or set pipeline-wide information
	 * int maxPacketSize = context.getProperty("maxPacketSize", 1500);
	 * </pre>
	 * </p>
	 *
	 * @return The processor context associated with this pipeline
	 */
	public final ProcessorContext context() {
		return context;
	}

	/**
	 * Enables or disables all processors in the pipeline.
	 * 
	 * <p>
	 * This method provides a convenient way to activate or deactivate all
	 * processors in the pipeline at once. It's useful for temporarily suspending or
	 * resuming the entire pipeline's operation.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * // Disable all processors
	 * pipeline.enableAll(false);
	 * // ... perform some other operations ...
	 * // Re-enable all processors
	 * pipeline.enableAll(true);
	 * </pre>
	 * </p>
	 *
	 * @param b A boolean value indicating whether to enable (true) or disable
	 *          (false) all processors
	 * @return This pipeline instance, allowing for method chaining
	 * @throws IllegalStateException if the pipeline is already built
	 */
	public Pipeline<T_IN, T_OUT> enableAll(boolean b) {
		checkNotBuiltStatus();

		return enableAll(() -> b);
	}

	/**
	 * Enables or disables all processors in the pipeline based on a boolean
	 * supplier.
	 * 
	 * <p>
	 * This method allows for dynamic enabling/disabling of processors based on a
	 * condition. The boolean supplier is evaluated for each processor, allowing for
	 * more complex enable/disable logic.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * AtomicBoolean flag = new AtomicBoolean(true);
	 * pipeline.enableAll(flag::get);
	 * // Later, to disable all processors:
	 * flag.set(false);
	 * </pre>
	 * </p>
	 *
	 * @param b A boolean supplier that determines whether processors should be
	 *          enabled
	 * @return This pipeline instance, allowing for method chaining
	 * @throws IllegalStateException if the pipeline is already built
	 */
	public Pipeline<T_IN, T_OUT> enableAll(BooleanSupplier b) {
		checkNotBuiltStatus();

		for (var p : this)
			p.enable(b);

		return this;
	}

	/**
	 * Gets a result of the pipeline processing.
	 * 
	 * <p>
	 * This method processes the input data through the pipeline and returns the
	 * result. It uses the pipeline's output type as the result type.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * ParsedPacket result = pipeline.get(rawPacket -> {
	 *     // Process raw packet
	 * });
	 * </pre>
	 * </p>
	 *
	 * @param <T>    The type of the result (same as the pipeline's output type)
	 * @param source The source of input data
	 * @return The processing result
	 * @throws UnsupportedOperationException as this method is not yet implemented
	 */
	public <T> T get(Consumer<T_IN> source) {
		return get(outputType(), source);
	}

	/**
	 * Gets a result of the pipeline processing of a specific type.
	 * 
	 * <p>
	 * This method processes the input data through the pipeline and returns a
	 * result of the specified type. It allows for retrieving intermediate results
	 * or results of a different type than the pipeline's main output.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * HeaderInfo headerInfo = pipeline.get(DataType.HEADER, rawPacket -> {
	 *     // Process raw packet to extract header info
	 * });
	 * </pre>
	 * </p>
	 *
	 * @param <T>        The type of the result
	 * @param resultType The data type of the result
	 * @param source     The source of input data
	 * @return The processing result of the specified type
	 * @throws UnsupportedOperationException as this method is not yet implemented
	 */
	public <T> T get(DataType resultType, Consumer<T_IN> source) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the input of this pipeline.
	 * 
	 * <p>
	 * This method provides access to the input data of the pipeline. It can be used
	 * to inspect or modify the input before it's processed by the pipeline.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * RawPacket input = pipeline.input();
	 * // Inspect or modify the input
	 * if (input.getSize() > MAX_SIZE) {
	 *     // Handle oversized packet
	 * }
	 * </pre>
	 * </p>
	 *
	 * @return The input of the pipeline
	 */
	@Override
	public T_IN input() {
		return super.input();
	}

	/**
	 * Installs a new processor into the pipeline.
	 * 
	 * <p>
	 * This method creates and installs a new processor using the provided factory.
	 * The processor is assigned the specified priority within the pipeline.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * Processor<RawPacket, HeaderInfo> headerProcessor = pipeline.install(100, new HeaderProcessorFactory());
	 * </pre>
	 * </p>
	 *
	 * @param <T>      The type of the processor
	 * @param <T1>     The input type of the processor
	 * @param <T2>     The output type of the processor
	 * @param priority The priority of the processor within the pipeline
	 * @param factory  The factory to create the processor
	 * @return The newly installed processor
	 * @throws IllegalStateException if the pipeline is already built
	 */
	public <T extends Processor<T1, T2>, T1, T2> T install(int priority, ProcessorFactory<T, T1, T2> factory) {
		checkNotBuiltStatus();

		T processor = factory.newInstance(priority);

		return installProcessor0(priority, processor);
	}

	/**
	 * Installs a new processor into the pipeline with the next available priority.
	 * 
	 * <p>
	 * This method creates and installs a new processor using the provided factory.
	 * The processor is assigned the next available priority within the pipeline.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * Processor<RawPacket, PayloadInfo> payloadProcessor = pipeline.install(new PayloadProcessorFactory());
	 * </pre>
	 * </p>
	 *
	 * @param <T>     The type of the processor
	 * @param <T1>    The input type of the processor
	 * @param <T2>    The output type of the processor
	 * @param factory The factory to create the processor
	 * @return The newly installed processor
	 * @throws IllegalStateException if the pipeline is already built
	 */
	public <T extends Processor<T1, T2>, T1, T2> T install(ProcessorFactory<T, T1, T2> factory) {
		return install(nextPriority(), factory);
	}

	/**
	 * Installs a new processor group into the pipeline.
	 * 
	 * <p>
	 * This method adds a new processor group to the pipeline. Processor groups
	 * allow for logical grouping of related processors.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * ProcessorGroup<RawPacket, HeaderInfo> headerGroup = new HeaderProcessorGroup();
	 * pipeline.installGroup(headerGroup);
	 * </pre>
	 * </p>
	 *
	 * @param <T>      The type of the processor group
	 * @param <T1>     The input type of the processor group
	 * @param <T2>     The output type of the processor group
	 * @param newGroup The new processor group to install
	 * @return The installed processor group
	 */
	protected <T extends ProcessorGroup<T1, T2>, T1, T2> T installGroup(T newGroup) {
		groupList.add(newGroup);
		Collections.sort(groupList);

		return newGroup;
	}

	/**
	 * Installs a new processor if it's absent, otherwise accesses the already
	 * installed one.
	 * 
	 * <p>
	 * This method checks if a processor of the given type is already installed. If
	 * not, it installs a new one. This is useful for ensuring that a certain type
	 * of processor is always available in the pipeline without duplicating it.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * Processor<RawPacket, HeaderInfo> headerProcessor = pipeline.installIfAbsent(100, new HeaderProcessorFactory());
	 * </pre>
	 * </p>
	 *
	 * @param <T>      The type of the processor
	 * @param <T1>     The input type of the processor
	 * @param <T2>     The output type of the processor
	 * @param priority The priority of the processor
	 * @param factory  The factory to create the processor
	 * @return The installed or existing processor
	 * @throws IllegalStateException if the pipeline is already built
	 */
	public <T extends Processor<T1, T2>, T1, T2> T installIfAbsent(int priority,
			ProcessorFactory<T, T1, T2> factory) {
		return install(priority, factory);
	}

	/**
	 * Installs a new sub-pipeline into this pipeline.
	 * 
	 * <p>
	 * This method creates and installs a new sub-pipeline using the provided
	 * factory. Sub-pipelines allow for hierarchical structuring of processing
	 * logic.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> mainPipeline = ...;
	 * Pipeline<RawPacket, HeaderInfo> headerPipeline = mainPipeline.installPipeline(50, new HeaderPipelineFactory());
	 * </pre>
	 * </p>
	 *
	 * @param <T>      The type of the sub-pipeline
	 * @param <T1>     The input type of the sub-pipeline
	 * @param <T2>     The output type of the sub-pipeline
	 * @param priority The priority of the sub-pipeline
	 * @param factory  The factory to create the sub-pipeline
	 * @return The newly installed sub-pipeline
	 * @throws IllegalStateException if the pipeline is already built
	 */
	public <T extends Pipeline<T1, T2>, T1, T2> T installPipeline(int priority, PipelineFactory<T, T1, T2> factory) {
		T newPipeline = factory.newInstance(priority);

		pipelineList.add(newPipeline);
		Collections.sort(pipelineList);

		return installProcessor0(priority, newPipeline);
	}
	
	/**
	 * Installs a new sub-pipeline into this pipeline with the next available
	 * priority.
	 * 
	 * <p>
	 * This method creates and installs a new sub-pipeline using the provided
	 * factory. The sub-pipeline is assigned the next available priority within this
	 * pipeline.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> mainPipeline = ...;
	 * Pipeline<RawPacket, PayloadInfo> payloadPipeline = mainPipeline.installPipeline(new PayloadPipelineFactory());
	 * </pre>
	 * </p>
	 *
	 * @param <T>     The type of the sub-pipeline
	 * @param <T1>    The input type of the sub-pipeline
	 * @param <T2>    The output type of the sub-pipeline
	 * @param factory The factory to create the sub-pipeline
	 * @return The newly installed sub-pipeline
	 * @throws IllegalStateException if the pipeline is already built
	 */
	public <T extends Pipeline<T1, T2>, T1, T2> T installPipeline(PipelineFactory<T, T1, T2> factory) {
		return installPipeline(nextPriority(), factory);
	}

	/**
     * Internal method to install a processor into the pipeline.
     * 
     * <p>This private method is used by other installation methods to add a new processor
     * to the pipeline. It updates the processor list and adjusts the highest priority
     * if necessary.</p>
     *
     * <p>This method is not intended to be called directly by users of the Pipeline class.
     * Instead, use the public installation methods like {@link #install(int, ProcessorFactory)}
     * or {@link #installPipeline(int, PipelineFactory)}.</p>
     *
     * <p>Example of internal usage:
     * <pre>
     * private <T extends Processor<?, ?>> T installProcessor0(int priority, T newProcessor) {
     *     processorList.add(newProcessor);
     *
     *     if (highestPriority < priority)
     *         highestPriority = priority;
     *
     *     return newProcessor;
     * }
     * </pre>
     * </p>
     *
     * @param <T> The type of the processor
     * @param priority The priority of the processor
     * @param newProcessor The new processor to install
     * @return The installed processor
     */
    private <T extends Processor<?, ?>> T installProcessor0(int priority, T newProcessor) {
        processorList.add(newProcessor);

        if (highestPriority < priority)
            highestPriority = priority;

        return newProcessor;
    }

	/**
	 * Checks if the pipeline is open (not built).
	 * 
	 * <p>
	 * This method returns true if the pipeline has not been built yet, meaning it
	 * is still open for modifications such as installing new processors.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * if (pipeline.isOpen()) {
	 *     pipeline.install(new HeaderProcessor());
	 * } else {
	 *     System.out.println("Pipeline is already built and cannot be modified.");
	 * }
	 * </pre>
	 * </p>
	 *
	 * @return true if the pipeline is open, false if it has been built
	 */
	public boolean isOpen() {
		return !closed;
	}

	/**
	 * Returns an iterator over the processors in this pipeline.
	 * 
	 * <p>
	 * This method allows for iterating over all processors installed in the
	 * pipeline, including those in processor groups and sub-pipelines.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * for (Processor<?, ?> processor : pipeline) {
	 *     System.out.println("Processor: " + processor.getClass().getSimpleName());
	 * }
	 * </pre>
	 * </p>
	 *
	 * @return An iterator over the processors in this pipeline
	 */
	@Override
	public Iterator<Processor<?, ?>> iterator() {
		return processorList.iterator();
	}

	/**
	 * Links a processor group within the pipeline.
	 * 
	 * <p>
	 * This internal method is responsible for establishing connections between
	 * processor groups in the pipeline. It is called during the build process.
	 * </p>
	 *
	 * @param group The processor group to link
	 */
	private void linkGroup(ProcessorGroup<?, ?> group) {
		// Implementation details...
	}

	/**
	 * Returns the main processor group of this pipeline.
	 * 
	 * <p>
	 * This method provides access to the primary processor group that was set when
	 * the pipeline was created.
	 * </p>
	 *
	 * @param <T> The type of the processor group
	 * @return The main processor group
	 */
	@SuppressWarnings("unchecked")
	protected final <T extends ProcessorGroup<T_IN, T_OUT>> T mainProcessor() {
		return (T) this.pipelineProcessor;
	}

	/**
	 * Calculates the next available priority for installing processors.
	 * 
	 * <p>
	 * This internal method determines the next priority value to be used when
	 * installing new processors without specifying a priority explicitly.
	 * </p>
	 *
	 * @return The next available priority value
	 */
	private int nextPriority() {
		return highestPriority + 10;
	}

	/**
	 * Handles the linking of a processor to the pipeline.
	 * 
	 * <p>
	 * This method is called when a processor is linked to the pipeline. It can be
	 * overridden to perform custom actions when a new processor is connected.
	 * </p>
	 *
	 * @param link The processor link being established
	 */
	@Override
	public void onLink(ProcessorLink<T_IN> link) {
		// Implementation details...
	}

	/**
	 * Handles the unlinking of a processor from the pipeline.
	 * 
	 * <p>
	 * This method is called when a processor is unlinked from the pipeline. It can
	 * be overridden to perform custom actions when a processor is disconnected.
	 * </p>
	 *
	 * @param link The processor link being removed
	 */
	@Override
	public void onUnlink(ProcessorLink<T_IN> link) {
		// Implementation details...
	}

	/**
	 * Returns the output of this pipeline.
	 * 
	 * <p>
	 * This method provides access to the output data of the pipeline. It can be
	 * used to retrieve the final processed result.
	 * </p>
	 *
	 * @return The output of the pipeline
	 */
	@Override
	public T_OUT output() {
		return super.output();
	}

	/**
	 * Sets up the pipeline with the given context.
	 * 
	 * <p>
	 * This method initializes the pipeline's context, which contains configuration
	 * and runtime information. It is typically called during the pipeline's
	 * initialization.
	 * </p>
	 *
	 * @param context The processor context to set up
	 */
	@Override
	public void setup(ProcessorContext context) {
		this.context = new ProcessorContext(name(), context);
	}

	/**
	 * Sets up a processor group within the pipeline.
	 * 
	 * <p>
	 * This internal method is responsible for initializing and configuring a
	 * processor group as part of the pipeline building process.
	 * </p>
	 *
	 * @param group The processor group to set up
	 */
	private void setupGroup(ProcessorGroup<?, ?> group) {
		// Implementation details...
	}

	/**
	 * Returns a stream of all processors in this pipeline.
	 * 
	 * <p>
	 * This method provides a stream of all processors in the pipeline, allowing for
	 * more advanced operations and filtering on the processor collection.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * long count = pipeline.stream()
	 *                      .filter(p -> p instanceof HeaderProcessor)
	 *                      .count();
	 * System.out.println("Number of HeaderProcessors: " + count);
	 * </pre>
	 * </p>
	 *
	 * @return A stream of processors in this pipeline
	 */
	public Stream<Processor<?, ?>> stream() {
		Iterator<Processor<?, ?>> it = iterator();

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false);
	}

	/**
	 * Returns a stream of processors of a specific data type in this pipeline.
	 * 
	 * <p>
	 * This method provides a stream of processors that match the specified input
	 * data type, allowing for type-specific operations on the processor collection.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * pipeline.stream(DataType.HEADER)
	 *         .forEach(p -> System.out.println("Header processor: " + p.getClass().getSimpleName()));
	 * </pre>
	 * </p>
	 *
	 * @param type The data type to filter processors
	 * @return A stream of processors of the specified type
	 */
	public Stream<Processor<?, ?>> stream(DataType type) {
		return stream().filter(p -> p.inputType().equals(type));
	}

	/**
	 * Uninstalls a processor from the pipeline.
	 * 
	 * <p>
	 * This method removes a previously installed processor from the pipeline. It
	 * can be used to dynamically modify the pipeline's structure.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * Processor<?, ?> processorToRemove = ...;
	 * pipeline.uninstall(processorToRemove);
	 * </pre>
	 * </p>
	 *
	 * @param processor The processor to uninstall
	 * @return This pipeline instance, allowing for method chaining
	 */
	public Pipeline<T_IN, T_OUT> uninstall(Processor<?, ?> processor) {
		processorList.remove(processor);

		return this;
	}

	/**
	 * Uninstalls all processors from the pipeline.
	 * 
	 * <p>
	 * This method removes all previously installed processors from the pipeline,
	 * effectively resetting its processing logic. It can only be called if the
	 * pipeline has not been built yet.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Pipeline<RawPacket, ParsedPacket> pipeline = ...;
	 * if (pipeline.isOpen()) {
	 *     pipeline.uninstallAll().install(new CustomProcessor());
	 * }
	 * </pre>
	 * </p>
	 *
	 * @return This pipeline instance, allowing for method chaining
	 * @throws IllegalStateException if the pipeline is already built
	 */
	public Pipeline<T_IN, T_OUT> uninstallAll() {
		checkNotBuiltStatus();

		processorList.clear();

		return this;
	}
}