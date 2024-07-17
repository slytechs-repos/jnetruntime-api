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
 * Defines a factory for creating processors in a data processing pipeline.
 * This interface allows for dynamic instantiation of processors with specified priorities.
 *
 * <p>The factory pattern used here enables flexible creation of processors, pipelines,
 * and processor groups, allowing for easy extension and customization of the pipeline architecture.</p>
 *
 * @param <T> The type of Processor to be created
 * @param <T_IN> The input type for the processor
 * @param <T_OUT> The output type for the processor
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public interface ProcessorFactory<T extends Processor<T_IN, T_OUT>, T_IN, T_OUT> {

    /**
     * Defines a factory for creating pipelines in a data processing system.
     * This interface allows for dynamic instantiation of pipelines with specified priorities.
     *
     * @param <T> The type of Pipeline to be created
     * @param <T_IN> The input type for the pipeline
     * @param <T_OUT> The output type for the pipeline
     */
    public interface PipelineFactory<T extends Pipeline<?, ?>, T_IN, T_OUT> {

        /**
         * Creates a new instance of a pipeline with the specified priority.
         *
         * @param priority The priority of the pipeline in the processing order
         * @return A new instance of the pipeline
         */
        T newInstance(int priority);
    }

    /**
     * Defines a factory for creating processor groups in a data processing system.
     * This interface allows for dynamic instantiation of processor groups with specified priorities.
     *
     * @param <T> The type of ProcessorGroup to be created
     * @param <T_IN> The input type for the processor group
     * @param <T_OUT> The output type for the processor group
     */
    public interface GroupFactory<T extends ProcessorGroup<T_IN, T_OUT>, T_IN, T_OUT> {

        /**
         * Creates a new instance of a processor group with the specified priority.
         *
         * @param priority The priority of the processor group in the processing order
         * @return A new instance of the processor group
         */
        T newInstance(int priority);
    }

    /**
     * Creates a new instance of a processor with the specified priority.
     *
     * @param priority The priority of the processor in the processing order
     * @return A new instance of the processor
     */
    T newInstance(int priority);
}