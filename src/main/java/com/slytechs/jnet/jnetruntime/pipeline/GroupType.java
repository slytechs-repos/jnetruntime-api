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

/**
 * Processor group classifier, so that processors can be grouped by their class
 * type and arranged in priority order within.
 * 
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public interface GroupType {

	String name();

	default int id() {

		/*
		 * This is mostly implemented by enums, and if we are one, we default to ordinal
		 * index
		 */
		if (this instanceof Enum<?> e)
			return e.ordinal();

		return 0;
	}
}