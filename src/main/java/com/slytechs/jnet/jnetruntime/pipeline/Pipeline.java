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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.slytechs.jnet.jnetruntime.NotFound;
import com.slytechs.jnet.jnetruntime.util.Reconfigurable;
import com.slytechs.jnet.jnetruntime.util.Registration;

/**
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public class Pipeline<T_BASE extends Pipeline<T_BASE>> extends AbstractElement implements Reconfigurable {

	private final String name;

	private final List<DataChannel<?, ?>> channelList = new ArrayList<>();

	public Pipeline(String name) {
		this.name = name;
	}

	public <T extends DataProcessor<T, C>, C> T addProcessor(int priority, T processor) throws NotFound {
		DataType type = processor.dataType();

		DataChannel<T, C> channel = getChannel(type);
		Registration registration = channel.addProcessor(priority, processor);

		return processor;
	}

	@SuppressWarnings("unchecked")
	public <T_CHANNEL extends DataChannel<T, C>, T extends DataProcessor<T, C>, C> Optional<T_CHANNEL> findChannel(
			DataType type) {

		Optional<DataChannel<?, ?>> channel = channelList.stream()
				.filter(c -> c.channelType().equals(type))
				.findFirst();

		if (channel.isEmpty())
			return Optional.empty();

		return (Optional<T_CHANNEL>) channel;
	}

	@SuppressWarnings("unchecked")
	public <T_CHANNEL extends DataChannel<T, C>, T extends DataProcessor<T, C>, C> Optional<T_CHANNEL> findChannel(
			String channelName)
			throws NotFound {

		Optional<DataChannel<?, ?>> channel = channelList.stream()
				.filter(c -> c.name().equals(channelName))
				.findFirst();

		if (channel.isEmpty())
			return Optional.empty();

		return (Optional<T_CHANNEL>) channel;
	}

	public <T_CHANNEL extends DataChannel<T, C>, T extends DataProcessor<T, C>, C> T_CHANNEL getChannel(DataType type)
			throws NotFound {

		Optional<T_CHANNEL> channel = findChannel(type);

		if (channel.isEmpty())
			throw new NotFound("Channel of type %s not found".formatted(type.name()));

		return channel.get();
	}

	public <T extends DataProcessor<T, C>, C> Registration registerProcessor(int priority, T processor)
			throws NotFound {
		DataType type = processor.dataType();

		DataChannel<T, C> channel = getChannel(type);
		Registration registration = channel.addProcessor(priority, processor);

		return registration;
	}

	public Registration registerChannel(DataChannel<?, ?> newChannel) {
		channelList.add(newChannel);

		return () -> channelList.remove(newChannel);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Pipeline [name=" + name + "]";
	}
}
