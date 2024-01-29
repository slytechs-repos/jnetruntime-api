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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The Class NetPipeline.
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public class NetPipeline extends NetProcessorGroup implements AutoCloseable, Iterable<NetProcessor<?>> {

	public NetPipeline(NetProcessorType type) {
		super(type);
	}

	NetPipeline(NetProcessorGroup parent, NetProcessorType type) {
		super(parent, type);
	}

	/** The groups. */
	private List<NetProcessorGroup> groupList = new ArrayList<>();

	/** The all processors. */
	private final List<NetProcessor<?>> allProcessors = new ArrayList<>();

	/**
	 * Builds the.
	 *
	 * @throws Exception the exception
	 */
	private void build() {
		checkNotBuiltStatus();
		
		Collections.sort(groupList);
		
		groupList.forEach(NetProcessorGroup::linkGroup);
		groupList.forEach(NetProcessorGroup::setup);

	}

	/**
	 * Check not built.
	 */
	private void checkNotBuiltStatus() {
		if (!groupList.isEmpty())
			throw new IllegalStateException("pipeline already built");
	}

	/**
	 * Close.
	 *
	 * @throws Exception the exception
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() {
		build();
	}

	/**
	 * Enable all.
	 *
	 * @param b the b
	 * @return the net pipeline
	 */
	public NetPipeline enableAll(boolean b) {
		checkNotBuiltStatus();

		return enableAll(() -> b);
	}

	/**
	 * Enable all.
	 *
	 * @param b the b
	 * @return the net pipeline
	 */
	public NetPipeline enableAll(BooleanSupplier b) {
		checkNotBuiltStatus();

		for (var p : this)
			p.enable(b);

		return this;
	}

	/**
	 * Install.
	 *
	 * @param <T>      the generic type
	 * @param priority the priority
	 * @param factory  the factory
	 * @return the t
	 */
	public <T extends NetProcessor<T>> T install(int priority, NetProcessorFactory<T> factory) {
		checkNotBuiltStatus();

		T processor = factory.newInstance(this, priority);

		allProcessors.add(processor);

		return processor;
	}

	/**
	 * Install.
	 *
	 * @param <T>     the generic type
	 * @param factory the factory
	 * @return the t
	 */
	public <T extends NetProcessor<T>> T install(NetProcessorFactory<T> factory) {
		return install(nextPriority(), factory);
	}

	/**
	 * Iterator.
	 *
	 * @return the iterator
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<NetProcessor<?>> iterator() {
		return allProcessors.iterator();
	}

	/**
	 * Next priority.
	 *
	 * @return the int
	 */
	private int nextPriority() {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/**
	 * Stream.
	 *
	 * @return the stream
	 */
	public Stream<NetProcessor<?>> stream() {
		Iterator<NetProcessor<?>> it = iterator();

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false);
	}

	/**
	 * Stream.
	 *
	 * @param type the type
	 * @return the stream
	 */
	public Stream<NetProcessor<?>> stream(GroupType type) {
		return stream().filter(p -> p.type().equals(type));
	}

	/**
	 * Uninstall.
	 *
	 * @param processor the processor
	 * @return the net pipeline
	 */
	public NetPipeline uninstall(NetProcessor<?> processor) {
		allProcessors.remove(processor);

		return this;
	}

	/**
	 * Uninstall all.
	 *
	 * @return the net pipeline
	 */
	public NetPipeline uninstallAll() {
		checkNotBuiltStatus();

		allProcessors.clear();

		return this;
	}

	@Override
	public NetPipeline pipeline() {
		return this;
	}

	@Override
	NetProcessorGroup resolveByType(NetProcessorType type) {
		return groupList.stream()
				.filter(g -> g.type() == type)
				.findAny()
				.orElseGet(() -> {
					var g = new NetProcessorGroup(this, type);

					groupList.add(g);
					return g;
				});
	}

}
