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

package dev.foxgirl.rembulan;

import dev.foxgirl.rembulan.util.ByteIterator;
import dev.foxgirl.rembulan.util.ByteSink;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An immutable sequence of bytes.
 *
 * <p>The purpose of this class is to serve as a bridge between Java strings
 * (with their characters corresponding to 16-bit code units in the Basic
 * Multilingual Plane (BMP)) and Lua strings (raw 8-bit sequences).</p>
 *
 * <p>This class provides a natural lexicographical ordering that is consistent
 * with equals.</p>
 */
public final class ByteString implements Comparable<ByteString>, Iterable<Byte> {

	private final byte[] data;
	private final int hash;

	private volatile String string;

	private ByteString(byte[] data, int hash, String string) {
		this.data = data;
		this.hash = hash;
		this.string = string;
	}

	/**
	 * Returns the length of this byte string.
	 *
	 * @return  the length of this byte string
	 */
	public int length() {
		return data.length;
	}

	/**
	 * Returns true if {@link #length()} is zero.
	 *
	 * @return  true if this byte string is empty
	 */
	public boolean isEmpty() {
		return data.length == 0;
	}

	/**
	 * Returns the byte at position {@code index}.
	 *
	 * @param index  index into this byte string
	 * @return  the byte at the given index
	 * @throws ArrayIndexOutOfBoundsException
	 *   if {@code index} is out of bounds
	 */
	public byte byteAt(int index) {
		return data[index];
	}

	/**
	 * Returns a copy of the underlying byte array of this byte string.
	 *
	 * @return  newly created byte array
	 */
	public byte[] getBytes() {
		return data.clone();
	}

	public int indexOf(byte b, int start) {
		for (int i = start, imax = data.length; i < imax; i++) {
			if (data[i] == b) return i;
		}
		return -1;
	}
	public int indexOf(byte[] bytes, int start) {
		final int dataLength = data.length;
		final int bytesLength = bytes.length;

		if (bytesLength == 0) {
			throw new IllegalArgumentException("Argument 'bytes' is empty");
		}
		if (bytesLength == 1) {
			return indexOf(bytes[0], start);
		}

		for (int i = start, imax = dataLength - bytesLength; i < imax; i++) {
			boolean found = true;
			for (int ii = 0; ii < bytesLength; ii++) {
				if (data[i + ii] != bytes[ii]) {
					found = false;
					break;
				}
			}
			if (found) return i;
		}

		return -1;
	}
	public int indexOf(ByteString value, int start) {
		return indexOf(value.data, start);
	}

	public int indexOf(byte b) { return indexOf(b, 0); }
	public int indexOf(byte[] bytes) { return indexOf(bytes, 0); }
	public int indexOf(ByteString value) { return indexOf(value, 0); }

	public boolean contains(byte b) { return indexOf(b) >= 0; }
	public boolean contains(byte[] bytes) { return indexOf(bytes) >= 0; }
	public boolean contains(ByteString value) { return indexOf(value) >= 0; }

	public boolean startsWith(byte b) {
		return data.length > 0 && data[0] == b;
	}
	public boolean startsWith(byte[] bytes) {
		int length = bytes.length;
		if (length > data.length) return false;
		for (int i = 0; i < length; i++) {
			if (data[i] != bytes[i]) return false;
		}
		return true;
	}
	public boolean startsWith(ByteString prefix) {
		return startsWith(prefix.data);
	}

	public ByteString replace(byte a, byte b) {
		return new ByteSink().write(data).replace(a, b).byteString();
	}
	public ByteString replace(byte[] target, byte[] replacement) {
		return new ByteSink().write(data).replace(target, replacement).byteString();
	}
	public ByteString replace(ByteString target, ByteString replacement) {
		return replace(target.data, replacement.data);
	}

	/**
	 * Returns a substring of this byte string.
	 *
	 * @param start  starting index, inclusive
	 * @param end  ending index, exclusive
	 * @return  byte string result
	 * @throws IllegalArgumentException
	 *   if the substring created by {@code start} and {@code end} is invalid
	 */
	public ByteString substring(int start, int end) {
		return copyOf(data, start, end);
	}

	/**
	 * Returns a new byte string formed by concatenating this byte string with
	 * {@code other}.
	 *
	 * <p>This method directly concatenates the underlying byte arrays and will
	 * preserve unmappable and malformed characters occurring in the two
	 * strings.</p>
	 *
	 * @param other  byte string to concatenate with
	 * @return  byte string result
	 * @throws NullPointerException  if {@code other} is null
	 */
	public ByteString concat(ByteString other) {
		Objects.requireNonNull(other);

		final byte[] a = this.data;
		final byte[] b = other.data;
		final int aLength = a.length;
		final int bLength = b.length;

		final byte[] bytes = new byte[aLength + bLength];
		System.arraycopy(a, 0, bytes, 0, aLength);
		System.arraycopy(b, 0, bytes, aLength, bLength);

		return intern(bytes, null);
	}

	/**
	 * Compares this byte string lexicographically with {@code other}.
	 *
	 * <p>For the purposes of this ordering, bytes are interpreted as
	 * <i>unsigned</i> integers.</p>
	 *
	 * <p>This method directly compares the underlying byte arrays. It is
	 * therefore possible that for two byte {@code a} and {@code b}, the result
	 * of their comparison will not be the same as the result of comparing
	 * their Java string versions as provided by {@link #toString()}:</p>
	 * <pre>
	 *     int byteResult = a.compareTo(b);
	 *     int stringResult = a.toString().compareTo(b.toString());
	 *
	 *     // May fail!
	 *     assert Integer.signum(byteResult) == Integer.signum(stringResult);
	 * </pre>
	 *
	 * <p>This is done in order to ensure that the natural ordering provided by
	 * this method is consistent with equals.</p>
	 *
	 * @param other  byte string to be compared
	 * @return
	 *   a negative, zero, or positive integer if this byte string is
	 *   lexicographically lesser than, equal to or greater than {@code other}
	 * @throws NullPointerException  if {@code other} is null
	 */
	@Override
	public int compareTo(ByteString other) {
		Objects.requireNonNull(other);

		final byte[] a = this.data;
		final byte[] b = other.data;
		final int aLength = a.length;
		final int bLength = b.length;

		for (int i = 0, length = Math.min(aLength, bLength); i < length; i++) {
			int diff = (a[i] & 0xFF) - (b[i] & 0xFF);
			if (diff != 0) return diff;
		}

		return aLength - bLength;
	}

	/**
	 * Returns a hash code value for this byte string.
	 *
	 * <p>This value is precomputed at creation time and will not change, so
	 * this method acts as a getter and has little performance impact.</p>
	 *
	 * @return  hash code value
	 */
	@Override
	public int hashCode() {
		return hash;
	}

	/**
	 * Converts this byte string to a {@link java.lang.String} by encoding it
	 * as UTF-8.
	 *
	 * @return  string representation of this byte string
	 */
	@Override
	public String toString() {
		String string = this.string;
		if (string == null) {
			string = new String(data, StandardCharsets.UTF_8);
			this.string = string;
		}
		return string;
	}

	/**
	 * Converts this byte string to a {@link java.lang.String} by
	 * zero-extending each byte to a character. This method is the complement
	 * of {@link #fromRaw(String)}.
	 *
	 * @return  raw string representation of this byte string
	 */
	public String toRawString() {
		final int length = data.length;
		final char[] chars = new char[length];
		for (int i = 0; i < length; i++) {
			chars[i] = (char) (data[i] & 0xff);
		}
		return new String(chars);
	}

	private static final class ByteIteratorImpl implements ByteIterator {
		private final byte[] bytes;
		private int index;

		private ByteIteratorImpl(byte[] bytes) {
			this.bytes = bytes;
			this.index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < bytes.length;
		}

		@Override
		public byte nextByte() {
			if (!hasNext()) throw new NoSuchElementException();
			return bytes[index++];
		}

		@Override
		public Byte next() {
			return nextByte();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Returns an iterator over the bytes in this byte string.
	 *
	 * @return  byte iterator instance
	 */
	@Override
	public ByteIterator iterator() {
		return new ByteIteratorImpl(data);
	}


	/**
	 * Returns a byte string containing a copy of {@code bytes}.
	 *
	 * @param bytes  byte array to copy
	 * @return  byte string instance
	 * @throws NullPointerException  if {@code bytes} is null
	 */
	public static ByteString copyOf(byte[] bytes) {
		if (bytes == null) {
			throw new NullPointerException("Argument 'bytes'");
		}
		if (bytes.length == 0) {
			return EMPTY;
		}
		return intern(bytes.clone(), null);
	}

	/**
	 * Returns a byte string containing a slice of {@code bytes}.
	 *
	 * @param bytes  byte array to slice
	 * @param start  starting index, inclusive
	 * @param end  ending index, exclusive
	 * @return  byte string instance
	 * @throws IllegalArgumentException
	 *   if the slice created by {@code start} and {@code end} is invalid
	 * @throws NullPointerException  if {@code bytes} is null
	 */
	public static ByteString copyOf(byte[] bytes, int start, int end) {
		if (bytes == null) {
			throw new NullPointerException("Argument 'bytes'");
		}
		if (start == end) {
			return EMPTY;
		}
		return intern(Arrays.copyOfRange(bytes, start, end), null);
	}

	/**
	 * Returns a byte string containing the UTF-8 representation of the given
	 * string.
	 *
	 * <p>This method differs from {@link #of(String)} in that it may force the
	 * computation of lazily-evaluated properties of the resulting byte string
	 * at instantiation time and cache them for use at runtime.</p>
	 *
	 * @param string  string to convert
	 * @return  byte string instance
	 * @throws NullPointerException  if {@code string} is null
	 */
	public static ByteString constOf(String string) {
		return of(string);
	}

	/**
	 * Returns a byte string containing the UTF-8 representation of the given
	 * string.
	 *
	 * @param string  string to convert
	 * @return  byte string instance
	 * @throws NullPointerException  if {@code string} is null
	 */
	public static ByteString of(String string) {
		if (string == null) {
			throw new NullPointerException("Argument 'string'");
		}
		if (string.isEmpty()) {
			return EMPTY;
		}
		return intern(string.getBytes(StandardCharsets.UTF_8), string);
	}

	/**
	 * Returns a byte string containing the provided {@code string} encoded
	 * with the given {@code charset}.
	 *
	 * @param string  string to convert
	 * @param charset charset to use
	 * @return  byte string instance
	 * @throws NullPointerException
	 *   if either {@code string} or {@code charset} is null
	 */
	public static ByteString of(String string, Charset charset) {
		if (string == null) {
			throw new NullPointerException("Argument 'string'");
		}
		if (charset == null) {
			throw new NullPointerException("Argument 'charset'");
		}
		if (string.isEmpty()) {
			return EMPTY;
		}
		return intern(string.getBytes(charset), null);
	}

	/**
	 * Returns a byte string corresponding to bytes in the given string by
	 * taking the least significant byte of each character.
	 *
	 * @param string  string to convert
	 * @return  byte string instance
	 * @throws NullPointerException  if {@code string} is null
	 */
	public static ByteString fromRaw(String string) {
		if (string == null) {
			throw new NullPointerException("Argument 'string'");
		}
		if (string.isEmpty()) {
			return EMPTY;
		}
		final char[] chars = string.toCharArray();
		final byte[] bytes = new byte[chars.length];
		for (int i = 0, length = bytes.length; i < length; i++) {
			bytes[i] = (byte) ((int) chars[i] & 0xFF);
		}
		return intern(bytes, null);
	}

	/**
	 * Returns an empty byte string.
	 *
	 * @return  byte string instance with zero length
	 */
	public static ByteString empty() {
		return EMPTY;
	}


	/**
	 * Hashes an array of bytes using the FNV-1a hash function.
	 *
	 * @param bytes  array of bytes to hash
	 * @return  hash code derived from {@code bytes}
	 * @throws NullPointerException  if {@code bytes} is null
	 */
	private static int fnv1a(byte[] bytes) {
		int hash = 0x811C9DC5;
		for (byte b : bytes) {
			hash ^= b & 0xFF;
			hash *= 16777619;
		}
		return hash;
	}

	private static final class InternEntry extends WeakReference<ByteString> {
		private final int hash;
		private InternEntry next;

		private InternEntry(ByteString referent, InternEntry next) {
			super(referent, INTERN_QUEUE);
			this.hash = referent.hash;
			this.next = next;
		}
	}

	private static final ReferenceQueue<ByteString> INTERN_QUEUE = new ReferenceQueue<>();
	private static InternEntry[] INTERN_TABLE = new InternEntry[64];
	private static int INTERN_SIZE = 0;

	private static final ByteString EMPTY = intern(new byte[0], "");

	/**
	 * Finds or creates an interned byte string for the given byte array. Note
	 * that the byte array will be used directly, not copied.
	 *
	 * @param bytes  byte array to wrap
	 * @param string
	 *   optional Java string to associate with the resulting byte string, note that
	 *   {@code Arrays.equals(bytes, string.getBytes(StandardCharsets.UTF_8))} must be true.
	 * @return  interned byte string
	 * @throws NullPointerException  if {@code bytes} is null
	 */
	private static ByteString intern(byte[] bytes, String string) {
		final int hash = fnv1a(bytes);

		synchronized (INTERN_QUEUE) {
			final int mask = INTERN_TABLE.length - 1;

			for (;;) {
				final InternEntry entry = (InternEntry) INTERN_QUEUE.poll();
				if (entry == null) break;

				final int index = entry.hash & mask;
				InternEntry prev = INTERN_TABLE[index];
				InternEntry next;

				if (entry == prev) {
					INTERN_TABLE[index] = entry.next;
					INTERN_SIZE--;
				} else {
					while (prev != null) {
						if (entry == (next = prev.next)) {
							prev.next = entry.next;
							INTERN_SIZE--;
							break;
						}
						prev = next;
					}
				}

				entry.next = null;
				entry.clear();
			}

			for (InternEntry entry = INTERN_TABLE[hash & mask]; entry != null; entry = entry.next) {
				final ByteString value = entry.get();
				if (
					value != null &&
					value.hash == hash &&
					Arrays.equals(value.data, bytes)
				) {
					if (string != null && value.string == null) {
						value.string = string;
					}
					return value;
				}
			}

			return internCreate(bytes, hash, string);
		}
	}

	private static ByteString internCreate(byte[] bytes, int hash, String string) {
		int length = INTERN_TABLE.length;

		if (INTERN_SIZE >= (length >>> 1) + (length >>> 2)) {
			final InternEntry[] oldTable = INTERN_TABLE;
			final InternEntry[] newTable = INTERN_TABLE = new InternEntry[length <<= 1];
			final int mask = length - 1;

			for (InternEntry entry : oldTable) {
				while (entry != null) {
					final InternEntry entry0 = entry; entry = entry0.next;
					final int index = entry0.hash & mask;
					entry0.next = newTable[index];
					newTable[index] = entry0;
				}
			}
		}

		final ByteString value = new ByteString(bytes, hash, string);
		final int index = hash & (length - 1);
		INTERN_TABLE[index] = new InternEntry(value, INTERN_TABLE[index]);
		INTERN_SIZE++;
		return value;
	}

}
