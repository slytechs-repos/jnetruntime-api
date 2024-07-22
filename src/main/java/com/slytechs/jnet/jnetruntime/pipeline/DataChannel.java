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
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.slytechs.jnet.jnetruntime.pipeline.DataType.DataSupport;
import com.slytechs.jnet.jnetruntime.util.Registration;

/**
 * Represents a data channel in a processing pipeline, managing the flow of data
 * between different processors or components.
 * 
 * A DataChannel acts as a connector between data producers and consumers,
 * handling input of type T1 and output of type T2. It supports
 * enabling/disabling, connection management, and data flow control.
 * 
 * Example usage:
 * 
 * <pre>
 * DataChannel&lt;RawPacket, ParsedPacket&gt; channel = new DataChannel&lt;&gt;("Parser",
 * 		DataType.RAW,
 * 		DataType.PARSED,
 * 		new PacketParser());
 * channel.connect(nextProcessor);
 * channel.sink(packetConsumer);
 * channel.enable(true);
 * </pre>
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 * @param <T1> the generic type
 * @param <T2> the generic type
 */
public final class DataChannel<T1, T2> {

	/**
	 * Represents a connection between this channel (upstream) and a downstream
	 * channel. Manages the lifecycle of the connection and provides methods for
	 * closing and reconnecting.
	 */
	public class Connection {

		/** The node. */
		private final DataChannel<T2, ?> node;

		/** The node registration. */
		private Registration nodeRegistration;

		/**
		 * Constructs a new Connection between this channel and a downstream channel.
		 *
		 * @param nodeRegistration The registration object for this connection
		 * @param downstreamNode   The downstream channel being connected
		 */
		public Connection(Registration nodeRegistration, DataChannel<T2, ?> downstreamNode) {
			this.nodeRegistration = nodeRegistration;
			this.node = downstreamNode;
		}

		/**
		 * Closes this connection, unregistering it from the upstream channel.
		 *
		 * @throws IllegalStateException if the connection is already closed
		 */
		public void close() throws IllegalStateException {
			if (nodeRegistration == null)
				throw new IllegalStateException("connection already closed");

			nodeRegistration.unregister();
			nodeRegistration = null;
		}

		/**
		 * Reconnects this connection, triggering an update of the output.
		 */
		public void reconnect() {
			updateOutput();
		}
	}

	/** Flag indicating whether this channel is enabled. */
	private boolean isEnabled; // When bypassed, inline data handler is bypassed and input is current output

	/** List of downstream nodes connected to this channel. */
	private final List<DataChannel<T2, ?>> downstreamNodes = new ArrayList<>();

	/** List of sink objects consuming the output of this channel. */
	private final List<T2> sinkList = new ArrayList<>();

	/** The upstream node connected to this channel. */
	private DataChannel<?, T1> upstreamNode; // connected to single upstream node

	/** The output handler for this channel. */
	private T2 outputHandler; // Send data downstream

	/** The inline handler for processing input data. */
	private final T1 inlineHandler;

	/** The input type of this channel. */
	private final DataType inputType;

	/** The output type of this channel. */
	private final DataType outputType;

	/** List of listeners for output updates. */
	private final List<Consumer<T2>> updateListeners = new ArrayList<>();

	/** The connection to the upstream channel. */
	private DataChannel<?, T1>.Connection upstreamConnection;

	/** Flag indicating whether input and output types are the same. */
	private final boolean isDataSameType;

	/** The name of this channel. */
	private final String name;

	/** Factory for creating no-op handlers when keep-alive is enabled. */
	private Supplier<T2> noopHandlerFactory;

	/**
	 * Constructs a new DataChannel with the specified name, input/output types, and
	 * inline handler.
	 *
	 * @param name       The name of this channel, used for identification
	 * @param inputType  The type of input data this channel accepts
	 * @param outputType The type of output data this channel produces
	 * @param inline     The inline handler for processing input data
	 */
	public DataChannel(String name, DataType inputType, DataType outputType, T1 inline) {
		this.name = name;
		this.inputType = Objects.requireNonNull(inputType, "inputType");
		this.outputType = Objects.requireNonNull(outputType, "outputType");
		this.inlineHandler = Objects.requireNonNull(inline, "inline");
		this.isDataSameType = (inputType == outputType);
	}

	/**
	 * Returns the name of this channel.
	 *
	 * @return The name of this channel
	 */
	public String name() {
		return name;
	}

	/**
	 * Connects this channel to a downstream channel.
	 *
	 * Example:
	 * 
	 * <pre>
	 * DataChannel&lt;RawPacket, ParsedPacket&gt; channel1 = ...;
	 * DataChannel&lt;ParsedPacket, AnalyzedPacket&gt; channel2 = ...;
	 * Registration reg = channel1.connect(channel2);
	 * </pre>
	 *
	 * @param node The downstream channel to connect to
	 * @return A Registration object that can be used to unregister the connection
	 */
	public Registration connect(DataChannel<T2, ?> node) {
		this.downstreamNodes.add(node);

		updateOutput();

		Registration reg = () -> {
			downstreamNodes.remove(node);
			updateOutput();
		};

		node.onConnection(new Connection(reg, node));

		return reg;
	}

	/**
	 * Disables the keep-alive feature for this channel. When disabled, the channel
	 * may become inactive if there are no downstream connections.
	 */
	public void disableKeepAlive() {
		enableKeepAlive(null);
	}

	/**
	 * Enables or disables this channel.
	 *
	 * When disabled, the channel bypasses its inline handler and directly passes
	 * input to output.
	 *
	 * @param b true to enable the channel, false to disable it
	 */
	public void enable(boolean b) {
		if (isEnabled == b)
			return;

		isEnabled = b;

		updateOutput();
	}

	/**
	 * Enables the keep-alive feature for this channel with a specified no-op
	 * handler factory.
	 *
	 * The keep-alive feature ensures that the channel remains active even when
	 * there are no downstream connections.
	 *
	 * @param noopFactory A supplier that creates a no-op handler when needed
	 */
	public void enableKeepAlive(Supplier<T2> noopFactory) {
		this.noopHandlerFactory = noopFactory;
	}

	/**
	 * Returns the input handler for this channel.
	 *
	 * @return The input handler, or null if the channel is not active or enabled
	 */
	@SuppressWarnings("unchecked")
	public T1 input() {
		if (!isActive())
			return null;

		if (isEnabled) {
			if (isDataSameType)
				return (T1) outputHandler;
			else
				return null;
		}

		return inlineHandler;
	}

	/**
	 * Returns the input type of this channel.
	 *
	 * @return The DataType of the input this channel accepts
	 */
	public DataType inputType() {
		return inputType;
	}

	/**
	 * Checks if this channel is currently active (has an output handler).
	 *
	 * @return true if the channel is active, false otherwise
	 */
	public boolean isActive() {
		return (outputHandler != null);
	}

	/**
	 * Checks if this channel is currently enabled.
	 *
	 * @return true if the channel is enabled, false otherwise
	 */
	public boolean isEnabled() {
		return isEnabled;
	}

	/**
	 * Notifies the upstream channel of changes in this channel. This method is
	 * called internally when the output handler changes.
	 */
	private void notifyUpstream() {
		if (upstreamNode == null)
			return;

		/* Notify up the channel */
		upstreamConnection.reconnect();
	}

	/**
	 * Handles the connection from an upstream channel. This method is called
	 * internally when this channel is connected as a downstream node.
	 *
	 * @param upstreamLink The Connection object representing the upstream link
	 */
	void onConnection(DataChannel<?, T1>.Connection upstreamLink) {
		this.upstreamConnection = upstreamLink;
	}

	/**
	 * Returns the current output handler of this channel.
	 *
	 * @return The current output handler
	 */
	public T2 output() {
		return outputHandler;
	}

	/**
	 * Returns the output type of this channel.
	 *
	 * @return The DataType of the output this channel produces
	 */
	public DataType outputType() {
		return outputType;
	}

	/**
	 * Adds a sink (consumer) for the output of this channel.
	 *
	 * Example:
	 * 
	 * <pre>
	 * DataChannel&lt;RawPacket, ParsedPacket&gt; channel = ...;
	 * ParsedPacketConsumer consumer = ...;
	 * Registration reg = channel.sink(consumer);
	 * </pre>
	 *
	 * @param sink The sink to add
	 * @return A Registration object that can be used to unregister the sink
	 */
	public Registration sink(T2 sink) {
		this.sinkList.add(sink);

		updateOutput();

		return () -> {
			sinkList.remove(sink);
		};
	}

	/**
	 * Returns a string representation of this channel, including its name, type,
	 * and connections.
	 *
	 * @return A string representation of this channel
	 */
	@Override
	public String toString() {
		var list = downstreamNodes;
		var type = isDataSameType
				? inputType.toString()
				: "%s=>%s".formatted(inputType, outputType);
		var act = isActive() ? "+" : "-";

		if (list.isEmpty())
			return "[%s%s,%s]".formatted(act, name, type);

		else if (list.size() == 1)
			return "[%s%s,%s]->%s".formatted(act, name, type, list.get(0));

		return "[%s%s,%s]->%s".formatted(act, name, type, list.stream()
				.map(String::valueOf)
				.collect(Collectors.joining("\n  ", "{\n  ", "\n}")));
	}

	/**
	 * Adds a listener for updates to the output handler of this channel.
	 *
	 * Example:
	 * 
	 * <pre>
	 * DataChannel&lt;RawPacket, ParsedPacket&gt; channel = ...;
	 * Registration reg = channel.updateListener(newOutput -> {
	 *     System.out.println("Output updated: " + newOutput);
	 * });
	 * </pre>
	 *
	 * @param updateHandler A consumer that will be called when the output handler
	 *                      changes
	 * @return A Registration object that can be used to unregister the listener
	 */
	public Registration updateListener(Consumer<T2> updateHandler) {
		updateListeners.add(updateHandler);

		return () -> updateListeners.remove(updateHandler);
	}

	/**
	 * Updates the output handler of this channel. This method is called internally
	 * when connections or sinks change.
	 */
	private void updateOutput() {

		var oldHandler = this.outputHandler;

		// Extract a list of downstream data handlers
		List<T2> handlerList = new ArrayList<>(downstreamNodes.stream()
				.map(DataChannel<T2, ?>::input)
				.filter(Objects::nonNull)
				.toList());

		handlerList.addAll(sinkList);

		/*
		 * If no handlers, check if auto-disable is active (noopFactory as flag is
		 * null), otherwise we install a NO-OP handler which force keeps this node
		 * active.
		 */
		if (handlerList.isEmpty() && (noopHandlerFactory != null))
			sinkList.add(noopHandlerFactory.get());

		if (!handlerList.isEmpty()) {
			final DataSupport<T2> support = inputType.dataSupport();
			this.outputHandler = support.createListHandler(handlerList);

			updateListeners.forEach(h -> h.accept(this.outputHandler));

		} else {
			this.outputHandler = null;
		}

		/*
		 * When bypassed, the output is also the input, so we need to pass on update
		 * change to the upstream pipe so it can update with output changes.
		 */
		if ((this.outputHandler != oldHandler) || isEnabled)
			notifyUpstream();
	}
}