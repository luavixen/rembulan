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

import dev.foxgirl.rembulan.runtime.Dispatch;
import dev.foxgirl.rembulan.runtime.ExecutionContext;

import java.util.Set;

/**
 * An abstract class representing a Lua table.
 *
 * <p><b>Note on equality:</b> according to §3.4.4 of the Lua Reference Manual,
 * tables {@code a} and {@code b} are expected to be equal if and only if they are
 * the same object. However, {@link Ordering#isRawEqual(Object, Object)} compares
 * tables using {@link Object#equals(Object)}. <b>Exercise caution when overriding
 * {@code equals()}.</b></p>
 */
public abstract class Table extends LuaObject {

	/**
	 * Retrieves the value associated with the given {@code key}, returning {@code null}
	 * when {@code key} has no value associated with it.
	 *
	 * <p>Implementations of this method must ensure that the Lua rules for valid
	 * table keys are honoured, e.g. by normalising keys using
	 * {@link Conversions#normaliseKey(Object)}.</p>
	 *
	 * <p>This method provides <i>raw</i> access to the table. For non-raw access
	 * (i.e., handling the {@code __index} metamethod), use
	 * {@link Dispatch#index(ExecutionContext, Table, Object)}.</p>
	 *
	 * @param key  the key, may be {@code null}
	 * @return  the value associated with {@code key}, or {@code null} when there is no
	 *          value associated with {@code key} in this table
	 */
	public abstract Object rawget(Object key);

	/**
	 * Retrieves the value associated with the given integer {@code idx}, returning
	 * {@code null} when {@code idx} has no value associated with it.
	 *
	 * <p>This method must be functionally equivalent to {@link #rawget(Object)} with the
	 * corresponding boxed key. However, implementations of this method may optimise the retrieval
	 * in this case, since the type of the key is known at compile-time.</p>
	 *
	 * <p>This method provides <i>raw</i> access to the table. For non-raw access
	 * (i.e., handling the {@code __index} metamethod), use
	 * {@link Dispatch#index(ExecutionContext, Table, long)}.</p>
	 *
	 * @param idx  the integer key
	 * @return  the value associated with {@code idx}, or {@code null} when there is no
	 *          value associated with {@code idx} in this table
	 */
	public Object rawget(long idx) {
		return rawget(Long.valueOf(idx));
	}

	/**
	 * Sets the value associated with the key {@code key} to {@code value}. When {@code value}
	 * is {@code null}, removes {@code key} from the table.
	 *
	 * <p>When {@code key} is {@code null} (i.e., a <b>nil</b>) or a <i>NaN</i>,
	 * an {@link IllegalArgumentException} is thrown.</p>
	 *
	 * <p>This method provides <i>raw</i> access to the table. For non-raw access
	 * (i.e., handling the {@code __newindex} metamethod), use
	 * {@link Dispatch#setindex(ExecutionContext, Table, Object, Object)}.</p>
	 *
	 * <p><b>Implementation notes:</b> Implementations of this method must ensure that
	 * the behaviour of this method conforms to the Lua semantics as delineated in the Lua
	 * Reference Manual. In particular:</p>
	 * <ul>
	 *     <li>float keys that have an integer value must be treated as integer keys
	 *     (e.g. by using {@link Conversions#normaliseKey(Object)}</li>
	 *     <li>updates of the value associated with the key {@code "__mode"}</li>
	 * </ul>
	 *
	 * @param key  the key, must not be {@code null} or <i>NaN</i>
	 * @param value  the value to associate with {@code key}, may be {@code null}
	 *
	 * @throws IllegalArgumentException  when {@code key} is {@code null} or a <i>NaN</i>
	 */
	public abstract void rawset(Object key, Object value);

	/**
	 * Sets the value associated with the integer key {@code idx} to {@code value}.
	 * When {@code value} is {@code null}, removes {@code idx} from the table.
	 *
	 * <p>This method must be functionally equivalent to {@link #rawset(Object,Object)} with the
	 * corresponding boxed key. However, implementations of this method may be more optimised
	 * than in the generic case, since the type of the key is known at compile-time.</p>
	 *
	 * <p>This method provides <i>raw</i> access to the table. For non-raw access
	 * (i.e., handling the {@code __newindex} metamethod), use
	 * {@link Dispatch#setindex(ExecutionContext, Table, long, Object)}.</p>
	 *
	 * @param idx  the integer key
	 * @param value  the value to associate with {@code idx}, may be {@code null}
	 */
	public void rawset(long idx, Object value) {
		rawset(Long.valueOf(idx), value);
	}

	/**
	 * If this table is a sequence, returns the length of this sequence.
	 *
	 * <p>According to §2.1 of the Lua Reference Manual, a <i>sequence</i> is</p>
	 * <blockquote>
	 *     a table where the set of all positive numeric keys is equal to {1..<i>n</i>} for some
	 *     non-negative integer <i>n</i>, which is called the length of the sequence
	 * </blockquote>
	 *
	 * <p>Note that when this table is not a sequence, the return value of this method
	 * is undefined.</p>
	 *
	 * @return  the length of the sequence if this table is a sequence
	 */
	public long rawlen() {
		long idx = 1;

		while (idx >= 0 && rawget(idx) != null) {
			idx <<= 1;
		}

		// if idx overflows (idx < 0), don't check rawget(idx)

		if (idx == 1) {
			return 0;
		} else {
			// binary search in [idx >>> 1, idx]

			long min = idx >>> 1;
			long max = idx;

			// invariant: (min > 0 && rawget(min) != null) && (max < 0 || rawget(max) == null)

			while (min + 1 != max) {
				// works even if max == (1 << 63)
				long mid = (min + max) >>> 1;
				if (rawget(mid) == null) {
					max = mid;
				} else {
					min = mid;
				}
			}

			// min + 1 == max; given the invariant, min is the result

			return min;
		}
	}

	/**
	 * Returns the initial key for iterating through the set of keys in this table.
	 *
	 * <p>Conceptually speaking, all keys in this table are totally ordered; this method
	 * returns the minimal key.</p>
	 *
	 * <p>The key returned by this method, together with the subsequent calls
	 * to {@link #successorKey(Object)} will visit all keys in this table exactly once
	 * (in an unspecified order):</p>
	 * <pre>
	 *     Object k = table.initialIndex();
	 *     while (k != null) {
	 *         // process the key k
	 *         k = table.nextIndex(k);
	 *     }
	 *     // at this point, we visited all keys in table exactly once
	 * </pre>
	 *
	 * @return  an initial key for iteration through all keys in this table
	 */
	public abstract Object initialKey();

	/**
	 * Returns the next key for iterating through the set of keys in this table.
	 *
	 * <p>Conceptually speaking, all keys in this table are totally ordered; this method
	 * returns the immediate successor of {@code key}, or {@code null} if {@code key} is
	 * the maximal key.</p>
	 *
	 * <p>When no value is associated with the key {@code key} in this table,
	 * an {@link IllegalArgumentException} is thrown.</p>
	 *
	 * <p>To retrieve the initial key for iterating through this table, use
	 * {@link #initialKey()}.</p>
	 *
	 * @param key  the key to get the immediate successor of, must not be {@code null}
	 * @return  the immediate successor of {@code key} in this table
	 *
	 * @throws IllegalArgumentException  when no value is associated with {@code key}
	 *                                   in this table, or {@code key} is {@code null}
	 */
	public abstract Object successorKey(Object key);

	/**
	 * Returns a {@link Set} view of the keys contained in this table. The set
	 * is backed by the table, so changes to the table are reflected in the set.
	 *
	 * @return  a set view of the keys contained in this table
	 */
	public abstract Set<Object> keySet();

	/**
	 * Creates a new table using the supplied {@code factory}, and copies the
	 * contents of this table to it.
	 *
	 * @param factory  the table factory to use, must not be {@code null}
	 * @return  a copy of this table
	 */
	public Table copy(TableFactory factory) {
		Table copy = factory.newTable();
		for (Object key : keySet()) {
			copy.rawset(key, rawget(key));
		}
		return copy;
	}

}
