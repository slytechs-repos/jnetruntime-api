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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import com.slytechs.jnet.jnetruntime.pipeline.DataType.DataSupport;
import com.slytechs.jnet.jnetruntime.util.Registration;

/**
 * Represents a generic processor in a data processing pipeline.
 * 
 * <p>
 * This abstract class provides the foundation for creating specific processors
 * that can be chained together in a pipeline. Each processor has input and
 * output types, a priority for ordering, and can have multiple output channels.
 * </p>
 *
 * <p>
 * Example usage:
 * 
 * <pre>
 * public class PacketProcessor extends Processor&lt;RawPacket, ParsedPacket&gt; {
 * 	public PacketProcessor() {
 * 		super(100, DataType.RAW, DataType.PARSED);
 * 	}
 * 
 * 	// Implement processing logic
 * }
 * </pre>
 * </p>
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 * @param <T_IN>  The input type of the processor
 * @param <T_OUT> The output type of the processor
 */
public abstract class Processor<T_IN, T_OUT>
		implements Comparable<Processor<?, ?>> {

	/**
	 * The priority of this processor in the pipeline. Higher priority processors
	 * are executed earlier in the pipeline.
	 */
	private final int priority;

	/**
	 * The main data channel for this processor. Handles the primary input and
	 * output flow.
	 */
	private final DataChannel<T_IN, T_OUT> mainChannel;

	/**
	 * Map of additional data channels, keyed by their data type. Allows for
	 * multiple output types from a single processor.
	 */
	private final Map<DataType, DataChannel<?, ?>> subChannels = new HashMap<>();

	/**
	 * The name of this processor, used for identification and logging.
	 */
	private String name;

	/**
	 * The current output of this processor.
	 */
	protected T_OUT output;

	/**
	 * Constructs a new Processor with the specified priority and data types.
	 * 
	 * <p>
	 * This constructor is typically used when the processor itself implements the
	 * input interface.
	 * </p>
	 *
	 * @param priority   The priority of this processor in the pipeline
	 * @param inputType  The type of input data this processor accepts
	 * @param outputType The type of output data this processor produces
	 */
	@SuppressWarnings("unchecked")
	protected Processor(int priority, DataType inputType, DataType outputType) {
		this.priority = priority;
		T_IN inline = (T_IN) this; // Force subclassed processors implement T_IN interface
		this.name = getClass().getSimpleName();
		this.mainChannel = new DataChannel<>(name, inputType, outputType, inline);

		this.mainChannel.updateListener(newOut -> this.output = newOut);
	}

	/**
	 * Constructs a new Processor with the specified priority, input object, and
	 * data types.
	 * 
	 * <p>
	 * This constructor is used when the input is provided as a separate object.
	 * </p>
	 *
	 * @param priority   The priority of this processor in the pipeline
	 * @param inline     The input object that this processor will use
	 * @param inputType  The type of input data this processor accepts
	 * @param outputType The type of output data this processor produces
	 */
	protected Processor(int priority, T_IN inline, DataType inputType, DataType outputType) {
		this.priority = priority;
		this.name = getClass().getSimpleName();
		this.mainChannel = new DataChannel<>(name, inputType, outputType, Objects.requireNonNull(inline, "inline"));

		this.mainChannel.updateListener(newOut -> this.output = newOut);
	}

	/**
	 * Adds an output sink to this processor.
	 * 
	 * <p>
	 * This method allows connecting the output of this processor to another
	 * component in the pipeline.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Processor&lt;RawPacket, ParsedPacket&gt; processor = ...;
	 * ParsedPacketConsumer consumer = ...;
	 * Registration reg = processor.addOutput(consumer);
	 * // Later, to remove the output:
	 * reg.unregister();
	 * </pre>
	 * </p>
	 *
	 * @param out The output sink to add
	 * @return A Registration object that can be used to unregister the output
	 */
	protected Registration addOutput(T_OUT out) {
		return mainChannel.sink(out);
	}

	/**
	 * Adds an output processor to this processor.
	 * 
	 * <p>
	 * This method allows chaining another processor to the output of this
	 * processor.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Processor&lt;RawPacket, ParsedPacket&gt; processor1 = ...;
	 * Processor&lt;ParsedPacket, AnalyzedPacket&gt; processor2 = ...;
	 * Registration reg = processor1.addOutput(null, processor2);
	 * </pre>
	 * </p>
	 *
	 * @param out       The output object (can be null if not needed)
	 * @param processor The processor to connect to the output of this processor
	 * @return A Registration object that can be used to unregister the connection
	 */
	protected Registration addOutput(T_OUT out, Processor<T_OUT, T_OUT> processor) {
		return mainChannel.connect(processor.mainChannel);
	}

	/**
	 * Links a sub-channel of this processor to an output.
	 * 
	 * <p>
	 * This method is used to connect secondary output types to their respective
	 * consumers.
	 * </p>
	 *
	 * @param <T_SUB>    The type of the sub-channel output
	 * @param type       The data type of the sub-channel
	 * @param subChannel The output sink for the sub-channel
	 * @return A Registration object that can be used to unregister the sub-channel
	 * @throws IllegalArgumentException if the specified sub-channel does not exist
	 */
	@SuppressWarnings("unchecked")
	protected <T_SUB> Registration linkSubChannel(DataType type, T_SUB subChannel) {
		if (!subChannels.containsKey(type))
			throw new IllegalArgumentException("sub-channel not found [%s]".formatted(type.toString()));

		DataChannel<?, T_SUB> sub = (DataChannel<?, T_SUB>) subChannels.get(type);

		return sub.sink(subChannel);
	}

	/**
	 * Returns the main data channel of this processor.
	 * 
	 * <p>
	 * This method provides access to the primary input/output channel of the
	 * processor.
	 * </p>
	 *
	 * @return The main DataChannel of this processor
	 */
	protected final DataChannel<T_IN, T_OUT> channel() {
		return this.mainChannel;
	}

	/**
	 * Compares this processor to another based on their priorities.
	 * 
	 * <p>
	 * This method is used to order processors in a pipeline.
	 * </p>
	 *
	 * @param o The other processor to compare to
	 * @return A negative integer, zero, or a positive integer as this processor has
	 *         less than, equal to, or greater than priority compared to the
	 *         specified processor.
	 */
	@Override
	public final int compareTo(Processor<?, ?> o) {
		return priority() - o.priority();
	}

	/**
	 * Creates a sub-channel for this processor.
	 * 
	 * <p>
	 * This method allows the processor to handle multiple output types.
	 * </p>
	 *
	 * @param <T_SUB>    The type of the sub-channel output
	 * @param subChannel The sub-channel to create
	 * @throws IllegalArgumentException if a sub-channel with the same output type
	 *                                  already exists
	 */
	protected final <T_SUB> void createSubChannel(DataChannel<?, T_SUB> subChannel) {
		if (subChannels.containsKey(subChannel.outputType()))
			throw new IllegalArgumentException("duplicate sub-channel %s".formatted(subChannel.name()));

		subChannels.put(subChannel.outputType(), subChannel);
	}

	/**
	 * Destroys this processor, releasing any resources it holds.
	 * 
	 * <p>
	 * This method can be overridden to perform cleanup operations when the
	 * processor is no longer needed.
	 * </p>
	 */
	protected void destroy() {
	}

	/**
	 * Enables or disables this processor.
	 * 
	 * <p>
	 * When disabled, the processor will not process any input or produce any
	 * output.
	 * </p>
	 *
	 * @param b true to enable the processor, false to disable it
	 * @return This processor instance, allowing for method chaining
	 */
	public Processor<T_IN, T_OUT> enable(boolean b) {
		this.mainChannel.enable(b);
		this.subChannels.values().forEach(s -> s.enable(b));

		return this;
	}

	/**
	 * Enables or disables this processor based on a boolean supplier.
	 * 
	 * <p>
	 * This method allows for dynamic enabling/disabling of the processor.
	 * </p>
	 *
	 * @param b A BooleanSupplier that determines whether the processor should be
	 *          enabled
	 * @return This processor instance, allowing for method chaining
	 * @throws NullPointerException if the supplied BooleanSupplier is null
	 */
	public final Processor<T_IN, T_OUT> enable(BooleanSupplier b) {
		var enabled = Objects.requireNonNull(b, "enabled").getAsBoolean();

		return enable(enabled);
	}

	/**
	 * Processes the input and sends the result to the specified output.
	 * 
	 * <p>
	 * This method is typically used in a loop to process multiple inputs.
	 * </p>
	 *
	 * @param out The output to send the processed result to
	 */
	public void forEach(T_OUT out) {
		peek(out);
	}

	/**
	 * Processes the input and sends the result to the specified output, with a user
	 * object.
	 * 
	 * <p>
	 * This method allows passing additional user data along with the output.
	 * </p>
	 *
	 * @param <U>  The type of the user object
	 * @param out  The output to send the processed result to
	 * @param user The user object to associate with the output
	 */
	public <U> void forEach(T_OUT out, U user) {
		peek(out, user);
	}

	/**
	 * Returns the input of this processor.
	 * 
	 * @return The input object of this processor
	 */
	T_IN input() {
		return mainChannel.input();
	}

	/**
	 * Returns the input type of this processor.
	 * 
	 * @return The DataType of the input this processor accepts
	 */
	public final DataType inputType() {
		return mainChannel.inputType();
	}

	/**
	 * Checks if this processor is currently enabled.
	 * 
	 * @return true if the processor is enabled, false otherwise
	 */
	public final boolean isEnabled() {
		return mainChannel.isEnabled();
	}

	/**
	 * Returns the name of this processor.
	 * 
	 * @return The name of this processor
	 */
	public final String name() {
		return name;
	}

	/**
	 * Sets a new name for this processor.
	 * 
	 * @param newName The new name to set for this processor
	 * @return This processor instance, allowing for method chaining
	 */
	public final Processor<T_IN, T_OUT> name(String newName) {
		this.name = newName;

		return this;
	}

	/**
	 * Called when this processor is linked to another component in the pipeline.
	 * 
	 * <p>
	 * This method can be overridden to perform setup operations when the processor
	 * is connected to the pipeline.
	 * </p>
	 *
	 * @param link The ProcessorLink object representing the connection
	 */
	protected void onLink(ProcessorLink<T_IN> link) {
	}

	/**
	 * Called when this processor is unlinked from another component in the
	 * pipeline.
	 * 
	 * <p>
	 * This method can be overridden to perform cleanup operations when the
	 * processor is disconnected from the pipeline.
	 * </p>
	 *
	 * @param link The ProcessorLink object representing the connection being
	 *             removed
	 */
	protected void onUnlink(ProcessorLink<T_IN> link) {
	}

	/**
	 * Returns the current output of this processor.
	 * 
	 * @return The current output object of this processor
	 */
	T_OUT output() {
		return output;
	}

	/**
	 * Returns the output type of this processor.
	 * 
	 * @return The DataType of the output this processor produces
	 */
	public final DataType outputType() {
		return mainChannel.outputType();
	}

	/**
	 * Adds an output and immediately processes the current input.
	 * 
	 * <p>
	 * This method is useful for quick, one-time processing operations.
	 * </p>
	 *
	 * @param out The output to send the processed result to
	 * @return This processor instance, allowing for method chaining
	 */
	public Processor<T_IN, T_OUT> peek(T_OUT out) {
		addOutput(out);

		return this;
	}

	/**
	 * Adds an output with a user object and immediately processes the current
	 * input.
	 * 
	 * <p>
	 * This method allows passing additional user data along with the output.
	 * </p>
	 *
	 * @param <U>  The type of the user object
	 * @param out  The output to send the processed result to
	 * @param user The user object to associate with the output
	 * @return This processor instance, allowing for method chaining
	 */
	public <U> Processor<T_IN, T_OUT> peek(T_OUT out, U user) {
		DataSupport<T_OUT> support = outputType().dataSupport();
		return peek(support.wrap(out, user));
	}

	/**
	 * Returns the priority of this processor.
	 * 
	 * @return The priority value of this processor
	 */
	public final int priority() {
		return priority;
	}

	/**
	 * Sets up this processor with the given context.
	 * 
	 * <p>
	 * This method can be overridden to perform initialization operations when the
	 * processor is being set up in the pipeline.
	 * </p>
	 *
	 * @param context The ProcessorContext containing configuration and runtime
	 *                information
	 */
	protected void setup(ProcessorContext context) {
	}
}