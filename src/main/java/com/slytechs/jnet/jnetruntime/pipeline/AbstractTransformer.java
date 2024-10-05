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

import java.util.Objects;

import com.slytechs.jnet.jnetruntime.util.HasPriority;

/**
 * Abstract base class for data transformers in a pipeline.
 * 
 * <p>
 * This class implements the core functionality of a data transformer, including
 * input and output data management and type handling. It serves as a foundation
 * for creating specific data transformer implementations.
 * </p>
 *
 * @param <T_IN>   The input data type
 * @param <T_OUT>  The output data type
 * @param <T_BASE> The specific type of the transformer implementation
 * @author Mark Bednarczyk
 */
public class AbstractTransformer<T_IN, T_OUT, T_BASE extends DataTransformer<T_IN, T_OUT, T_BASE> & PipeComponent<T_BASE>>
		extends AbstractComponent<T_BASE>
		implements DataTransformer<T_IN, T_OUT, T_BASE> {

	/** The output data. */
	private T_OUT outputData;

	/** The input data. */
	private T_IN inputData;

	private T_IN inputDataSave;

	/** The input type. */
	private final DataType inputType;

	/** The output type. */
	private final DataType outputType;

	/**
	 * Constructs a new AbstractTransformer with specified name, input data, input
	 * type, and output type.
	 *
	 * @param name       The name of the transformer
	 * @param input      The initial input data
	 * @param inputType  The type of the input data
	 * @param outputType The type of the output data
	 */
	public AbstractTransformer(PipeComponent<?> component, String name, T_IN input, DataType inputType,
			DataType outputType) {
		super(component, name, HasPriority.DEFAULT_PRIORITY_VALUE);
		assert inputType.isCompatibleWith(input.getClass());
		this.inputData = input;
		this.inputDataSave = input;
		this.inputType = inputType;
		this.outputType = outputType;
	}

	/**
	 * Constructs a new AbstractTransformer with specified name, input type, and
	 * output type. The transformer itself is set as the initial input data.
	 *
	 * @param name       The name of the transformer
	 * @param inputType  The type of the input data
	 * @param outputType The type of the output data
	 */
	@SuppressWarnings("unchecked")
	public AbstractTransformer(PipeComponent<?> component, String name, DataType inputType, DataType outputType) {
		super(component, name, HasPriority.DEFAULT_PRIORITY_VALUE);
		assert inputType.isCompatibleWith(this.getClass()) : ""
				+ "incompatible input type [%s] with subclass [%s]"
						.formatted(inputType.name(), this.getClass().getSimpleName());
		this.inputType = inputType;
		this.outputType = outputType;
		this.inputData = (T_IN) this;
		this.inputDataSave = (T_IN) this;
	}

	/**
	 * {@inheritDoc}
	 */
	public T_OUT outputData() {
		try {
			readLock.lock();

			return this.outputData;

		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Sets the output data for this transformer.
	 *
	 * @param output The new output data
	 * @return The set output data
	 */
	T_OUT outputData(T_OUT output) {
		try {
			writeLock.lock();
			this.outputData = output;

			return output;

		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Sets the input data for this transformer.
	 *
	 * @param input The new input data
	 * @return The set input data
	 */
	T_IN inputData(T_IN input) {
		try {
			writeLock.lock();
			this.inputData = input;

			return input;

		} finally {
			writeLock.unlock();
		}
	}

	protected final void restoreInputData() {
		try {
			writeLock.lock();

			this.inputData = this.inputDataSave;

		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public T_IN inputData() {
		try {
			readLock.lock();

			return this.inputData;

		} finally {
			readLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataType inputType() {
		return this.inputType;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataType outputType() {
		return this.outputType;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		try {
			readLock.lock();
			var in = inputData == null ? "" : Objects.toIdentityString(inputData);
			var out = outputData == null ? "" : Objects.toIdentityString(outputData);

			return ""
					+ getClass().getSimpleName()
					+ " [name=" + name()
					+ ", inputType=" + inputType
					+ ", outputType=" + outputType
					+ ", output=" + out
					+ ", input=" + in
					+ "]";

		} finally {
			readLock.unlock();
		}
	}

}