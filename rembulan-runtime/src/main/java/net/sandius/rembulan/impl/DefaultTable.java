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

import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * Default implementation of the Lua table storing all key-value pairs in a hashmap.
 * The table implementation does not support weak keys or values.
 */
public class DefaultTable extends Table {

	private static final class Factory implements TableFactory {
		private static final Factory INSTANCE = new Factory();

		@Override
		public Table newTable() {
			return new DefaultTable();
		}

		@Override
		public Table newTable(int array, int hash) {
			return new DefaultTable(array, hash);
		}
	}

	/**
	 * Returns the table factory for constructing instances of {@code DefaultTable}.
	 *
	 * @return  the table factory for {@code DefaultTable}s
	 */
	public static TableFactory factory() {
		return Factory.INSTANCE;
	}

	private ArrayList<Object> arrayValues;
	private TraversableHashMap<Object, Object> hashValues;

	private Table metatable;

	/**
	 * Constructs a new empty table.
	 */
	public DefaultTable() {
	}

	/**
	 * Constructs a new empty table with the given initial capacities.
	 *
	 * @param arrayCapacity  initial capacity for the array part
	 * @param hashCapacity  initial capacity for the hash part
	 */
	public DefaultTable(int arrayCapacity, int hashCapacity) {
		if (arrayCapacity > 0) {
			arrayValues = new ArrayList<>(arrayCapacity);
		}
		if (hashCapacity > 0) {
			hashValues = new TraversableHashMap<>(hashCapacity);
		}
	}

	@Override
	public Table getMetatable() {
		return metatable;
	}

	@Override
	public Table setMetatable(Table newMetatable) {
		Table oldMetatable = this.metatable;
		this.metatable = newMetatable;
		return oldMetatable;
	}

	private void arrayRemove(long index) {
		if (arrayValues != null && index > 0) {
			int size = arrayValues.size();
			if (index == (long) size) {
				int i = size - 1;
				do { arrayValues.remove(i--); }
				while (i >= 0 && arrayValues.get(i) == null);
				return;
			}
			if (index < (long) size) {
				arrayValues.set((int) index - 1, null);
				return;
			}
		}
		if (hashValues != null) {
			hashValues.remove(Long.valueOf(index));
		}
	}

	private void arrayConvert() {
		if (hashValues == null) return;
		for (Long key = (long) arrayValues.size() + 1L; hashValues.containsKey(key); key++) {
			arrayValues.add(hashValues.remove(key));
		}
	}

	@Override
	public void rawset(long index, Object value) {
		if (value == null) {
			arrayRemove(index);
			return;
		}
		if (arrayValues != null) {
			int size = arrayValues.size();
			if (index > 0 && index <= (long) size) {
				arrayValues.set((int) index - 1, value);
				return;
			}
			if (index == (long) size + 1L) {
				arrayValues.add(value);
				arrayConvert();
				return;
			}
		} else if (index == 1L) {
			arrayValues = new ArrayList<>();
			arrayValues.add(value);
			arrayConvert();
			return;
		}
		if (hashValues == null) {
			hashValues = new TraversableHashMap<>();
		}
		hashValues.put(Long.valueOf(index), value);
	}

	@Override
	public Object rawget(long index) {
		if (arrayValues != null && index > 0 && index <= (long) arrayValues.size()) {
			return arrayValues.get((int) index - 1);
		}
		if (hashValues != null) {
			return hashValues.get(Long.valueOf(index));
		}
		return null;
	}

	@Override
	public long rawlen() {
		return arrayValues != null ? (long) arrayValues.size() : 0L;
	}

	@Override
	public void rawset(Object key, Object value) {
		key = Conversions.normaliseKey(key);
		if (key == null) {
			throw new IllegalArgumentException("table index is nil");
		}
		if (key instanceof Double && Double.isNaN(((Double) key).doubleValue())) {
			throw new IllegalArgumentException("table index is NaN");
		}
		if (key instanceof Long) {
			rawset(((Long) key).longValue(), value);
		} else {
			if (value != null) {
				if (hashValues == null) {
					hashValues = new TraversableHashMap<>();
				}
				hashValues.put(key, value);
			} else if (hashValues != null) {
				hashValues.remove(key);
			}
		}
	}

	@Override
	public Object rawget(Object key) {
		if ((key = Conversions.normaliseKey(key)) instanceof Long) {
			return rawget(((Long) key).longValue());
		}
		return hashValues != null ? hashValues.get(key) : null;
	}

	@Override
	public Object initialKey() {
		if (arrayValues != null && !arrayValues.isEmpty()) {
			return Long.valueOf(1L);
		}
		if (hashValues != null) {
			return hashValues.getFirstKey();
		}
		return null;
	}

	@Override
	public Object successorKeyOf(Object key) {
		key = Conversions.normaliseKey(key);
		if (key != null && !(key instanceof Double && Double.isNaN(((Double) key).doubleValue()))) {
			if (key instanceof Long && arrayValues != null && !arrayValues.isEmpty()) {
				long index = ((Long) key).longValue();
				long size = arrayValues.size();
				if (index == size) {
					return hashValues != null ? hashValues.getFirstKey() : null;
				}
				if (index > 0 && index < size) {
					return Long.valueOf(index + 1L);
				}
			}
			if (hashValues != null) {
				try {
					return hashValues.getSuccessorKey(key);
				} catch (NoSuchElementException err) {
					throw new IllegalArgumentException("invalid key to 'next'", err);
				}
			}
		}
		throw new IllegalArgumentException("invalid key to 'next'");
	}

}
