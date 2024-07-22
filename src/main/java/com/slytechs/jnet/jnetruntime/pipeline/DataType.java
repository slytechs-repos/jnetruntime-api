/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
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
 * Defines a data type classifier for processors in a pipeline, enabling
 * grouping and prioritization. This interface provides methods for type
 * compatibility checks and support for data handling operations.
 * 
 * Implementations of this interface can be used to categorize processors and
 * ensure type safety within a processing pipeline. Each DataType represents a
 * specific kind of data that can be processed within the pipeline, allowing for
 * efficient routing and handling of different data structures.
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public interface DataType {

	/**
	 * Provides support for data handling operations specific to a particular data
	 * type. This class encapsulates the logic for creating array handlers, wrapping
	 * data, and performing compatibility checks.
	 * 
	 * The DataSupport class acts as a concrete implementation of DataType,
	 * providing additional utility methods for working with specific data types
	 * within the pipeline.
	 * 
	 * Example usage with an enum implementing DataType:
	 * 
	 * <pre>
	 * public enum PcapDataType implements DataType {
	 * 	PCAP_NATIVE(NativeCallback.class, NativeCallback::wrapUser, NativeCallback::wrapArray),
	 * 	PCAP_RAW(OfByteBuffer.class, OfByteBuffer::wrapUser, OfByteBuffer::wrapArray);
	 * 
	 * 	private final DataSupport&lt;?&gt; dataSupport;
	 * 
	 * 	&lt;T, U&gt; PcapDataType(Class&lt;T&gt; arrayFactory, BiFunction&lt;T, U, T&gt; wrapFunction, Function&lt;T[], T&gt; arrayHandler) {
	 * 		this.dataSupport = new DataSupport&lt;T&gt;(this, arrayFactory, arrayHandler, wrapFunction);
	 * 	}
	 * 
	 * 	&#64;Override
	 * 	public &lt;T&gt; DataSupport&lt;T&gt; dataSupport() {
	 * 		return (DataSupport&lt;T&gt;) dataSupport;
	 * 	}
	 * }
	 * </pre>
	 * 
	 * In this example, PcapDataType enum uses DataSupport to handle different types
	 * of PCAP data, providing type-safe operations and compatibility checks for
	 * each data type.
	 * 
	 * @param <T> The type of data this support class handles
	 */
	public final class DataSupport<T> implements DataType {
		private final Class<T> dataInterface;
		private final IntFunction<T[]> arrayFactory;
		private final Function<T[], T> arrayHandler;
		private final DataType src;
		private final BiFunction<T, ? super Object, T> wrapFunction;

		/**
		 * Constructs a new DataSupport instance.
		 * 
		 * This constructor is typically used when implementing a DataType enum, as
		 * shown in the class example.
		 *
		 * @param <U>          The type of user data that can be wrapped with the data
		 * @param src          The source DataType, typically an enum implementing
		 *                     DataType
		 * @param dataClass    The class representing the data type
		 * @param arrayHandler A function to handle arrays of the data type
		 * @param wrapFunction A function to wrap data with user objects
		 */
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
		 * {@inheritDoc}
		 */
		@Override
		public Class<?> dataInterface() {
			return dataInterface;
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public <T2> DataSupport<T2> dataSupport() {
			return (DataSupport<T2>) this;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * If the source DataType is an enum, returns its ordinal value as the priority.
		 * Otherwise, returns 0.
		 */
		@Override
		public int priority() {
			if (this.src instanceof Enum<?> e)
				return e.ordinal();

			return 0;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * For Processor objects, checks compatibility with the processor's input. For
		 * other objects, checks if they are instances of the data interface.
		 */
		@Override
		public boolean isCompatibleWith(Object sink) {
			if (sink instanceof Processor<?, ?> p)
				return dataInterface.isInstance(p.input());

			return dataInterface.isInstance(sink);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isCompatibleWithClass(Class<?> sink) {
			return dataInterface.isAssignableFrom(sink);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isCompatibleWithProcessor(Processor<?, ?> processor) {
			return dataInterface.isInstance(processor.input());
		}

		/**
		 * Creates a handler for a list of data elements.
		 * 
		 * If the list contains only one element, that element is returned directly.
		 * Otherwise, an array handler is created and applied to the list converted to
		 * an array.
		 * 
		 * In the context of the PcapDataType example, this method would be used to
		 * create handlers for lists of NativeCallback or OfByteBuffer objects,
		 * depending on the data type.
		 *
		 * @param list The list of data elements
		 * @return A handler for the list, either a single element or an array handler
		 */
		public T createListHandler(List<T> list) {
			if (list.size() == 1)
				return list.get(0);

			final T[] array = list.toArray(arrayFactory);
			return arrayHandler.apply(array);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String name() {
			return src.name();
		}

		/**
		 * Wraps the original data with a new user object.
		 * 
		 * This method allows attaching additional user-defined information to the data
		 * object. In the PcapDataType example, this could be used to attach metadata to
		 * PCAP packets.
		 *
		 * @param <U>      The type of the user object
		 * @param original The original data
		 * @param newUser  The new user object to wrap with the data
		 * @return The wrapped data
		 */
		public <U> T wrap(T original, U newUser) {
			return wrapFunction.apply(original, newUser);
		}
	}

	/**
	 * Returns the interface class representing this data type.
	 * 
	 * @return The Class object of the data interface
	 */
	default Class<?> dataInterface() {
		return dataSupport().dataInterface();
	}

	/**
	 * Returns the DataSupport object for this data type.
	 * 
	 * This method provides access to additional type-specific operations and
	 * checks.
	 *
	 * @param <T> The type of data supported
	 * @return The DataSupport object
	 */
	<T> DataSupport<T> dataSupport();

	/**
	 * Returns the priority of this data type.
	 * 
	 * Priority can be used to determine the order of processing in the pipeline.
	 *
	 * @return The priority as an integer
	 */
	default int priority() {
		return dataSupport().priority();
	}

	/**
	 * Checks if this data type is compatible with a given sink object.
	 * 
	 * @param sink The object to check compatibility with
	 * @return true if compatible, false otherwise
	 */
	default boolean isCompatibleWith(Object sink) {
		return dataSupport().isCompatibleWith(sink);
	}

	/**
	 * Checks if this data type is compatible with a given class.
	 * 
	 * @param sink The class to check compatibility with
	 * @return true if compatible, false otherwise
	 */
	default boolean isCompatibleWithClass(Class<?> sink) {
		return dataSupport().isCompatibleWithClass(sink);
	}

	/**
	 * Checks if this data type is compatible with a given processor.
	 * 
	 * @param processor The processor to check compatibility with
	 * @return true if compatible, false otherwise
	 */
	default boolean isCompatibleWithProcessor(Processor<?, ?> processor) {
		return dataSupport().isCompatibleWithProcessor(processor);
	}

	/**
	 * Returns the name of this data type.
	 * 
	 * This name can be used for identification and logging purposes.
	 *
	 * @return The name as a String
	 */
	String name();
}