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

import com.slytechs.jnet.jnetruntime.pipeline.DataTransformer.OutputTransformer;
import com.slytechs.jnet.jnetruntime.pipeline.DataTransformer.OutputTransformer.EndPoint;
import com.slytechs.jnet.jnetruntime.util.HasPriority;
import com.slytechs.jnet.jnetruntime.util.Registration;

/**
 * Implementation of the EndPoint interface for managing output endpoints.
 *
 * @param <T> the generic type
 * @author Mark Bednarczyk
 */
public class MultiEndPoint<T>
		extends AbstractComponent<MultiEndPoint<T>>
		implements EndPoint<T> {

	/** The id. */
	private final String id;

	/** The data list registration. */
	private Registration dataListRegistration;

	private final AbstractOutput<?, T, ?> output;

	/**
	 * Constructs a new EndPointImpl with the given ID.
	 *
	 * @param id The identifier for this endpoint
	 */
	public MultiEndPoint(OutputTransformer<T> output, String id) {
		super((PipeComponent<?>) output, id, HasPriority.DEFAULT_PRIORITY_VALUE);
		this.id = id;
		this.output = (AbstractOutput<?, T, ?>) output;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endPointData(T data) {
		if (this.dataListRegistration != null)
			throw new IllegalStateException("output's [%s] endpoint [%s] is already set"
					.formatted(name(), id()));

		output.outputList.add(data);
		this.dataListRegistration = () -> output.outputList.remove(data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataType endPointType() {
		return endPointType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String id() {
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unregister() {
		if (dataListRegistration != null)
			dataListRegistration.unregister();

		output.endPointMap.remove(id);
	}
}