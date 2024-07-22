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
 * An abstract class representing a unary processor in a data processing
 * pipeline. A unary processor has the same input and output type, allowing for
 * in-place data transformation.
 *
 * <p>
 * This class extends the {@link Processor} class and provides additional
 * functionality for managing links and enabling/disabling the processor
 * dynamically within the pipeline.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * public class PacketFilter extends UnaryProcessor&lt;Packet&gt; {
 * 	public PacketFilter(int priority) {
 * 		super(priority, DataType.PACKET);
 * 	}
 *
 * 	// Implement processing logic
 * }
 * </pre>
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 *
 * @param <T> The type of data this processor handles (both input and output)
 */
public abstract class UnaryProcessor<T> extends Processor<T, T> {

	/** List of processor links connected to this unary processor. */
	private final List<ProcessorLink<T>> links = new ArrayList<>();

	/**
	 * Constructs a new UnaryProcessor with the specified priority and data type.
	 *
	 * @param priority The priority of this processor in the pipeline
	 * @param type     The data type this processor handles
	 */
	public UnaryProcessor(int priority, DataType type) {
		super(priority, type, type);
	}

	/**
	 * Handles the linking of this processor to another component in the pipeline.
	 * Adds the link to the list of connected links.
	 *
	 * @param link The ProcessorLink being established
	 */
	@Override
	public void onLink(ProcessorLink<T> link) {
		links.add(link);
	}

	/**
	 * Handles the unlinking of this processor from another component in the
	 * pipeline. Removes the link from the list of connected links.
	 *
	 * @param link The ProcessorLink being removed
	 */
	@Override
	public void onUnlink(ProcessorLink<T> link) {
		links.remove(link);
	}

	/**
	 * Enables or disables this unary processor. When disabled, the processor is
	 * bypassed in the pipeline, effectively linking its input directly to its
	 * output for all connected links.
	 *
	 * <p>
	 * This method provides an optimization for unary processors, allowing them to
	 * be dynamically enabled or disabled without reconstructing the pipeline.
	 * </p>
	 *
	 * @param b true to enable the processor, false to disable it
	 * @return This UnaryProcessor instance for method chaining
	 */
	@Override
	public UnaryProcessor<T> enable(boolean b) {
		if ((isEnabled() && b) || (!isEnabled() && !b))
			return this; // Nothing to do

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

		return this;
	}

	/**
	 * Switches the input of a linked processor to this processor's output. Used
	 * when disabling this processor to bypass it in the pipeline.
	 *
	 * @param link The ProcessorLink to switch
	 */
	private void switchToInactiveInput(ProcessorLink<T> link) {
		link.relinkProcessor(super.output());
	}

	/**
	 * Switches the input of a linked processor back to this processor's input. Used
	 * when re-enabling this processor to include it in the pipeline.
	 *
	 * @param link The ProcessorLink to switch
	 */
	private void switchToActiveInput(ProcessorLink<T> link) {
		link.relinkProcessor(super.input());
	}
}