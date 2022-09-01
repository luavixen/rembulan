/*
 * Copyright 2016 Miroslav Janíček
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

import dev.foxgirl.rembulan.runtime.ReturnBuffer;

import java.util.Collection;
import java.util.Iterator;

/**
 * A return buffer implementation that stores the first two values in private fields,
 * and the remaining values in a re-sizable array.
 */
class PairCachingReturnBuffer implements ReturnBuffer {

	private static final Object[] EMPTY_ARRAY = new Object[0];

	public static final int MIN_BUF_SIZE = 3;

	// by default, handle up to 10 values without reallocating
	private static final int DEFAULT_PREFERRED_BUF_SIZE = 8;

	// size to trim down to as soon as possible
	private final int preferredBufSize;

	private int size;
	private Object _0;
	private Object _1;
	private Object[] _buf;

	private Object tailCallTarget;
	private boolean tailCall;

	public PairCachingReturnBuffer(int preferredBufSize) {
		if (preferredBufSize < MIN_BUF_SIZE) {
			throw new IllegalArgumentException("Preferred array size must be at least " + MIN_BUF_SIZE);
		}

		this.preferredBufSize = preferredBufSize;

		this._0 = null;
		this._1 = null;
		this._buf = new Object[preferredBufSize];
		this.size = 0;

		this.tailCallTarget = null;
		this.tailCall = false;
	}

	public PairCachingReturnBuffer() {
		this(DEFAULT_PREFERRED_BUF_SIZE);
	}

	@Override
	public boolean isCall() {
		return tailCall;
	}

	@Override
	public Object getCallTarget() {
		if (tailCall) {
			return tailCallTarget;
		}
		else {
			throw new IllegalStateException("Not a tail call");
		}
	}

	protected void unsetTailCall() {
		tailCall = false;
		tailCallTarget = null;
	}

	protected void _setTailCall(Object target) {
		tailCall = true;
		tailCallTarget = target;
	}

	@Override
	public int size() {
		return size;
	}

	private void ensureBufSizeAtLeast(int sizeAtLeast) {
		int sz = sizeAtLeast > preferredBufSize ? sizeAtLeast : preferredBufSize;

		if (sz != _buf.length) {
			// resize: initialised to nulls, we're done
			_buf = new Object[sz];
		}
		else {
			// new size still fits, null everything between oldSize and newSize
			int oldSize = size - 2;
			for (int i = sizeAtLeast; i < oldSize; i++) {
				_buf[i] = null;
			}
		}
	}

	private void _set(Object a, Object b, int bufSize, int size) {
		_0 = a;
		_1 = b;
		ensureBufSizeAtLeast(bufSize);
		this.size = size;
	}

	private void _setArray(Object[] a) {
		int sz = a.length;

		int asz = sz - 2;
		if (asz > 0) {
			// copy contents to buffer
			ensureBufSizeAtLeast(asz);
			System.arraycopy(a, 2, _buf, 0, asz);
		}
		else {
			// just clear the buffer
			ensureBufSizeAtLeast(0);
		}

		Object o0 = null, o1 = null;
		switch (sz) {
			default:
			case 2: o1 = a[1];
			case 1: o0 = a[0];
			case 0:
		}
		_0 = o0;
		_1 = o1;

		size = sz;
	}

	private void _setCollection(Collection<?> collection) {
		int sz = collection.size();

		// make sure the buffer is big enough
		ensureBufSizeAtLeast(Math.max(0, sz - 2));

		Iterator<?> it = collection.iterator();

		_0 = it.hasNext() ? it.next() : null;
		_1 = it.hasNext() ? it.next() : null;

		// copy contents into the buffer
		for (int i = 0; i < sz - 2; i++) {
			_buf[i] = it.next();
		}

		size = sz;
	}

	@Override
	public void setTo() {
		unsetTailCall();
		_set(null, null, 0, 0);
	}

	@Override
	public void setTo(Object a) {
		unsetTailCall();
		_set(a, null, 0, 1);
	}

	@Override
	public void setTo(Object a, Object b) {
		unsetTailCall();
		_set(a, b, 0, 2);
	}

	@Override
	public void setTo(Object a, Object b, Object c) {
		unsetTailCall();
		_set(a, b, 1, 3);
		_buf[0] = c;
	}

	@Override
	public void setTo(Object a, Object b, Object c, Object d) {
		unsetTailCall();
		_set(a, b, 2, 4);
		_buf[0] = c;
		_buf[1] = d;
	}

	@Override
	public void setTo(Object a, Object b, Object c, Object d, Object e) {
		unsetTailCall();
		_set(a, b, 3, 5);
		_buf[0] = c;
		_buf[1] = d;
		_buf[2] = e;
	}

	@Override
	public void setToContentsOf(Object[] a) {
		unsetTailCall();
		_setArray(a);
	}

	@Override
	public void setToContentsOf(Collection<?> collection) {
		unsetTailCall();
		_setCollection(collection);
	}

	@Override
	public void setToCall(Object target) {
		_setTailCall(target);
		_set(null, null, 0, 0);
	}

	@Override
	public void setToCall(Object target, Object arg1) {
		_setTailCall(target);
		_set(arg1, null, 0, 1);
	}

	@Override
	public void setToCall(Object target, Object arg1, Object arg2) {
		_setTailCall(target);
		_set(arg1, arg2, 0, 2);
	}

	@Override
	public void setToCall(Object target, Object arg1, Object arg2, Object arg3) {
		_setTailCall(target);
		_set(arg1, arg2, 1, 3);
		_buf[0] = arg3;
	}

	@Override
	public void setToCall(Object target, Object arg1, Object arg2, Object arg3, Object arg4) {
		_setTailCall(target);
		_set(arg1, arg2, 2, 4);
		_buf[0] = arg3;
		_buf[1] = arg4;
	}

	@Override
	public void setToCall(Object target, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		_setTailCall(target);
		_set(arg1, arg2, 3, 5);
		_buf[0] = arg3;
		_buf[1] = arg4;
		_buf[2] = arg5;
	}

	@Override
	public void setToCallWithContentsOf(Object target, Object[] args) {
		_setTailCall(target);
		_setArray(args);
	}

	@Override
	public void setToCallWithContentsOf(Object target, Collection<?> args) {
		_setTailCall(target);
		_setCollection(args);
	}

	@Override
	public Object[] getAsArray() {
		switch (size) {
			case 0: return EMPTY_ARRAY;
			case 1: return new Object[] { _0 };
			case 2: return new Object[] { _0, _1 };
			default:
				Object[] result = new Object[size];
				result[0] = _0;
				result[1] = _1;
				System.arraycopy(_buf, 0, result, 2, size - 2);
				return result;
		}
	}

	@Override
	public Object get(int idx) {
		if (idx < 0) {
			return null;
		}
		else {
			switch (idx) {
				case 0:  return _0;
				case 1:  return _1;
				default: return idx < size ? _buf[idx - 2] : null;
			}
		}
	}

	@Override
	public Object get0() {
		return _0;
	}

	@Override
	public Object get1() {
		return _1;
	}

	@Override
	public Object get2() {
		// assuming buf is always big enough
		return _buf[0];
	}

	@Override
	public Object get3() {
		// assuming buf is always big enough
		return _buf[1];
	}

	@Override
	public Object get4() {
		// assuming buf is always big enough
		return _buf[2];
	}

}
