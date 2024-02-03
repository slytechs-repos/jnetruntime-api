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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The Class NetProcessorContext.
 */
public final class NetProcessorContext {

	/**
	 * The Class Key.
	 *
	 * @param <T> the generic type
	 */
	private class Key<T> {

		/** The name. */
		private final String name;

		/** The type. */
		private final Class<T> type;

		/**
		 * Instantiates a new key.
		 *
		 * @param name the name
		 * @param type the type
		 */
		public Key(String name, Class<T> type) {
			this.name = name;
			this.type = type;

		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + Objects.hash(name, type);
			return result;
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			return Objects.equals(name, other.name) && Objects.equals(type, other.type);
		}

		/**
		 * Gets the enclosing instance.
		 *
		 * @return the enclosing instance
		 */
		private NetProcessorContext getEnclosingInstance() {
			return NetProcessorContext.this;
		}

	}

	/** The map. */
	private final Map<Key<?>, Object> map = new HashMap<>();

	/** The parent. */
	private final NetProcessorContext parent;

	/**
	 * Instantiates a new net processor context.
	 */
	public NetProcessorContext() {
		this.parent = null;
	}

	/**
	 * Instantiates a new net processor context.
	 *
	 * @param parent the parent
	 */
	public NetProcessorContext(NetProcessorContext parent) {
		this.parent = parent;

		property("parent", parent);
	}

	/**
	 * Find property.
	 *
	 * @param <T>  the generic type
	 * @param type the type
	 * @return the optional
	 */
	public <T> Optional<T> findProperty(Class<T> type) {
		return Optional.ofNullable(getProperty(type));
	}

	/**
	 * Find property.
	 *
	 * @param <T>  the generic type
	 * @param name the name
	 * @param type the type
	 * @return the optional
	 */
	public <T> Optional<T> findProperty(String name, Class<T> type) {
		return Optional.ofNullable(getProperty(name, type));
	}

	/**
	 * Gets the property.
	 *
	 * @param <T>  the generic type
	 * @param type the type
	 * @return the property
	 */
	public <T> T getProperty(Class<T> type) {
		return getProperty("", type);
	}

	/**
	 * Gets the property.
	 *
	 * @param <T>  the generic type
	 * @param name the name
	 * @param type the type
	 * @return the property
	 */
	public synchronized <T> T getProperty(String name, Class<T> type) {
		Key<T> key = new Key<T>(name, type);

		return getProperty(key);
	}

	/**
	 * Gets the property.
	 *
	 * @param <T> the generic type
	 * @param key the key
	 * @return the property
	 */
	private synchronized <T> T getProperty(Key<T> key) {
		@SuppressWarnings("unchecked")
		T value = (T) map.get(key);
		if (value == null && parent != null)
			return parent.getProperty(key);

		return value;
	}

	/**
	 * Property.
	 *
	 * @param <T>   the generic type
	 * @param value the value
	 * @return the net processor context
	 */
	public <T> NetProcessorContext property(T value) {
		return property("", value);
	}

	/**
	 * Property.
	 *
	 * @param <T>   the generic type
	 * @param name  the name
	 * @param value the value
	 * @return the net processor context
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T> NetProcessorContext property(String name, T value) {
		Key<T> key = new Key<T>(name, (Class<T>) value.getClass());

		map.put(key, value);

		return this;
	}

}