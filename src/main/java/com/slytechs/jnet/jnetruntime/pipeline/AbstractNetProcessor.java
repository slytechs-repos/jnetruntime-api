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

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public abstract class AbstractNetProcessor<T extends NetProcessor<T>> implements NetProcessor<T> {

	private final int priority;
	private final NetProcessorType type;
	
	private String name;
	private BooleanSupplier enabled = () -> true;
	private final NetProcessorGroup group;

	AbstractNetProcessor(int priority, NetProcessorType classifier) {
		this.group = null;
		this.priority = priority;
		this.type = classifier;
		this.name = getClass().getSimpleName();
	}

	protected AbstractNetProcessor(NetProcessorGroup group, int priority, NetProcessorType type) {
		this.group = Objects.requireNonNull(group, "group").resolveByType(type);
		this.priority = priority;
		this.type = type;
		this.name = getClass().getSimpleName();
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#name()
	 */
	@Override
	public String name() {
		return name;
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#name(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T name(String newName) {
		this.name = newName;

		return (T) this;
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#priority()
	 */
	@Override
	public int priority() {
		return priority;
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#type()
	 */
	@Override
	public GroupType type() {
		return type;
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return enabled.getAsBoolean();
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#enable(java.util.function.BooleanSupplier)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T enable(BooleanSupplier b) {
		this.enabled = Objects.requireNonNull(b, "enabled");

		return (T) this;
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#enable(boolean)
	 */
	@Override
	public T enable(boolean b) {
		return enable(() -> b);
	}

	/**
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#apply()
	 */
	@Override
	public NetPipeline apply() {
		return pipeline();
	}
	
	public NetPipeline pipeline() {
		return group.pipeline();
	}

	@Override
	public NetProcessorGroup group() {
		return group;
	}

}
