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

/**
 * Represents a link between processors in a data processing pipeline. This
 * interface provides a mechanism for dynamically reconnecting processors,
 * allowing for flexible reconfiguration of the pipeline structure at runtime.
 * 
 * <p>
 * The ProcessorLink is typically used internally by the pipeline framework to
 * manage connections between processors, enabling operations such as processor
 * bypassing, replacement, or dynamic pipeline restructuring.
 * </p>
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 * @param <T> The type of data flowing through this link
 */
public interface ProcessorLink<T> {

	/**
	 * Relinks the processor associated with this link to a new input source.
	 * 
	 * <p>
	 * This method is used to dynamically change the input of a processor,
	 * effectively rewiring the pipeline. It can be used for various purposes such
	 * as:
	 * </p>
	 * <ul>
	 * <li>Bypassing a processor by linking its predecessor directly to its
	 * successor</li>
	 * <li>Inserting a new processor into the pipeline</li>
	 * <li>Changing the data flow path in response to runtime conditions</li>
	 * </ul>
	 *
	 * @param newInput The new input source to which the processor should be linked
	 */
	void relinkProcessor(T newInput);
}