/*
 * Copyright 2024 Sly Technologies Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slytechs.jnet.jnetruntime.pipeline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a context for processors in a pipeline, managing hierarchical
 * properties and relationships. This class provides a mechanism for storing and
 * retrieving typed properties in a hierarchical structure.
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * ProcessorContext rootContext = new ProcessorContext("root");
 * rootContext.property("globalSetting", 42);
 * 
 * ProcessorContext childContext = new ProcessorContext("child", rootContext);
 * childContext.property("localSetting", "value");
 * 
 * int globalSetting = childContext.getProperty(Integer.class, "globalSetting");
 * String localSetting = childContext.getProperty(String.class, "localSetting");
 * </pre>
 */
public final class ProcessorContext {

	/**
	 * Represents a key for storing properties in the context. Each key is a
	 * combination of a name and a type.
	 *
	 * @param <T> The type of the property associated with this key
	 */
	private class Key<T> {

		/** The name of the property. */
		private final String name;

		/** The type of the property. */
		private final Class<T> type;

		/**
		 * Constructs a new Key with the given name and type.
		 *
		 * @param name The name of the property
		 * @param type The class representing the type of the property
		 */
		public Key(String name, Class<T> type) {
			this.name = name;
			this.type = type;
		}

		/**
		 * Generates a hash code for this key.
		 *
		 * @return The hash code
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
		 * Checks if this key is equal to another object.
		 *
		 * @param obj The object to compare with
		 * @return true if the objects are equal, false otherwise
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
		 * Gets the enclosing ProcessorContext instance.
		 *
		 * @return The enclosing ProcessorContext instance
		 */
		private ProcessorContext getEnclosingInstance() {
			return ProcessorContext.this;
		}
	}

	/**
	 * Parses a path string into an array of path components. Removes all spaces and
	 * splits the string using '/' as the separator.
	 *
	 * @param path The path string to parse
	 * @return An array of path components
	 */
	public static String[] parsePath(String path) {
		if (path == null)
			return new String[0];

		return validatePath(path.replaceAll("\\s+", "").split("/"));
	}

	/**
	 * Validates the path components, ensuring none are null or blank.
	 *
	 * @param path The array of path components to validate
	 * @return The validated path array
	 * @throws IllegalArgumentException if any path component is invalid
	 */
	private static String[] validatePath(String[] path) throws IllegalArgumentException {
		for (var comp : path) {
			if (comp == null || comp.isBlank())
				throw new IllegalArgumentException("Path component cannot be null or blank");
		}

		return path;
	}

	/** The map storing the properties for this context. */
	private final Map<Key<?>, Object> map = new HashMap<>();

	/** The parent context, if any. */
	private final ProcessorContext parent;

	/** The root context of the hierarchy. */
	private final ProcessorContext root;

	/** The name of this context. */
	private final String name;

	/** The full path of this context in the hierarchy. */
	private final String[] path;

	/**
	 * Constructs a new root ProcessorContext with the given name.
	 *
	 * @param name The name of the context
	 * @throws IllegalArgumentException if the name is blank
	 */
	public ProcessorContext(String name) {
		this.name = Objects.requireNonNull(name, "name");
		this.parent = null;
		this.root = this;
		this.path = new String[] { name };

		if (name.isBlank())
			throw new IllegalArgumentException("Context name cannot be blank");
	}

	/**
	 * Constructs a new ProcessorContext with the given name and parent.
	 *
	 * @param name   The name of the context
	 * @param parent The parent context
	 * @throws IllegalArgumentException if the name is blank
	 */
	public ProcessorContext(String name, ProcessorContext parent) {
		this.name = Objects.requireNonNull(name, "name");
		this.parent = Objects.requireNonNull(parent, "parent");
		this.root = parent.root;
		this.path = Arrays.copyOf(parent.path, parent.path.length + 1);
		this.path[path.length - 1] = name;

		if (name.isBlank())
			throw new IllegalArgumentException("Context name cannot be blank");
	}

	/**
	 * Returns the name of this context.
	 *
	 * @return The name of the context
	 */
	public final String name() {
		return this.name;
	}

	/**
	 * Returns the full path of this context in the hierarchy.
	 *
	 * @return An array representing the path of the context
	 */
	public final String[] path() {
		return this.path;
	}

	/**
	 * Returns a string representation of this context's path.
	 *
	 * @return A string representation of the context's path
	 */
	@Override
	public String toString() {
		return Arrays.stream(this.path)
				.collect(Collectors.joining("/", "/", ""));
	}

	/**
	 * Finds a property of the specified type in this context or its ancestors.
	 *
	 * @param <T>  The type of the property
	 * @param type The class of the property type
	 * @return An Optional containing the property value if found, or empty if not
	 *         found
	 */
	public <T> Optional<T> findProperty(Class<T> type) {
		return Optional.ofNullable(getProperty(type));
	}

	/**
	 * Finds a property of the specified type and path in this context or its
	 * ancestors.
	 *
	 * @param <T>  The type of the property
	 * @param type The class of the property type
	 * @param path The path components to the property
	 * @return An Optional containing the property value if found, or empty if not
	 *         found
	 */
	public <T> Optional<T> findProperty(Class<T> type, String... path) {
		return Optional.ofNullable(getProperty(type, path));
	}

	/**
	 * Gets a property of the specified type from this context or its ancestors.
	 *
	 * @param <T>  The type of the property
	 * @param type The class of the property type
	 * @return The property value, or null if not found
	 */
	public <T> T getProperty(Class<T> type) {
		return getProperty(type, "");
	}

	/**
	 * Gets a property of the specified type and path from this context or its
	 * ancestors.
	 *
	 * @param <T>  The type of the property
	 * @param type The class of the property type
	 * @param path The path components to the property
	 * @return The property value, or null if not found
	 */
	public synchronized <T> T getProperty(Class<T> type, String... path) {
		Key<T> key = new Key<T>(String.join("/", path), type);
		return getProperty(key);
	}

	/**
	 * Internal method to get a property using a Key object.
	 *
	 * @param <T> The type of the property
	 * @param key The Key object representing the property
	 * @return The property value, or null if not found
	 */
	private synchronized <T> T getProperty(Key<T> key) {
		@SuppressWarnings("unchecked")
		T value = (T) map.get(key);
		if (value == null && parent != null)
			return parent.getProperty(key);

		return value;
	}

	/**
	 * Sets a property in this context with an empty name.
	 *
	 * @param <T>   The type of the property
	 * @param value The value of the property
	 * @return This ProcessorContext instance for method chaining
	 */
	public <T> ProcessorContext property(T value) {
		return property("", value);
	}

	/**
	 * Sets a property in this context with the specified name.
	 *
	 * @param <T>   The type of the property
	 * @param name  The name of the property
	 * @param value The value of the property
	 * @return This ProcessorContext instance for method chaining
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T> ProcessorContext property(String name, T value) {
		Key<T> key = new Key<T>(name, (Class<T>) value.getClass());
		map.put(key, value);
		return this;
	}
}