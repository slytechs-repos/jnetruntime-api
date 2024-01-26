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
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The Class NetPipeline.
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public class NetPipeline implements AutoCloseable, Iterable<NetProcessor<?>> {

	/**
	 * The Class Group.
	 *
	 * @author Sly Technologies Inc
	 * @author repos@slytechs.com
	 */
	static class Group extends AbstractNetProcessor<Group> {

		/**
		 * The Class Empty.
		 */
		private static class Empty extends Group {

			/** The Constant EMPTY. */
			private final static NetProcessor<?>[] EMPTY = new NetProcessor[0];

			/**
			 * Instantiates a new empty.
			 *
			 * @param pipeline the pipeline
			 * @param type     the type
			 */
			protected Empty(NetPipeline pipeline, NetProcessorType type) {
				super(pipeline, type);
			}

			/**
			 * Checks if is empty.
			 *
			 * @return true, if is empty
			 * @see com.slytechs.jnet.protocol.NetProcessorGroup#isEmpty()
			 */
			@Override
			public boolean isEmpty() {
				return true;
			}

			/**
			 * Processors.
			 *
			 * @return the net processor[]
			 * @see com.slytechs.jnet.protocol.NetProcessorGroup#processors()
			 */
			@Override
			public NetProcessor<?>[] processors() {
				return EMPTY;
			}

			/**
			 * Source.
			 *
			 * @return the object
			 * @see com.slytechs.jnet.protocol.NetProcessorGroup#source()
			 */
			@Override
			public Object source() {
				throw new IllegalStateException("no processors in group");
			}

			/**
			 * To array.
			 *
			 * @return the net processor[]
			 * @see com.slytechs.jnet.protocol.NetProcessorGroup#toArray()
			 */
			@Override
			public NetProcessor<?>[] toArray() {
				return EMPTY;
			}

		}

		/**
		 * Empty.
		 *
		 * @param pipeline the pipeline
		 * @param type     the type
		 * @return the group
		 */
		public static final Group empty(NetPipeline pipeline, NetProcessorType type) {
			return new Empty(pipeline, type);
		}

		/**
		 * Of type.
		 *
		 * @param pipeline the pipeline
		 * @param type     the type
		 * @return the group
		 */
		public static final Group ofType(NetPipeline pipeline, NetProcessorType type) {
			boolean empty = pipeline.stream(type)
					.anyMatch(NetProcessor::isEnabled);

			if (empty)
				return empty(pipeline, type);
			else
				return new Group(pipeline, type);
		}

		/** The processors. */
		private final NetProcessor<?>[] processors;

		/**
		 * Instantiates a new group.
		 *
		 * @param pipeline the pipeline
		 * @param type     the type
		 */
		private Group(NetPipeline pipeline, NetProcessorType type) {
			super(pipeline, type.id(), type);

			this.processors = toArray();
		}

		/**
		 * Dispose.
		 *
		 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#dispose()
		 */
		@Override
		public void dispose() {
			for (var p : processors)
				p.dispose();
		}

		/**
		 * Checks if is empty.
		 *
		 * @return true, if is empty
		 */
		public boolean isEmpty() {
			return processors.length == 0;
		}

		/**
		 * Processors.
		 *
		 * @return the net processor[]
		 */
		public NetProcessor<?>[] processors() {
			return this.processors;
		}

		/**
		 * Setup.
		 *
		 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#setup()
		 */
		@Override
		public void setup() {
			for (var p : processors)
				p.setup();
		}

		/**
		 * Source.
		 *
		 * @return the object
		 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#source()
		 */
		@Override
		public Object source() {
			return processors[0].source();
		}

		/**
		 * To array.
		 *
		 * @return the net processor[]
		 */
		public NetProcessor<?>[] toArray() {
			NetProcessor<?>[] array = pipeline().stream(type())
					.filter(NetProcessor::isEnabled)
					.sorted()
					.toArray(NetProcessor[]::new);

			return array;
		}

	}

	/** The groups. */
	private Group[] groups;

	/** The all processors. */
	private final List<NetProcessor<?>> allProcessors = new ArrayList<>();

	/**
	 * Builds the.
	 *
	 * @throws Exception the exception
	 */
	private void build() {
		checkNotBuiltStatus();

		var list = new ArrayList<Group>();

		for (var t : NetProcessorType.values()) {
			Group g = Group.ofType(this, t);

			if (!g.isEmpty())
				list.add(g);
		}

		this.groups = list.toArray(Group[]::new);
		Arrays.sort(groups);
	}

	/**
	 * Check not built.
	 */
	private void checkNotBuiltStatus() {
		if (groups != null)
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

}
