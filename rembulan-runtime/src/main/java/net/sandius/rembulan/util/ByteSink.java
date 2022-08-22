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

package net.sandius.rembulan.util;

import net.sandius.rembulan.ByteString;

import java.util.Arrays;

/**
 * ByteSink provides a very simple {@link java.lang.StringBuilder}-like
 * interface for creating byte arrays.
 */
public final class ByteSink {
	private static final byte[] EMPTY = new byte[0];
	private static final int CAPACITY_MIN = 32;

	private byte[] data;
	private int size;

	/**
	 * Constructs a new empty sink.
	 */
	public ByteSink() {
		this.data = EMPTY;
	}

	/**
	 * Constructs a new empty sink with the given capacity.
	 *
	 * @param capacity  initial capacity
	 * @throws IllegalArgumentException  if {@code capacity} is negative
	 */
	public ByteSink(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Argument 'capacity' is negative");
		}
		this.data = new byte[Math.max(capacity, CAPACITY_MIN)];
	}

	private void grow(int capacity) {
		byte[] dataOld = this.data;
		byte[] dataNew = new byte[capacity];
		System.arraycopy(dataOld, 0, dataNew, 0, this.size);
		this.data = dataNew;
	}

	private void grow() {
		int capacityOld = this.data.length;
		if (capacityOld >= CAPACITY_MIN) {
			int capacityNew = capacityOld << 1;
			if (capacityNew < capacityOld) {
				throw new OutOfMemoryError("ByteSink of capacity " + capacityOld + " cannot grow");
			}
			this.grow(capacityNew);
		} else {
			this.grow(CAPACITY_MIN);
		}
	}

	/**
	 * Appends the given byte to this sink.
	 *
	 * @param b  byte to append
	 * @return  this sink
	 */
	public ByteSink write(byte b) {
		if (this.data.length == this.size) {
			this.grow();
		}
		this.data[this.size++] = b;
		return this;
	}

	/**
	 * Appends the given byte array slice to this sink.
	 *
	 * @param bytes  byte array to slice
	 * @param count  number of bytes in the slice
	 * @param start  starting index of the slice
	 * @return  this sink
	 * @throws IllegalArgumentException
	 *   if the slice created by {@code count} and {@code start} is invalid
	 * @throws NullPointerException  if {@code bytes} is null
	 */
	public ByteSink write(byte[] bytes, int count, int start) {
		if (bytes == null) {
			throw new NullPointerException("Argument 'bytes'");
		}
		if (start < 0 || start > bytes.length) {
			throw new IllegalArgumentException("Argument 'start' " + start + " out of range");
		}
		if (count < 0 || count > bytes.length - start) {
			throw new IllegalArgumentException("Argument 'count' " + count + " out of range");
		}
		if (count == 0) {
			return this;
		}
		int sizeOld = this.size;
		int sizeNew = sizeOld + count;
		if (sizeNew < sizeOld) {
			throw new OutOfMemoryError("ByteSink cannot fit " + count + " bytes");
		}
		int capacityOld = this.data.length;
		int capacityNew = Math.max(Math.max(capacityOld, sizeNew), CAPACITY_MIN);
		if (capacityNew != capacityOld) {
			this.grow(capacityNew);
		}
		System.arraycopy(bytes, start, this.data, sizeOld, count);
		this.size = sizeNew;
		return this;
	}

	/**
	 * Appends the given byte array to this sink.
	 *
	 * @param bytes  byte array to append
	 * @return  this sink
	 * @throws NullPointerException  if {@code bytes} is null
	 */
	public ByteSink write(byte[] bytes) {
		return this.write(bytes, bytes.length, 0);
	}

	/**
	 * Replaces all occurrences of the given byte in the sink.
	 *
	 * @param a  target byte to replace
	 * @param b  new replacement byte
	 * @return  this sink
	 */
	public ByteSink replace(byte a, byte b) {
		for (int i = 0, length = data.length; i < length; i++) {
			if (data[i] == a) data[i] = b;
		}
		return this;
	}

	/**
	 * Replaces all occurrences of the given byte array in this sink.
	 *
	 * @param target  target byte sequence to replace
	 * @param replacement  new replacement byte sequence
	 * @return  this byte sink
	 * @throws IllegalArgumentException  if {@code target} is empty
	 * @throws NullPointerException
	 *   if either {@code target} or {@code replacement} is null
	 */
	public ByteSink replace(byte[] target, byte[] replacement) {
		if (target == null) {
			throw new NullPointerException("Argument 'target'");
		}
		if (replacement == null) {
			throw new NullPointerException("Argument 'replacement'");
		}
		int length = target.length;
		if (length == 0) {
			throw new IllegalArgumentException("Argument 'target' is empty");
		}
		final byte[] data = this.data;
		final int size = this.size;
		this.data = new byte[data.length];
		this.size = 0;
		int i = 0, imax = size - length;
		while (i < imax) {
			boolean replace = true;
			for (int ii = 0; ii < length; ii++) {
				if (data[i + ii] != target[ii]) {
					replace = false;
					break;
				}
			}
			if (replace) {
				this.write(replacement);
				i += length;
			} else {
				this.write(data[i]);
				i += 1;
			}
		}
		int remaining = size - i;
		if (remaining > 0) {
			this.write(data, remaining, i);
		}
		return this;
	}

	/**
	 * Clears the contents of this sink.
	 *
	 * @return  this sink
	 */
	public ByteSink clear() {
		this.size = 0;
		return this;
	}

	/**
	 * Returns the contents of this sink as a byte array.
	 *
	 * @return  contents of this sink
	 */
	public byte[] bytes() {
		int size = this.size;
		if (size == 0) {
			return EMPTY;
		}
		return Arrays.copyOf(this.data, size);
	}

	/**
	 * Returns the contents of this sink as a byte string.
	 *
	 * @return  contents of this sink
	 */
	public ByteString byteString() {
		return ByteString.copyOf(this.data, 0, size);
	}

	/**
	 * Returns the size of the contents of this sink.
	 *
	 * @return  contents size
	 */
	public int size() {
		return this.size;
	}

	/**
	 * Returns true if this sink is empty.
	 *
	 * @return  true if this sink is empty
	 */
	public boolean isEmpty() {
		return this.size == 0;
	}
}
