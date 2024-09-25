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

/**
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 *
 */
public class AbstractTransformer<T_IN, T_OUT, T_BASE extends DataTransformer<T_IN, T_OUT, T_BASE> & PipeComponent<T_BASE>>
		extends AbstractComponent<T_BASE>
		implements DataTransformer<T_IN, T_OUT, T_BASE> {

	private T_OUT outputData;
	private T_IN inputData;
	private final DataType inputType;
	private final DataType outputType;

	public AbstractTransformer(String name, T_IN input, DataType inputType, DataType outputType) {
		super(name);

		this.inputData = input;
		this.inputType = inputType;
		this.outputType = outputType;
	}

	@SuppressWarnings("unchecked")
	public AbstractTransformer(String name, DataType inputType, DataType outputType) {
		super(name);

		this.inputType = inputType;
		this.outputType = outputType;
		this.inputData = (T_IN) this;
	}

	public T_OUT outputData() {
		return this.outputData;
	}

	T_OUT outputData(T_OUT output) {
		this.outputData = output;

		return output;
	}

	T_IN inputData(T_IN input) {
		this.inputData = input;

		return input;
	}

	public T_IN inputData() {
		return this.inputData;
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.DataTransformer#inputType()
	 */
	@Override
	public DataType inputType() {
		return this.inputType;
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.DataTransformer#outputType()
	 */
	@Override
	public DataType outputType() {
		return this.outputType;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

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
	}

}