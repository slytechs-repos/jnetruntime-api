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
 * The Class Group.
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public class ProcessorGroup<T_US extends Processor<T_US, T_IN, T_OUT>, T_IN, T_OUT> extends
		Processor<T_US, T_IN, T_OUT> {

	/** The processors. */
	private final List<UnaryProcessor<?, T_OUT>> processors = new ArrayList<>();

	/**
	 * New root group.
	 *
	 * @param pipeline   the pipeline
	 * @param outputType the type
	 */
	public ProcessorGroup(int priority, DataType outputType, DataType inputType) {
		super(priority, inputType, outputType);
	}

	/**
	 * Dispose.
	 *
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#destroy()
	 */
	@Override
	public void destroy() {
		super.destroy();

		processors.forEach(Processor::destroy);
		Collections.fill(processors, null); // Destroy
		processors.clear();
	}

	/**
	 * Checks if is empty.
	 *
	 * @return true, if is empty
	 */
	public boolean isEmpty() {
		return processors.isEmpty();
	}

	/**
	 * Processors.
	 *
	 * @return the net processor[]
	 */
	public Processor<?, ?, ?>[] processors() {
		return this.processors.toArray(Processor[]::new);
	}

	/**
	 * Add to the last processor in this group, output
	 * 
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#addOutput(java.lang.Object)
	 */
	@Override
	public Registration addOutput(T_OUT out) {
		if (processors.isEmpty())
			return super.addOutput(out);

		return processors.getLast().addOutput(out);
	}

	Registration linkGroup() {
		if (isEmpty())
			return Registration.empty();

		final List<Registration> list = new ArrayList<>(processors.size());

		/* Add to this processors output */
		Registration reg = super.addOutput(processors.getFirst().input());
		list.add(reg);

		for (int i = 0; i < processors.size() - 1; i++) {
			Processor<?, T_OUT, T_OUT> prev = processors.get(i + 0);
			Processor<?, T_OUT, T_OUT> next = processors.get(i + 1);

			reg = prev.addOutput(next.input(), next);
			list.add(reg);
		}

		return () -> list.forEach(Registration::unregister);
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#setup(com.slytechs.jnet.jnetruntime.pipeline.Processor.ProcessorContext)
	 */
	@Override
	public void setup(ProcessorContext context) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#onLink(com.slytechs.jnet.jnetruntime.pipeline.ProcessorLink)
	 */
	@Override
	public void onLink(ProcessorLink<T_IN> link) {
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#onUnlink(com.slytechs.jnet.jnetruntime.pipeline.ProcessorLink)
	 */
	@Override
	public void onUnlink(ProcessorLink<T_IN> link) {
	}
}