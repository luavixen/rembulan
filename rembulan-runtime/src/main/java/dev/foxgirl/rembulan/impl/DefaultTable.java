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
 * Default implementation of a Lua table.
 */
public class DefaultTable extends Table {

	private static final class Factory implements TableFactory {

		private static final Factory INSTANCE = new Factory();

		@Override
		public Table newTable() {
			return new DefaultTable();
		}

		@Override
		public Table newTable(int arrayCapacity, int hashCapacity) {
			return new DefaultTable(arrayCapacity, hashCapacity);
		}

	}

	/**
	 * Returns the table factory for creating instances of {@code DefaultTable}.
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

	private void performConvert() {
		if (hashValues != null) {
			// starting at the index after the end of the array part
			Long index = (long) arrayValues.size() + 1L;
			// if index is contained within the hash part, move the value at
			// index into the array part, and increment index
			while (hashValues.containsKey(index)) {
				arrayValues.add(hashValues.remove(index++));
			}
		}
	}

	private void performRemove(long index) {
		// attempt to remove from array part
		if (arrayValues != null && index > 0L) {
			int size = arrayValues.size();
			// removing the last value in the array part
			if (index == (long) size) {
				// remove last value and any trailing nulls
				int i = size - 1;
				do { arrayValues.remove(i--); }
				while (i >= 0 && arrayValues.get(i) == null);
				return;
			}
			// removing any other value in the array part
			if (index < (long) size) {
				arrayValues.set((int) index - 1, null);
				return;
			}
		}
		// attempt to remove from hash part
		if (hashValues != null) {
			hashValues.remove(index);
		}
	}

	private void performSet(long index, Object value) {
		if (arrayValues != null) {
			int size = arrayValues.size();
			if (index > 0 && index <= (long) size) {
				arrayValues.set((int) index - 1, value);
				return;
			}
			if (index == (long) size + 1L) {
				arrayValues.add(value);
				performConvert();
				return;
			}
		} else if (index == 1L) {
			arrayValues = new ArrayList<>();
			arrayValues.add(value);
			performConvert();
			return;
		}
		if (hashValues == null) {
			hashValues = new TraversableHashMap<>();
		}
		hashValues.put(index, value);
	}

	@Override
	public void rawset(long index, Object value) {
		if (value == null) {
			performRemove(index);
		} else {
			performSet(index, Conversions.canonicalRepresentationOf(value));
		}
	}

	@Override
	public void rawset(Object key, Object value) {
		key = Conversions.normaliseKey(key);
		if (key == null) {
			throw new IllegalArgumentException("table index is nil");
		}
		if (key instanceof Double && Double.isNaN((Double) key)) {
			throw new IllegalArgumentException("table index is NaN");
		}
		if (key instanceof Long) {
			rawset(((Long) key).longValue(), value);
		} else {
			if (value != null) {
				if (hashValues == null) {
					hashValues = new TraversableHashMap<>();
				}
				hashValues.put(key, Conversions.canonicalRepresentationOf(value));
			} else if (hashValues != null) {
				hashValues.remove(key);
			}
		}
	}

	@Override
	public Object rawget(long index) {
		if (arrayValues != null && index > 0 && index <= (long) arrayValues.size()) {
			return arrayValues.get((int) index - 1);
		}
		if (hashValues != null) {
			return hashValues.get(index);
		}
		return null;
	}

	@Override
	public Object rawget(Object key) {
		if ((key = Conversions.normaliseKey(key)) instanceof Long) {
			return rawget(((Long) key).longValue());
		}
		return hashValues != null ? hashValues.get(key) : null;
	}

	@Override
	public long rawlen() {
		return arrayValues != null ? (long) arrayValues.size() : 0L;
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
	public Object successorKey(Object key) {
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

	@Override
	public Table copy(TableFactory factory) {
		Table copy = factory.newTable(
			arrayValues != null ? arrayValues.size() : 0,
			hashValues != null ? hashValues.size() + (hashValues.size() >>> 1) : 0
		);
		if (arrayValues != null) {
			ListIterator<Object> iter = arrayValues.listIterator();
			while (iter.hasNext()) {
				copy.rawset((long) iter.nextIndex(), iter.next());
			}
		}
		if (hashValues != null) {
			for (Map.Entry<?, ?> entry : hashValues.entrySet()) {
				copy.rawset(entry.getKey(), entry.getValue());
			}
		}
		return copy;
	}

	private final class KeyIterator implements Iterator<Object> {
		private final ListIterator<Object> arrayIterator;
		private final Iterator<Object> hashIterator;

		private KeyIterator() {
			arrayIterator = arrayValues != null
					? arrayValues.listIterator() : null;
			hashIterator = hashValues != null
					? hashValues.keySet().iterator() : null;
		}

		@Override
		public Object next() {
			if (arrayIterator != null && arrayIterator.hasNext()) {
				int index = arrayIterator.nextIndex(); arrayIterator.next();
				return Long.valueOf((long) index + 1L);
			}
			if (hashIterator != null && hashIterator.hasNext()) {
				return hashIterator.next();
			}
			throw new NoSuchElementException();
		}

		@Override
		public boolean hasNext() {
			return
					arrayIterator != null && arrayIterator.hasNext() ||
					hashIterator != null && hashIterator.hasNext();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private final class KeySet extends AbstractSet<Object> {
		@Override
		public Iterator<Object> iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			int size = 0;
			if (arrayValues != null) size += arrayValues.size();
			if (hashValues != null) size += hashValues.size();
			return size;
		}

		@Override
		public boolean contains(Object key) {
			if (key instanceof Long && arrayValues != null && !arrayValues.isEmpty()) {
				long index = ((Long) key).longValue();
				if (index > 0 && index <= (long) arrayValues.size()) return true;
			}
			if (hashValues != null && !hashValues.isEmpty()) {
				return hashValues.containsKey(key);
			}
			return false;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object key) {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public Set<Object> keySet() {
		return new KeySet();
	}

}
