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
 */
package com.slytechs.jnet.jnetruntime.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.slytechs.jnet.jnetruntime.pipeline.ProcessorFactory.GroupFactory;
import com.slytechs.jnet.jnetruntime.pipeline.ProcessorFactory.PipelineFactory;

/**
 * The Class NetPipeline.
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 * @param <T_IN>  the generic type
 * @param <T_OUT> the generic type
 */
public class Pipeline<T_IN, T_OUT>
		extends Processor<Pipeline<T_IN, T_OUT>, T_IN, T_OUT>
		implements AutoCloseable, Iterable<Processor<?, ?, ?>> {

	/** The pipeline list. */
	private final List<Pipeline<?, ?>> pipelineList = new ArrayList<>();

	/** The groups. */
	private final List<ProcessorGroup<?, ?, ?>> groupList = new ArrayList<>();

	/** The all processors. */
	private final List<Processor<?, ?, ?>> processorList = new ArrayList<>();

	/** The closed. */
	private boolean closed;

	/** The context. */
	private final ProcessorContext context;

	/** The highest priority. */
	private int highestPriority = 0;

	/**
	 * Instantiates a new net pipeline.
	 *
	 * @param priority   the priority
	 * @param inputType  the input type
	 * @param outputType the output type
	 */
	protected Pipeline(int priority, DataType inputType, DataType outputType) {
		super(priority, inputType, outputType);
		this.context = new ProcessorContext();
	}

	/**
	 * Builds the.
	 */
	private void build() {
		checkNotBuiltStatus();

		Collections.sort(groupList);

		groupList.forEach(this::setupGroup);
		groupList.forEach(this::linkGroup);

		closed = true;
	}

	/**
	 * Check not built.
	 */
	private void checkNotBuiltStatus() {
		if (closed)
			throw new IllegalStateException("pipeline already built");
	}

	/**
	 * Close.
	 *
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() {
		build();
	}

	/**
	 * Context.
	 *
	 * @return the net processor context
	 */
	public final ProcessorContext context() {
		return context;
	}

	/**
	 * Enable all.
	 *
	 * @param b the b
	 * @return the net pipeline
	 */
	public Pipeline<T_IN, T_OUT> enableAll(boolean b) {
		checkNotBuiltStatus();

		return enableAll(() -> b);
	}

	/**
	 * Enable all.
	 *
	 * @param b the b
	 * @return the net pipeline
	 */
	public Pipeline<T_IN, T_OUT> enableAll(BooleanSupplier b) {
		checkNotBuiltStatus();

		for (var p : this)
			p.enable(b);

		return this;
	}

	/**
	 * Install.
	 *
	 * @param <T>      the generic type
	 * @param <T1>     the generic type
	 * @param <T2>     the generic type
	 * @param priority the priority
	 * @param factory  the factory
	 * @return the t
	 */
	public <T extends Processor<T, T1, T2>, T1, T2> T install(int priority, ProcessorFactory<T, T1, T2> factory) {
		checkNotBuiltStatus();

		T processor = factory.newInstance(priority);

		return installProcessor0(priority, processor);
	}

	/**
	 * Install.
	 *
	 * @param <T>     the generic type
	 * @param <T1>    the generic type
	 * @param <T2>    the generic type
	 * @param factory the factory
	 * @return the t
	 */
	public <T extends Processor<T, T1, T2>, T1, T2> T install(ProcessorFactory<T, T1, T2> factory) {
		return install(nextPriority(), factory);
	}

	/**
	 * Install group.
	 *
	 * @param <T>     the generic type
	 * @param <T1>    the generic type
	 * @param <T2>    the generic type
	 * @param factory the factory
	 * @return the t
	 */
	protected <T extends ProcessorGroup<T, T1, T2>, T1, T2> T installGroup(GroupFactory<T, T1, T2> factory) {
		return installGroup(nextPriority(), factory);
	}

	/**
	 * Install pipeline.
	 *
	 * @param <T>      the generic type
	 * @param <T1>     the generic type
	 * @param <T2>     the generic type
	 * @param priority the priority
	 * @param factory  the factory
	 * @return the t
	 */
	protected <T extends ProcessorGroup<T, T1, T2>, T1, T2> T installGroup(int priority,
			GroupFactory<T, T1, T2> factory) {
		T newGroup = factory.newInstance(priority);

		groupList.add(newGroup);
		Collections.sort(groupList);

		return newGroup;
	}

	/**
	 * Install pipeline.
	 *
	 * @param <T>      the generic type
	 * @param <T1>     the generic type
	 * @param <T2>     the generic type
	 * @param priority the priority
	 * @param factory  the factory
	 * @return the t
	 */
	public <T extends Pipeline<T1, T2>, T1, T2> T installPipeline(int priority, PipelineFactory<T, T1, T2> factory) {
		T newPipeline = factory.newInstance(priority);

		pipelineList.add(newPipeline);
		Collections.sort(pipelineList);

		return installProcessor0(priority, newPipeline);
	}

	/**
	 * Install pipeline.
	 *
	 * @param <T>     the generic type
	 * @param <T1>    the generic type
	 * @param <T2>    the generic type
	 * @param factory the factory
	 * @return the t
	 */
	public <T extends Pipeline<T1, T2>, T1, T2> T installPipeline(PipelineFactory<T, T1, T2> factory) {
		return installPipeline(nextPriority(), factory);
	}

	/**
	 * Install processor 0.
	 *
	 * @param <T>          the generic type
	 * @param priority     the priority
	 * @param newProcessor the new processor
	 * @return the t
	 */
	private <T extends Processor<?, ?, ?>> T installProcessor0(int priority, T newProcessor) {
		processorList.add(newProcessor);

		if (highestPriority < priority)
			highestPriority = priority;

		return newProcessor;
	}

	/**
	 * Checks if is open.
	 *
	 * @return true, if is open
	 */
	public boolean isOpen() {
		return !closed;
	}

	/**
	 * Iterator.
	 *
	 * @return the iterator
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Processor<?, ?, ?>> iterator() {
		return processorList.iterator();
	}

	/**
	 * Link group.
	 *
	 * @param group the group
	 */
	private void linkGroup(ProcessorGroup<?, ?, ?> group) {

	}

	/**
	 * Next priority.
	 *
	 * @return the int
	 */
	private int nextPriority() {
		return highestPriority + 10;
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#onLink(com.slytechs.jnet.jnetruntime.pipeline.ProcessorLink)
	 */
	@Override
	public void onLink(ProcessorLink<T_IN> link) {
		// Nothing to do
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#onUnlink(com.slytechs.jnet.jnetruntime.pipeline.ProcessorLink)
	 */
	@Override
	public void onUnlink(ProcessorLink<T_IN> link) {
	}

	/**
	 * Sets the up.
	 *
	 * @param context the new up
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#setup(com.slytechs.jnet.jnetruntime.pipeline.Processor.ProcessorContext)
	 */
	@Override
	public void setup(ProcessorContext context) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/**
	 * Setup group.
	 *
	 * @param group the group
	 */
	private void setupGroup(ProcessorGroup<?, ?, ?> group) {

	}

	/**
	 * Stream.
	 *
	 * @return the stream
	 */
	public Stream<Processor<?, ?, ?>> stream() {
		Iterator<Processor<?, ?, ?>> it = iterator();

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false);
	}

	/**
	 * Stream.
	 *
	 * @param type the type
	 * @return the stream
	 */
	public Stream<Processor<?, ?, ?>> stream(DataType type) {
		return stream().filter(p -> p.inputType().equals(type));
	}

	/**
	 * Uninstall.
	 *
	 * @param processor the processor
	 * @return the net pipeline
	 */
	public Pipeline<T_IN, T_OUT> uninstall(Processor<?, ?, ?> processor) {
		processorList.remove(processor);

		return this;
	}

	/**
	 * Uninstall all.
	 *
	 * @return the net pipeline
	 */
	public Pipeline<T_IN, T_OUT> uninstallAll() {
		checkNotBuiltStatus();

		processorList.clear();

		return this;
	}
}
