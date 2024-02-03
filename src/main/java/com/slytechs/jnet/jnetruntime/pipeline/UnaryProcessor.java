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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 *
 */
public abstract class UnaryProcessor<T_US extends UnaryProcessor<T_US, T>, T> extends Processor<T_US, T, T> {

	private final List<ProcessorLink<T>> links = new ArrayList<>();

	/**
	 * @param priority
	 * @param type
	 */
	public UnaryProcessor(int priority, DataType type) {
		super(priority, type, type);
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#onLink(com.slytechs.jnet.jnetruntime.pipeline.ProcessorLink)
	 */
	@Override
	public void onLink(ProcessorLink<T> link) {
		links.add(link);
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#onUnlink(com.slytechs.jnet.jnetruntime.pipeline.ProcessorLink)
	 */
	@Override
	public void onUnlink(ProcessorLink<T> link) {
		links.remove(link);
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.Processor#enable(boolean)
	 */
	@Override
	public T_US enable(boolean b) {
		if ((isEnabled() && b) || (!isEnabled() && !b))
			return us(); // Nothing to do

		super.enable(b);

		/*
		 * In unary processors IN and OUT are of the same type, we can re-map this
		 * processor's output as input to the linked source processor, in effect
		 * bypassing this processor when disabled:
		 * 
		 * Go from [in1|out1]--->[in2|out2]--->[in3|out3]
		 * 
		 * to this [in1|out1]-------->out2]--->[in3|out3]
		 * 
		 * to then [in1|out1]--->[in2|out2]--->[in3|out3]
		 * 
		 * back to original state when re-enabled.
		 */
		if (b)
			links.forEach(this::switchToActiveInput);
		else
			links.forEach(this::switchToInactiveInput);

		return us();
	}

	private void switchToInactiveInput(ProcessorLink<T> link) {
		link.relinkProcessor(super.output());
	}

	private void switchToActiveInput(ProcessorLink<T> link) {
		link.relinkProcessor(super.input());
	}

}
