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

import static com.slytechs.jnet.jnetruntime.pipeline.PipelineUtils.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Class HeadNode.
 *
 * @param <T> the generic type
 * @author Mark Bednarczyk
 */
public final class HeadNode<T>
		extends BuiltinNode<T, HeadNode<T>> {

	/** The input map. */
	private final Map<Object, AbstractInput<?, T, ?>> inputMap = new HashMap<>();

	/**
	 * Instantiates a new head node.
	 *
	 * @param parent the parent
	 * @param name   the name
	 * @param type   the type
	 */
	public HeadNode(Pipeline<T, ?> parent, String name, DataType type) {
		super(parent, Pipeline.HEAD_BUILTIN_PRIORITY, name, type, null);
	}

	/**
	 * Adds the input.
	 *
	 * @param input the input
	 * @param id    the id
	 */
	public void addInput(AbstractInput<?, T, ?> input, Object id) {
		if (inputMap.containsKey(id))
			throw new IllegalArgumentException("input [%s] with this id [%s] already exists in pipeline [%s]"
					.formatted(input.name(), id, name()));

		inputMap.put(id, input);
		setRegistration(() -> inputMap.remove(id));
		input.outputData(outputData());
	}

	/**
	 * Inputs to string.
	 *
	 * @return the string
	 */
	public String inputsToString() {
		return inputMap.values().stream()
				.sorted()
				.map(in -> (in.isEnabled() ? "%s=>IX[%s(%s):%s]" : "!%s=>IN[%s(%s):%s]")
						.formatted(in.entryPointsToString(), in.name(), ID(in.inputData()), ID(in.outputData())))
				.collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.AbstractProcessor#outputData(java.lang.Object)
	 */
	@Override
	T outputData(T out) {
		try {
			writeLock.lock();

			// Do the actual set
			T output = super.outputData(out);

			// Pass the output to all of the input nodes
			inputMap.values().stream()
					.filter(PipeComponent::isEnabled)
					.sorted()
					.forEach(in -> in.outputData(output));

			return out;

		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.AbstractProcessor#prevElement(com.slytechs.jnet.jnetruntime.pipeline.AbstractProcessor)
	 */
	@Override
	public void prevElement(AbstractProcessor<T, ?> e) {
		if (e == null)
			return;

		String n = e == null ? "" : e.name();

		throw new UnsupportedOperationException(
				"Can not prepend processor [%s] before the head node, use addInput() method to add data sources "
						.formatted(n));
	}

}