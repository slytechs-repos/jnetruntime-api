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
import java.util.List;

import com.slytechs.jnet.jnetruntime.util.Registration;

/**
 * Represents a group of processors in a data processing pipeline. This class
 * manages a collection of UnaryProcessors that operate on the same data type,
 * allowing for sequential processing within the group.
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 * @param <T_IN>  The input type for the processor group
 * @param <T_OUT> The output type for the processor group
 */
public class ProcessorGroup<T_IN, T_OUT> extends Processor<T_IN, T_OUT> {

	/** The list of UnaryProcessors in this group. */
	private final List<UnaryProcessor<T_OUT>> processors = new ArrayList<>();

	/** The main processor for this group. */
	private final Processor<T_IN, T_OUT> mainProcessor;

	/**
	 * Constructs a new ProcessorGroup with a specified group processor.
	 *
	 * @param <T>            The type of the group processor
	 * @param priority       The priority of this processor group
	 * @param groupProcessor The main processor for this group
	 * @param inputType      The input data type for this group
	 * @param outputType     The output data type for this group
	 */
	protected <T extends Processor<T_IN, T_OUT>> ProcessorGroup(int priority, T groupProcessor, DataType inputType,
			DataType outputType) {
		super(priority, groupProcessor.input(), inputType, outputType);
		this.mainProcessor = groupProcessor;
	}

	/**
	 * Constructs a new ProcessorGroup without a specific group processor.
	 *
	 * @param priority   The priority of this processor group
	 * @param inputType  The input data type for this group
	 * @param outputType The output data type for this group
	 */
	protected ProcessorGroup(int priority, DataType inputType, DataType outputType) {
		super(priority, inputType, outputType);
		this.mainProcessor = this;
	}

	/**
	 * Returns the main group processor.
	 *
	 * @param <T> The type of the group processor
	 * @return The main group processor
	 */
	@SuppressWarnings("unchecked")
	protected final <T extends Processor<T_IN, T_OUT>> T groupProcessor() {
		return (T) this.mainProcessor;
	}

	/**
	 * Disposes of this processor group and all its members.
	 */
	@Override
	protected void destroy() {
		super.destroy();
		processors.forEach(Processor::destroy);
		Collections.fill(processors, null); // Destroy
		processors.clear();
	}

	/**
	 * Checks if this processor group is empty.
	 *
	 * @return true if the group contains no processors, false otherwise
	 */
	public boolean isEmpty() {
		return processors.isEmpty();
	}

	/**
	 * Returns an array of all processors in this group.
	 *
	 * @return An array of Processor objects
	 */
	public Processor<?, ?>[] members() {
		return this.processors.toArray(Processor[]::new);
	}

	/**
	 * Links all processors in this group sequentially. This method connects the
	 * output of each processor to the input of the next processor in the group.
	 *
	 * @return A Registration object that can be used to unregister all the links
	 */
	Registration linkGroupProcessors() {
		if (isEmpty())
			return Registration.empty();

		final List<Registration> list = new ArrayList<>(processors.size());

		/* Add to this processors output */
		Registration reg = super.addOutput(processors.getFirst().input());
		list.add(reg);

		for (int i = 0; i < processors.size() - 1; i++) {
			Processor<T_OUT, T_OUT> prev = processors.get(i + 0);
			Processor<T_OUT, T_OUT> next = processors.get(i + 1);
			reg = prev.addOutput(next.input(), next);
			list.add(reg);
		}

		return () -> list.forEach(Registration::unregister);
	}
}