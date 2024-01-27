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

import java.util.function.BooleanSupplier;

/**
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 *
 */
public interface NetProcessor<T extends NetProcessor<T>> extends Comparable<NetProcessor<?>> {

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	default int compareTo(NetProcessor<?> o) {
		return priority() - o.priority();
	}

	String name();

	T name(String newName);

	int priority();

	boolean isEnabled();

	T enable(BooleanSupplier b);

	T enable(boolean b);
	
	NetPipeline apply();
	
	GroupType type();
	
	Object sink();
	
	void setup();
	
	void dispose();
	
	void link(NetProcessor<?> next);
	
	NetProcessorGroup group();
}
