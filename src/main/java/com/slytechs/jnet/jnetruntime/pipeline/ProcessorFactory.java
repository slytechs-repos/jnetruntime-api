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
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public interface ProcessorFactory<T extends NetProcessor<T, T_IN, T_OUT>, T_IN, T_OUT> {

	public interface PipelineFactory<T extends NetPipeline<?, ?>, T_IN, T_OUT> {
		T newInstance(int priority);
	}

	public interface GroupFactory<T extends ProcessorGroup<T, T_IN, T_OUT>, T_IN, T_OUT> {
		T newInstance(int priority);
	}

	T newInstance(int priority);

}
