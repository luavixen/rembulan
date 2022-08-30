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

package net.sandius.rembulan.impl;

import net.sandius.rembulan.Conversions;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.TableFactory;
import net.sandius.rembulan.util.TraversableHashMap;

import java.util.Map;

/**
 * An immutable table.
 *
 * <p>The contents of this table may be queried, but not changed: the methods
 * {@link #rawset(Object, Object)}, {@link #rawset(long, Object)} and {@link #setMetatable(Table)}
 * will throw an {@link UnsupportedOperationException}.</p>
 *
 * <p>The table has no metatable.</p>
 *
 * <p>To instantiate a new {@code ImmutableTable}, use one of the static constructor methods
 * (e.g., {@link #of(Iterable)}), or a {@link ImmutableTable.Builder} as follows:</p>
 *
 * <pre>
 *     ImmutableTable t = new ImmutableTable.Builder()
 *         .add("key1", "value1")
 *         .add("key2", "value2")
 *         .build();
 * </pre>
 *
 * <p><b>A word of caution:</b> this class violates the expectation that all Lua tables are
 * mutable, and should therefore be used with care. In order to create a mutable copy of this
 * table, use {@link #newCopy(TableFactory)}.</p>
 */
public class ImmutableTable extends Table {

	private final TraversableHashMap<Object, Object> entries;

	public ImmutableTable(Map<Object, Object> map) {
		this.entries = new TraversableHashMap<>(map);
	}

	/**
	 * Returns an {@code ImmutableTable} based on the contents of the map {@code map}.
	 *
	 * <p>For every {@code key}-{@code value} pair in {@code map}, the behaviour of this method
	 * is similar to that of {@link Table#rawset(Object, Object)}:</p>
	 * <ul>
	 *   <li>when {@code value} is <b>nil</b> (i.e., {@code null}), then {@code key}
	 *     will not have any value associated with it in the resulting table;</li>
	 *   <li>if {@code key} is <b>nil</b> or <i>NaN</i>, a {@link IllegalArgumentException}
	 *     is thrown;</li>
	 *   <li>if {@code key} is a number that has an integer value, it is converted to that integer
	 *     value.</li>
	 * </ul>
	 *
	 * @param map  the map used to source the contents of the table, must not be {@code null}
	 * @return  an immutable table based on the contents of {@code map}
	 *
	 * @throws NullPointerException  if {@code entries} is {@code null}
	 * @throws IllegalArgumentException  if {@code map} contains a {@code null} or <i>NaN</i> key
	 */
	public static ImmutableTable of(Map<Object, Object> map) {
		return new ImmutableTable(map);
	}

	/**
	 * Returns an {@code ImmutableTable} based on the contents of the sequence of
	 * map entries {@code entries}.
	 *
	 * <p>For every {@code key}-{@code value} pair in {@code entries}, the behaviour of this
	 * method is similar to that of {@link Table#rawset(Object, Object)}:</p>
	 * <ul>
	 *   <li>when {@code value} is <b>nil</b> (i.e., {@code null}), then {@code key}
	 *     will not have any value associated with it in the resulting table;</li>
	 *   <li>if {@code key} is <b>nil</b> or <i>NaN</i>, a {@link IllegalArgumentException}
	 *     is thrown;</li>
	 *   <li>if {@code key} is a number that has an integer value, it is converted to that integer
	 *     value.</li>
	 * </ul>
	 *
	 * <p>Keys may occur multiple times in {@code entries} &mdash; only the last occurrence
	 * counts.</p>
	 *
	 * @param entries  the map entries, must not be {@code null}
	 * @return  an immutable table based on the contents of {@code entries}
	 *
	 * @throws NullPointerException  if {@code entries} is {@code null}
	 * @throws IllegalArgumentException  if {@code entries} contains an entry with
	 *                                   a {@code null} or <i>NaN</i> key
	 */
	public static ImmutableTable of(Iterable<Map.Entry<Object, Object>> entries) {
		Builder builder = new Builder();
		for (Map.Entry<Object, Object> entry : entries) {
			builder.add(entry.getKey(), entry.getValue());
		}
		return builder.build();
	}

	/**
	 * Returns a new table constructed using the supplied {@code tableFactory}, and copies
	 * the contents of this table to it.
	 *
	 * @param tableFactory  the table factory to use, must not be {@code null}
	 * @return  a mutable copy of this table
	 */
	public Table newCopy(TableFactory tableFactory) {
		Table copy = tableFactory.newTable(0, (entries.size() >>> 1) + (entries.size() >>> 2));
		for (Map.Entry<Object, Object> entry : entries.entrySet()) {
			copy.rawset(entry.getKey(), entry.getValue());
		}
		return copy;
	}

	@Override
	public int hashCode() {
		return entries.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) return true;
		if (other instanceof ImmutableTable) {
			return entries.equals(((ImmutableTable) other).entries);
		}
		return false;
	}

	@Override
	public Object rawget(Object key) {
		return entries.get(Conversions.normaliseKey(key));
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
		return entries.getFirstKey();
	}

	@Override
	public Object successorKeyOf(Object key) {
		key = Conversions.normaliseKey(key);
		if (key == null || (key instanceof Double && Double.isNaN(((Double) key).doubleValue()))) {
			throw new IllegalArgumentException("invalid key to 'next'");
		}
		try {
			return entries.getSuccessorKey(key);
		} catch (NullPointerException err) {
			throw new IllegalArgumentException("invalid key to 'next'", err);
		}
	}

	/**
	 * Builder class for constructing instances of {@link ImmutableTable}.
	 */
	public static class Builder {

		private final TraversableHashMap<Object, Object> entries;

		/**
		 * Constructs a new empty builder.
		 */
		public Builder() {
			entries = new TraversableHashMap<>();
		}

		/**
		 * Constructs a copy of the given builder (a copy constructor).
		 *
		 * @param builder  the original builder, must not be {@code null}
		 *
		 * @throws  NullPointerException  if {@code builder} is {@code null}
		 */
		public Builder(Builder builder) {
			entries = new TraversableHashMap<>(builder.entries);
		}

		/**
		 * Sets the value associated with the key {@code key} to {@code value}.
		 *
		 * <p>The behaviour of this method is similar to that of
		 * {@link Table#rawset(Object, Object)}:</p>
		 * <ul>
		 *   <li>when {@code value} is <b>nil</b> (i.e., {@code null}), the key {@code key}
		 *     will not have any value associated with it after this method returns;</li>
		 *   <li><b>nil</b> and <i>NaN</i> keys are rejected by throwing
		 *     a {@link IllegalArgumentException};</li>
		 *   <li>numeric keys with an integer value are converted to that integer value.</li>
		 * </ul>
		 *
		 * <p>The method returns {@code this}, allowing calls to this method to be chained.</p>
		 *
		 * @param key  the key, must not be {@code null} or <i>NaN</i>
		 * @param value  the value, may be {@code null}
		 * @return  this builder
		 *
		 * @throws IllegalArgumentException  when {@code key} is {@code null} or a <i>NaN</i>
		 */
		public Builder add(Object key, Object value) {
			key = Conversions.normaliseKey(key);
			if (key == null || (key instanceof Double && Double.isNaN(((Double) key).doubleValue()))) {
				throw new IllegalArgumentException("invalid table key: " + Conversions.toHumanReadableString(key));
			}
			if (value != null) {
				entries.put(key, value);
			} else {
				entries.remove(key);
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
		 * Constructs and returns a new immutable table based on the contents of this
		 * builder.
		 *
		 * @return  a new immutable table
		 */
		public ImmutableTable build() {
			return new ImmutableTable(entries);
		}

	}

}
