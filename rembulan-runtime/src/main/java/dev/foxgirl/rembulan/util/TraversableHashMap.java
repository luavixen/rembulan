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

package dev.foxgirl.rembulan.util;

import java.util.*;

/**
 * A hash map similar in functionality to {@link java.util.LinkedHashMap}, with
 * additional methods for accessing successor and predecessor keys/entries.
 *
 * <p>To get the keys following and preceding {@code k} in the map, use
 * {@link #getSuccessorKey(Object)} and {@link #getPredecessorKey(Object)}, respectively.
 * In order to access the first and last keys in the traversal orders, use
 * {@link #getFirstKey()} and {@link #getLastKey()}.</p>
 *
 * @param <K>  key type
 * @param <V>  value type
 */
public class TraversableHashMap<K, V> implements Map<K, V> {

	/**
	 * A map entry (key-value pair) for {@link TraversableHashMap}.
	 *
	 * @param <K>  key type
	 * @param <V>  value type
	 */
	public interface Entry<K, V> extends Map.Entry<K, V> {
		/**
		 * Returns the entry that precedes this entry in the map, following
		 * insertion order.
		 *
		 * @return
		 *   the entry that precedes this entry or {@code null} if this entry
		 *   is the first entry in the map
		 */
		Entry<K, V> getPredecessor();

		/**
		 * Returns the entry that succeeds this entry in the map, following
		 * insertion order.
		 *
		 * @return
		 *   the entry that succeeds this entry or {@code null} if this entry
		 *   is the last entry in the map
		 */
		Entry<K, V> getSuccessor();
	}

	private static final class Node<K, V> implements Entry<K, V> {
		private final int hash;
		private final K key;
		private V value;
		private Node<K, V> next;

		private Node<K, V> predecessor;
		private Node<K, V> successor;

		private Node(int hash, K key, V value, Node<K, V> next) {
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}

		@Override public Entry<K, V> getPredecessor() { return predecessor; }
		@Override public Entry<K, V> getSuccessor() { return successor; }

		@Override public K getKey() { return key; }
		@Override public V getValue() { return value; }
		@Override public String toString() { return key + "=" + value; }

		@Override
		public V setValue(V newValue) {
			V oldValue = this.value;
			this.value = newValue;
			return oldValue;
		}

		@Override
		public int hashCode() {
			return hash ^ Objects.hashCode(value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj instanceof Map.Entry<?, ?>) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
				return
					Objects.equals(key, entry.getKey()) &&
					Objects.equals(value, entry.getValue());
			}
			return false;
		}

		private boolean matches(int hash, Object key) {
			return this.hash == hash && Objects.equals(this.key, key);
		}

		private void unlink() {
			Node<K, V> predecessor = this.predecessor;
			Node<K, V> successor = this.successor;
			if (predecessor != null) {
				predecessor.successor = successor;
			}
			if (successor != null) {
				successor.predecessor = predecessor;
			}
			this.predecessor = null;
			this.successor = null;
		}
	}

	private abstract class NodeIterator {
		private Node<K, V> nodeNext;

		protected NodeIterator() {
			nodeNext = head;
		}

		protected final Node<K, V> nextNode() {
			Node<K, V> node = nodeNext;
			nodeNext = node.successor;
			return node;
		}

		public final boolean hasNext() {
			return nodeNext != null;
		}

		public final void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private final class EntryIterator extends NodeIterator implements Iterator<Map.Entry<K, V>> {
		@Override public Map.Entry<K, V> next() { return nextNode(); }
	}

	private final class KeyIterator extends NodeIterator implements Iterator<K> {
		@Override public K next() { return nextNode().key; }
	}

	private final class ValueIterator extends NodeIterator implements Iterator<V> {
		@Override public V next() { return nextNode().value; }
	}

	private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override public int size() { return size; }
		@Override public Iterator<Map.Entry<K, V>> iterator() { return new EntryIterator(); }

		@Override public void clear() { throw new UnsupportedOperationException(); }
		@Override public boolean remove(Object obj) { throw new UnsupportedOperationException(); }

		@Override
		public boolean contains(Object obj) {
			throw new UnsupportedOperationException();
		}
	}

	private final class KeySet extends AbstractSet<K> {
		@Override public int size() { return size; }
		@Override public Iterator<K> iterator() { return new KeyIterator(); }

		@Override public void clear() { throw new UnsupportedOperationException(); }
		@Override public boolean remove(Object obj) { throw new UnsupportedOperationException(); }

		@Override
		public boolean contains(Object key) {
			return containsKey(key);
		}
	}

	private final class Values extends AbstractCollection<V> {
		@Override public int size() { return size; }
		@Override public Iterator<V> iterator() { return new ValueIterator(); }

		@Override public void clear() { throw new UnsupportedOperationException(); }
		@Override public boolean remove(Object obj) { throw new UnsupportedOperationException(); }

		@Override
		public boolean contains(Object value) {
			return containsValue(value);
		}
	}

	//
	// Since many common hashCode implementations produce values that are
	// suboptimal for use in hash tables, we use Fibonacci hashing (a kind of
	// multiplicative hashing) to scramble the result and provide good
	// distribution of hash values.
	// See: https://en.wikipedia.org/wiki/Hash_function#Fibonacci_hashing
	//
	// Note the use of the "shift" field instead of using the length of the
	// underlying "nodes" array directly with modulo. Since we are using
	// multiplicative hashing the upper bits of any given hash are very well
	// distributed while the lower bits may still be suboptimal, so by ensuring
	// the underlying array's length is a power of two and then applying
	// `hash >>> (32 - n)` where `nodes.length == 1 << n` we get great
	// distribution with little work.
	//
	private Node<K, V>[] nodes;
	private int shift;
	private int size;

	private Node<K, V> head;
	private Node<K, V> tail;

	private EntrySet entrySet;
	private KeySet keySet;
	private Values values;

	private static int hash(Object key) {
		// 0x9E3779B9 or 2654435769 is ⌊2^32/ϕ⌋, where ϕ is the golden ratio
		return key != null ? key.hashCode() * 0x9E3779B9 : 0;
	}

	private static final int CAPACITY_MIN = 4;
	private static final int CAPACITY_MAX = 1 << 30;

	private static int shift(int capacity) {
		// Clamp capacity into the range 4..2^30
		if (capacity < CAPACITY_MIN) capacity = CAPACITY_MIN;
		else if (capacity > CAPACITY_MAX) capacity = CAPACITY_MAX;
		// Calculate the largest shift that will contain the requested capacity
		return Integer.numberOfLeadingZeros(capacity - 1);
	}

	// Note that DEFAULT_LENGTH must be a power of two and DEFAULT_SHIFT must
	// be `32 - n` where `DEFAULT_LENGTH == 1 << n`.
	private static final int DEFAULT_LENGTH = 1 << 4;
	private static final int DEFAULT_SHIFT = 32 - 4;

	/**
	 * Constructs a new empty map with the default capacity.
	 */
	@SuppressWarnings("unchecked")
	public TraversableHashMap() {
		nodes = new Node[DEFAULT_LENGTH];
		shift = DEFAULT_SHIFT;
	}

	/**
	 * Constructs a new empty map with the specified initial capacity.
	 *
	 * @param capacity  initial capacity
	 * @throws IllegalArgumentException  if {@code capacity} is negative
	 */
	public TraversableHashMap(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Invalid initial capacity");
		}
		resize(shift(capacity));
	}

	/**
	 * Constructs a new map with the same contents as the given map.
	 *
	 * @param map  map to copy entries from
	 * @throws NullPointerException  if {@code map} is {@code null}
	 */
	public TraversableHashMap(Map<? extends K, ? extends V> map) {
		putAll(map);
	}

	private static <K, V> Node<K, V> traverse(Node<K, V> node, int hash, Object key) {
		for (; node != null; node = node.next) {
			if (node.matches(hash, key)) return node;
		}
		return null;
	}

	private Node<K, V> findNode(Object key) {
		int hash = hash(key);
		return traverse(nodes[hash >>> shift], hash, key);
	}

	private Node<K, V> findNodeValue(Object value) {
		for (Node<K, V> node = head; node != null; node = node.successor) {
			if (Objects.equals(node.value, value)) return node;
		}
		return null;
	}

	private Node<K, V> removeNode(int hash, Object key) {
		int index = hash >>> shift;
		Node<K, V> nodePrev = null;
		Node<K, V> node = nodes[index];
		while (node != null) {
			if (node.matches(hash, key)) {
				if (nodePrev == null) {
					nodes[index] = node.next;
				} else {
					nodePrev.next = node.next;
				}
				if (head == node) head = node.successor;
				if (tail == node) tail = node.predecessor;
				node.unlink();
				size--;
				return node;
			}
			nodePrev = node;
			node = node.next;
		}
		return null;
	}

	private V set(K key, V value) {
		int hash = hash(key);
		int index = hash >>> shift;

		Node<K, V> nodeHead = nodes[index];
		Node<K, V> node = traverse(nodeHead, hash, key);
		if (node != null) {
			return node.setValue(value);
		}

		Node<K, V> nodeNew = new Node<>(hash, key, value, nodeHead);
		if (head == null) {
			head = nodeNew;
		}
		if (tail != null) {
			tail.successor = nodeNew;
			nodeNew.predecessor = tail;
		}
		nodes[index] = tail = nodeNew;
		size++;

		return null;
	}

	private void resize(int shift) {
		@SuppressWarnings("unchecked")
		Node<K, V>[] nodes = new Node[1 << (32 - shift)];

		for (Node<K, V> node = head; node != null; node = node.successor) {
			int index = node.hash >>> shift;
			node.next = nodes[index]; nodes[index] = node;
		}

		this.nodes = nodes;
		this.shift = shift;
	}

	/**
	 * Returns the first entry in this map, following insertion order.
	 *
	 * @return  the first entry or {@code null} if the map is empty
	 */
	public Map.Entry<K, V> getFirst() {
		return head;
	}

	/**
	 * Returns the last entry in this map, following insertion order.
	 *
	 * @return  the last entry or {@code null} if the map is empty
	 */
	public Map.Entry<K, V> getLast() {
		return tail;
	}

	/**
	 * Returns the first key in this map, following insertion order.
	 *
	 * @return  the first key or {@code null} if the map is empty
	 */
	public K getFirstKey() {
		return head != null ? head.key : null;
	}

	/**
	 * Returns the last key in this map, following insertion order.
	 *
	 * @return  the last key or {@code null} if the map is empty
	 */
	public K getLastKey() { return tail != null ? tail.key : null; }

	/**
	 * Returns the entry associated with the given key.
	 *
	 * @param key  the key to find the associated entry of
	 * @return
	 *   the entry or {@code null} if no entry is associated with the given key
	 */
	public Entry<K, V> getEntry(Object key) {
		return findNode(key);
	}

	/**
	 * Returns the entry that precedes {@code key}'s associated entry,
	 * following insertion order.
	 *
	 * @param key  the key to find the predecessor of
	 * @return
	 *   the entry that precedes {@code key}'s associated entry or {@code null}
	 *   if {@code key}'s associated entry is the first entry in the map
	 * @throws NoSuchElementException
	 *   if this map has no entry associated with {@code key}
	 */
	public Entry<K, V> getPredecessor(Object key) {
		Node<K, V> node = findNode(key);
		if (node != null) return node.getPredecessor();
		throw new NoSuchElementException(key.toString());
	}

	/**
	 * Returns the entry that succeeds {@code key}'s associated entry,
	 * following insertion order.
	 *
	 * @param key  the key to find the successor of
	 * @return
	 *   the entry that succeeds {@code key}'s associated entry or {@code null}
	 *   if {@code key}'s associated entry is the last entry in the map
	 * @throws NoSuchElementException
	 *   if this map has no entry associated with {@code key}
	 */
	public Entry<K, V> getSuccessor(Object key) {
		Node<K, V> node = findNode(key);
		if (node != null) return node.getSuccessor();
		throw new NoSuchElementException(key.toString());
	}

	/**
	 * Returns the key that precedes {@code key} in the map, following
	 * insertion order.
	 *
	 * @param key  the key to find the predecessor of
	 * @return
	 *   the key of the entry that precedes {@code key}'s associated entry
	 *   or {@code null} if {@code key}'s associated entry is the first entry
	 *   in the map
	 * @throws NoSuchElementException
	 *   if this map has no entry associated with {@code key}
	 */
	public K getPredecessorKey(Object key) {
		Entry<K, V> entry = getPredecessor(key);
		return entry != null ? entry.getKey() : null;
	}

	/**
	 * Returns the key that succeeds {@code key} in the map, following
	 * insertion order.
	 *
	 * @param key  the key to find the successor of
	 * @return
	 *   the key of the entry that succeeds {@code key}'s associated entry
	 *   or {@code null} if {@code key}'s associated entry is the last entry
	 *   in the map
	 * @throws NoSuchElementException
	 *   if this map has no entry associated with {@code key}
	 */
	public K getSuccessorKey(Object key) {
		Entry<K, V> entry = getSuccessor(key);
		return entry != null ? entry.getKey() : null;
	}

	@Override
	public V get(Object key) {
		Node<K, V> node = findNode(key);
		return node != null ? node.value : null;
	}

	@Override
	public V remove(Object key) {
		Node<K, V> node = removeNode(hash(key), key);
		return node != null ? node.value : null;
	}

	@Override
	public boolean containsKey(Object key) {
		return findNode(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		return findNodeValue(value) != null;
	}

	@Override
	public V put(K key, V value) {
		int shift = this.shift;
		if (size >= 0xC0000000 >>> shift && shift > 2) {
			resize(shift - 1);
		}
		return set(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		if (map == null) {
			throw new NullPointerException("Argument 'map'");
		}

		int count = size + map.size();
		int shift = shift(count + (count >>> 1));
		if (shift < this.shift || nodes == null) {
			resize(shift);
		}

		for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
			set(entry.getKey(), entry.getValue());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void clear() {
		nodes = new Node[nodes.length];
		size = 0;
		head = null;
		tail = null;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns an <i>immutable</i> {@link Set} view of the mappings contained
	 * in this map. The set is backed by the map, so changes to the map are
	 * reflected in the set.
	 *
	 * @return  a set view of the mappings contained in this map
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		if (entrySet == null) {
			entrySet = new EntrySet();
		}
		return entrySet;
	}

	/**
	 * Returns an <i>immutable</i> {@link Set} view of the keys contained
	 * in this map. The set is backed by the map, so changes to the map are
	 * reflected in the set.
	 *
	 * @return  a set view of the keys contained in this map
	 */
	@Override
	public Set<K> keySet() {
		if (keySet == null) {
			keySet = new KeySet();
		}
		return keySet;
	}

	/**
	 * Returns an <i>immutable</i> {@link Collection} view of the values
	 * contained in this map. The collection is backed by the map, so changes
	 * to the map are reflected in the collection.
	 *
	 * @return  a collection view of the values contained in this map
	 */
	@Override
	public Collection<V> values() {
		if (values == null) {
			values = new Values();
		}
		return values;
	}

	@Override
	public String toString() {
		if (isEmpty()) return "{}";

		EntryIterator iter = new EntryIterator();
		StringBuilder builder = new StringBuilder();

		builder.append('{');
		for (;;) {
			Node<K, V> node = iter.nextNode();
			K key = node.key;
			V value = node.value;
			builder.append(key == this ? "<this>" : key);
			builder.append('=');
			builder.append(value == this ? "<this>" : value);
			if (iter.hasNext()) {
				builder.append(',');
				builder.append(' ');
			} else {
				builder.append('}');
				return builder.toString();
			}
		}
	}

	@Override
	public int hashCode() {
		return entrySet().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) obj;
			if (map.size() != size()) return false;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				Node<K, V> node = findNode(entry.getKey());
				if (node == null || !Objects.equals(node.value, entry.getValue())) return false;
			}
			return true;
		}
		return false;
	}

}
