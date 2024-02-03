/*
 * Sly Technologies Free License
 * 
 * Copyright 2023 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.jnet.jnetruntime.pipeline;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Processor group classifier, so that processors can be grouped by their class
 * type and arranged in priority order within.
 * 
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public interface DataType {

	public final class DataSupport<T> implements DataType {
		private final Class<T> dataInterface;
		private final IntFunction<T[]> arrayFactory;
		private final Function<T[], T> arrayHandler;
		private final DataType src;
		private final BiFunction<T, ? super Object, T> wrapFunction;

		@SuppressWarnings("unchecked")
		public <U> DataSupport(DataType src,
				Class<T> dataClass,
				Function<T[], T> arrayHandler,
				BiFunction<T, U, T> wrapFunction) {

			this.wrapFunction = (BiFunction<T, ? super Object, T>) wrapFunction;
			this.src = Objects.requireNonNull(src, "src");
			this.dataInterface = Objects.requireNonNull(dataClass, "dataClass");
			this.arrayFactory = size -> (T[]) Array.newInstance(dataClass, size);
			this.arrayHandler = Objects.requireNonNull(arrayHandler, "arrayHandler");
		}

		/**
		 * @see com.slytechs.jnet.jnetruntime.pipeline.DataType#dataInterface()
		 */
		@Override
		public Class<?> dataInterface() {
			return dataInterface;
		}

		/**
		 * @see com.slytechs.jnet.jnetruntime.pipeline.DataType#dataSupport()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public <T2> DataSupport<T2> dataSupport() {
			return (DataSupport<T2>) this;
		}

		/**
		 * @see com.slytechs.jnet.jnetruntime.pipeline.DataType#id()
		 */
		@Override
		public int id() {
			/*
			 * This is mostly implemented by enums, and if we are one, we default to ordinal
			 * index
			 */
			if (this.src instanceof Enum<?> e)
				return e.ordinal();

			return 0;
		}

		/**
		 * @see com.slytechs.jnet.jnetruntime.pipeline.DataType#isCompatibleWith(java.lang.Object)
		 */
		@Override
		public boolean isCompatibleWith(Object sink) {
			if (sink instanceof Processor<?, ?, ?> p)
				return dataInterface.isInstance(p.input());

			return dataInterface.isInstance(sink);
		}

		/**
		 * @see com.slytechs.jnet.jnetruntime.pipeline.DataType#isCompatibleWithClass(java.lang.Class)
		 */
		@Override
		public boolean isCompatibleWithClass(Class<?> sink) {
			return dataInterface.isAssignableFrom(sink);
		}

		/**
		 * @see com.slytechs.jnet.jnetruntime.pipeline.DataType#isCompatibleWithProcessor(com.slytechs.jnet.jnetruntime.pipeline.Processor)
		 */
		@Override
		public boolean isCompatibleWithProcessor(Processor<?, ?, ?> processor) {
			return dataInterface.isInstance(processor.input());
		}

		public T createListHandler(List<T> list) {
			if (list.size() == 1)
				return list.get(0);

			final T[] array = list.toArray(arrayFactory);
			return arrayHandler.apply(array);
		}

		/**
		 * @see com.slytechs.jnet.jnetruntime.pipeline.DataType#name()
		 */
		@Override
		public String name() {
			return src.name();
		}
		
		public <U> T wrap(T original, U newUser) {
			return wrapFunction.apply(original, newUser);
		}
	}

	default Class<?> dataInterface() {
		return dataSupport().dataInterface();
	}

	<T> DataSupport<T> dataSupport();

	default int id() {
		return dataSupport().id();
	}

	default boolean isCompatibleWith(Object sink) {
		return dataSupport().isCompatibleWith(sink);
	}

	default boolean isCompatibleWithClass(Class<?> sink) {
		return dataSupport().isCompatibleWithClass(sink);
	}

	default boolean isCompatibleWithProcessor(Processor<?, ?, ?> processor) {
		return dataSupport().isCompatibleWithProcessor(processor);
	}

	String name();
}