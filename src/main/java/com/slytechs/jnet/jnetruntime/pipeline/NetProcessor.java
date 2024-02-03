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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import com.slytechs.jnet.jnetruntime.pipeline.DataType.DataSupport;
import com.slytechs.jnet.jnetruntime.util.Registration;

/**
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public abstract class NetProcessor<T_US extends NetProcessor<T_US, T_IN, T_OUT>, T_IN, T_OUT>
		implements Comparable<NetProcessor<?, ?, ?>> {

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(NetProcessor<?, ?, ?> o) {
		return priority() - o.priority();
	}

	private final int priority;
	private final DataType outputType;
	private final DataType inputType;
	private final List<T_OUT> outputList = new ArrayList<>();
	private final List<NetProcessor<?, T_OUT, T_OUT>> outputProcessorList = new ArrayList<>();
	private final T_IN input;

	private String name;
	private BooleanSupplier enabled = () -> true;

	protected T_OUT output;

	@SuppressWarnings("unchecked")
	public NetProcessor(int priority, DataType inputType, DataType outputType) {
		this.priority = priority;
		this.outputType = outputType;
		this.inputType = inputType;
		this.input = (T_IN) this; // Force subclassed processors implement T_IN interface
		this.name = getClass().getSimpleName();
	}

	public Registration addOutput(T_OUT out) {
		final DataSupport<T_OUT> data = outputType.dataSupport();

		if (outputList.add(out))
			this.output = data.createListHandler(outputList);

		return () -> {
			if (outputList.remove(out))
				this.output = data.createListHandler(outputList);
		};
	}

	public Registration addOutput(T_OUT out, NetProcessor<?, T_OUT, T_OUT> processor) {
		outputProcessorList.add(processor);

		/*
		 * Store registration atomically, so that it can be mutated when relinking
		 * processors
		 */
		AtomicReference<Registration> mutableRegistration = new AtomicReference<>();
		mutableRegistration.set(addOutput(out));

		ProcessorLink<T_OUT> link = outputReplacement -> {
			Registration reg1 = mutableRegistration.get();
			try {
				reg1.unregister(); // Remove output to be replaced
			} finally {
				reg1 = addOutput(outputReplacement); // Add the replacement output
				mutableRegistration.set(reg1); // Update registration atomically
			}
		};

		/* Let the sink processor know about the link */
		processor.onLink(link);

		/* When processor is uninstalled/unlinked completely, allow for clean up */
		Registration reg3 = () -> {
			outputProcessorList.remove(processor);
			mutableRegistration.get().close();
			processor.onUnlink(link);
		};

		return reg3;
	}

	public void destroy() {
		Collections.fill(outputList, null);
		outputList.clear();
	}

//	protected AbstractProcessor(ProcessorGroup<?> group, int priority, DataType outputType) {
//		this(group, priority, outputType, outputType);
//	}
//
//	protected AbstractProcessor(ProcessorGroup<?> group, int priority, DataType outputType, DataType inputType) {
//		this.group = Objects.requireNonNull(group, "group").resolveByTypeOrCreate(outputType);
//		this.priority = priority;
//		this.outputType = outputType;
//		this.inputType = inputType;
//		this.name = getClass().getSimpleName();
//	}

	public T_US enable(boolean b) {
		return enable(() -> b);
	}

	public final T_US enable(BooleanSupplier b) {
		this.enabled = Objects.requireNonNull(b, "enabled");

		return us();
	}

	public void forEach(T_OUT out) {
		peek(out);
	}

	public <U> void forEach(T_OUT out, U user) {
		peek(out, user);
	}

	public final T_IN input() {
		return this.input;
	}

	public final DataType inputType() {
		return inputType;
	}

	public final boolean isEnabled() {
		return enabled.getAsBoolean();
	}

	public final String name() {
		return name;
	}

	public final T_US name(String newName) {
		this.name = newName;

		return us();
	}

	public final T_OUT output() {
		return output;
	}

	public final DataType outputType() {
		return outputType;
	}

	public T_US peek(T_OUT out) {
		addOutput(out);

		return us();
	}

	public <U> T_US peek(T_OUT out, U user) {
		DataSupport<T_OUT> support = outputType.dataSupport();
		return peek(support.wrap(out, user));
	}

	public final int priority() {
		return priority;
	}

	@SuppressWarnings("unchecked")
	protected final T_US us() {
		return (T_US) this;
	}

	public abstract void setup(NetProcessorContext context);

	public abstract void onLink(ProcessorLink<T_IN> link);

	public abstract void onUnlink(ProcessorLink<T_IN> link);

}
