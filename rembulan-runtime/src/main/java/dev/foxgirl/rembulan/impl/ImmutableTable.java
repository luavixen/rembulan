/*
 * Copyright 2016 Miroslav Janíček
 * Copyright 2022 Lua MacDougall <lua@foxgirl.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.foxgirl.rembulan.impl;

import dev.foxgirl.rembulan.Conversions;
import dev.foxgirl.rembulan.Table;
import dev.foxgirl.rembulan.TableFactory;
import dev.foxgirl.rembulan.util.TraversableHashMap;

import java.util.*;

/**
 * Immutable implementation of a Lua table backed by a hash map. Immutable
 * tables cannot have metatables.
 *
 * <p>The contents of this table may be queried, but not changed: the methods
 * {@link #rawset(Object, Object)}, {@link #rawset(long, Object)} and
 * {@link #setMetatable(Table)} will throw an
 * {@link UnsupportedOperationException}.</p>
 *
 * <p>To instantiate a new {@code ImmutableTable}, use either {@link #of(Map)}
 * or a {@link ImmutableTable.Builder} as follows:</p>
 *
 * <pre>
 *     ImmutableTable table = new ImmutableTable.Builder()
 *         .add("key1", "value1")
 *         .add("key2", "value2")
 *         .build();
 * </pre>
 *
 * <p><b>A word of caution:</b> this class violates the expectation that all
 * Lua tables are mutable, and should be used with care. In order to create a
 * mutable copy of this table, use {@link #copy(TableFactory)}.</p>
 */
public class ImmutableTable extends Table {

	private final TraversableHashMap<Object, Object> values;

	private ImmutableTable(TraversableHashMap<Object, Object> values) {
		this.values = values;
	}

	/**
	 * Creates a new {@code ImmutableTable} from the contents of {@code map}.
	 *
	 * <p>For every key-value pair in {@code map}, the behaviour of this method
	 * is similar to that of {@link Table#rawset(Object, Object)}:</p>
	 * <ul>
	 *   <li>when {@code value} is <b>nil</b> (i.e., {@code null}), then {@code key}
	 *     will not have any value associated with it in the resulting table;</li>
	 *   <li>if {@code key} is <b>nil</b> or <i>NaN</i>, a {@link IllegalArgumentException}
	 *     is thrown;</li>
	 *   <li>if {@code key} is a number that has an integer value, it is converted
	 *     to that integer value.</li>
	 * </ul>
	 *
	 * @param map  the map to copy from, must not be {@code null}
	 * @return  a new immutable table
	 *
	 * @throws NullPointerException  if {@code entries} is {@code null}
	 * @throws IllegalArgumentException  if {@code map} contains a {@code null} or <i>NaN</i> key
	 */
	public static ImmutableTable of(Map<?, ?> map) {
		Builder builder = new Builder(map.size());
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			builder.add(entry.getKey(), entry.getValue());
		}
		return builder.build();
	}

	@Override
	public Object rawget(Object key) {
		return values.get(Conversions.normaliseKey(key));
	}

	/**
	 * Throws an {@link UnsupportedOperationException}, since this table is immutable.
	 *
	 * @param key  ignored
	 * @param value  ignored
	 *
	 * @throws UnsupportedOperationException  every time this method is called
	 */
	@Override
	public void rawset(Object key, Object value) {
		throw new UnsupportedOperationException("table is immutable");
	}

	/**
	 * Throws an {@link UnsupportedOperationException}, since this table is immutable.
	 *
	 * @param idx  ignored
	 * @param value  ignored
	 *
	 * @throws UnsupportedOperationException  every time this method is called
	 */
	@Override
	public void rawset(long idx, Object value) {
		throw new UnsupportedOperationException("table is immutable");
	}

	@Override
	public Table getMetatable() {
		return null;
	}

	/**
	 * Throws an {@link UnsupportedOperationException}, since this table is immutable.
	 *
	 * @param mt  ignored
	 * @return  nothing (always throws an exception)
	 *
	 * @throws UnsupportedOperationException  every time this method is called
	 */
	@Override
	public Table setMetatable(Table mt) {
		throw new UnsupportedOperationException("table is immutable");
	}

	@Override
	public Object initialKey() {
		return values.getFirstKey();
	}

	@Override
	public Object successorKey(Object key) {
		try {
			return values.getSuccessorKey(Conversions.normaliseKey(key));
		} catch (NullPointerException err) {
			throw new IllegalArgumentException("invalid key to 'next'", err);
		}
	}

	@Override
	public Set<Object> keySet() {
		return values.keySet();
	}

	@Override
	public Table copy(TableFactory factory) {
		Table copy = factory.newTable(0, values.size() + (values.size() >>> 1));
		for (Map.Entry<Object, Object> entry : values.entrySet()) {
			copy.rawset(entry.getKey(), entry.getValue());
		}
		return copy;
	}

	/**
	 * Builder class for creating instances of {@link ImmutableTable}.
	 */
	public static class Builder {

		private static final class Entry {
			private final Object key;
			private final Object value;

			private Entry(Object key, Object value) {
				this.key = key;
				this.value = value;
			}
		}

		private final List<Entry> entries;

		/**
		 * Constructs a new empty builder.
		 */
		public Builder() {
			this.entries = new ArrayList<>();
		}

		/**
		 * Constructs a new empty builder with the given capacity.
		 *
		 * @param capacity  initial capacity
		 * @throws IllegalArgumentException  if {@code capacity} is negative
		 */
		public Builder(int capacity) {
			this.entries = new ArrayList<>(capacity);
		}

		/**
		 * Constructs a copy of the given builder.
		 *
		 * @param builder  the original builder, must not be {@code null}
		 *
		 * @throws  NullPointerException  if {@code builder} is {@code null}
		 */
		public Builder(Builder builder) {
			this.entries = new ArrayList<>(builder.entries);
		}

		/**
		 * Sets the value associated with the key {@code key} to {@code value}.
		 *
		 * <p>The behaviour of this method is similar to that of
		 * {@link Table#rawset(Object, Object)}:</p>
		 * <ul>
		 *   <li>when {@code value} is <b>nil</b> (i.e., {@code null}), the
		 *     key {@code key} will not have any value associated with it;</li>
		 *   <li>both <b>nil</b> and <i>NaN</i> keys are rejected by throwing
		 *     a {@link IllegalArgumentException};</li>
		 *   <li>numeric keys with an integer value are converted to that
		 *     integer value.</li>
		 * </ul>
		 *
		 * <p>This method returns {@code this}, allowing calls to this method to be chained.</p>
		 *
		 * @param key  the key, must not be {@code null} or <i>NaN</i>
		 * @param value  the value, may be {@code null}
		 * @return  this builder
		 *
		 * @throws IllegalArgumentException  when {@code key} is {@code null} or a <i>NaN</i>
		 */
		public Builder add(Object key, Object value) {
			if (value != null) {
				key = Conversions.normaliseKey(key);
				if (key == null) {
					throw new IllegalArgumentException("table index is nil");
				}
				if (key instanceof Double && Double.isNaN(((Double) key).doubleValue())) {
					throw new IllegalArgumentException("table index is NaN");
				}
				entries.add(new Entry(key, Conversions.canonicalRepresentationOf(value)));
			}
			return this;
		}

		/**
		 * Clears the builder.
		 */
		public void clear() {
			entries.clear();
		}

		/**
		 * Creates a new immutable table from the contents of this builder.
		 *
		 * @return  a new immutable table
		 */
		public ImmutableTable build() {
			TraversableHashMap<Object, Object> values =
					new TraversableHashMap<>(entries.size() + (entries.size() >>> 1));

			for (Entry entry : entries) {
				Object key = entry.key;
				Object value = entry.value;
				if (value == null) {
					values.remove(key);
				} else {
					values.put(key, value);
				}
			}

			return new ImmutableTable(values);
		}

	}

}
