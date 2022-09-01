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
 *
 * --
 * Portions of this file are licensed under the Lua license. For Lua
 * licensing details, please visit
 *
 *     http://www.lua.org/license.html
 *
 * Copyright (C) 1994-2016 Lua.org, PUC-Rio.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.foxgirl.rembulan.lib;

import dev.foxgirl.rembulan.*;
import dev.foxgirl.rembulan.runtime.*;

import java.util.ArrayList;
import java.util.Objects;

/**
 * <p>This library provides generic functions for table manipulation. It provides all its functions
 * inside the table {@code table}.</p>
 *
 * <p>Remember that, whenever an operation needs the length of a table, the table must be a proper
 * sequence or have a {@code __len} metamethod (see §3.4.7 of the Lua Reference Manual).
 * All functions ignore non-numeric keys in the tables given as arguments.</p>
 */
public final class TableLib {

	// Functions defined in this class are among the ugliest pieces of code ever written.
	// This is because we must assume that any operation that may suspend will -- and
	// given that most of these functions involve a loop, this means we must be able to
	// suspend and resume *in the middle* of such loops. Whenever a more complex logic
	// is involved, this becomes extremely hairy -- table.sort is a particularly juicy
	// example.

	static final LuaFunction CONCAT = new Concat();
	static final LuaFunction INSERT = new Insert();
	static final LuaFunction MOVE = new Move();
	static final LuaFunction PACK = new Pack();
	static final LuaFunction REMOVE = new Remove();
	static final LuaFunction SORT = new Sort();
	static final LuaFunction UNPACK = new Unpack();

	/**
	 * Returns the function {@code table.concat}.
	 *
	 * <p>The following is the corresponding entry from the Lua Reference Manual:</p>
	 *
	 * <blockquote>
	 * {@code table.concat (list [, sep [, i [, j]]])}
	 *
	 * <p>Given a list where all elements are strings or numbers, returns the string
	 * {@code list[i]..sep..list[i+1] ··· sep..list[j]}. The default value for {@code sep}
	 * is the empty string, the default for {@code i} is 1, and the default for {@code j}
	 * is {@code #list}. If {@code i} is greater than {@code j}, returns the empty string.</p>
	 * </blockquote>
	 *
	 * @return  the {@code table.concat} function
	 *
	 * @see <a href="http://www.lua.org/manual/5.3/manual.html#pdf-table.concat">
	 *     the Lua 5.3 Reference Manual entry for <code>table.concat</code></a>
	 */
	public static LuaFunction concat() {
		return CONCAT;
	}

	/**
	 * Returns the function {@code table.insert}.
	 *
	 * <p>The following is the corresponding entry from the Lua Reference Manual:</p>
	 *
	 * <blockquote>
	 * {@code table.insert (list, [pos,] value)}
	 *
	 * <p>Inserts element value at position {@code pos} in {@code list}, shifting up the elements
	 * {@code list[pos], list[pos+1], ···, list[#list]}. The default value for {@code pos}
	 * is {@code #list+1}, so that a call {@code table.insert(t,x)} inserts {@code x}
	 * at the end of list {@code t}.</p>
	 * </blockquote>
	 *
	 * @return  the {@code table.insert} function
	 *
	 * @see <a href="http://www.lua.org/manual/5.3/manual.html#pdf-table.insert">
	 *     the Lua 5.3 Reference Manual entry for <code>table.insert</code></a>
	 */
	public static LuaFunction insert() {
		return INSERT;
	}

	/**
	 * Returns the function {@code table.move}.
	 *
	 * <p>The following is the corresponding entry from the Lua Reference Manual:</p>
	 *
	 * <blockquote>
	 * {@code table.move (a1, f, e, t [,a2])}
	 *
	 * <p>Moves elements from table {@code a1} to table {@code a2}. This function performs
	 * the equivalent to the following multiple assignment:
	 * {@code a2[t],··· = a1[f],···,a1[e]}. The default for {@code a2} is {@code a1}.
	 * The destination range can overlap with the source range. The number of elements
	 * to be moved must fit in a Lua integer.</p>
	 * </blockquote>
	 *
	 * @return  the {@code table.move} function
	 *
	 * @see <a href="http://www.lua.org/manual/5.3/manual.html#pdf-table.move">
	 *     the Lua 5.3 Reference Manual entry for <code>table.move</code></a>
	 */
	public static LuaFunction move() {
		return MOVE;
	}

	/**
	 * Returns the function {@code table.pack}.
	 *
	 * <p>The following is the corresponding entry from the Lua Reference Manual:</p>
	 *
	 * <blockquote>
	 * {@code table.pack (···)}
	 *
	 * <p>Returns a new table with all parameters stored into keys 1, 2, etc. and with
	 * a field {@code "n"} with the total number of parameters. Note that the resulting table
	 * may not be a sequence.</p>
	 * </blockquote>
	 *
	 * @return  the {@code table.pack} function
	 *
	 * @see <a href="http://www.lua.org/manual/5.3/manual.html#pdf-table.pack">
	 *     the Lua 5.3 Reference Manual entry for <code>table.pack</code></a>
	 */
	public static LuaFunction pack() {
		return PACK;
	}

	/**
	 * Returns the function {@code table.remove}.
	 *
	 * <p>The following is the corresponding entry from the Lua Reference Manual:</p>
	 *
	 * <blockquote>
	 * {@code table.remove (list [, pos])}
	 *
	 * <p>Removes from {@code list} the element at position {@code pos}, returning the value
	 * of the removed element. When {@code pos} is an integer between 1 and {@code #list},
	 * it shifts down the elements{@code  list[pos+1], list[pos+2], ···, list[#list]}
	 * and erases element {@code list[#list]}; The index pos can also be 0 when {@code #list}
	 * is 0, or {@code #list + 1}; in those cases, the function erases the element
	 * {@code list[pos]}.</p>
	 *
	 * <p>The default value for {@code pos} is {@code #list}, so that a call
	 * {@code table.remove(l)} removes the last element of list {@code l}.</p>
	 * </blockquote>
	 *
	 * @return  the {@code table.remove} function
	 *
	 * @see <a href="http://www.lua.org/manual/5.3/manual.html#pdf-table.remove">
	 *     the Lua 5.3 Reference Manual entry for <code>table.remove</code></a>
	 */
	public static LuaFunction remove() {
		return REMOVE;
	}

	/**
	 * Returns the function {@code table.sort}.
	 *
	 * <p>The following is the corresponding entry from the Lua Reference Manual:</p>
	 *
	 * <blockquote>
	 * {@code table.sort (list [, comp])}
	 *
	 * <p>Sorts list elements in a given order, in-place, from {@code list[1]}
	 * to {@code list[#list]}. If {@code comp} is given, then it must be a function that receives
	 * two list elements and returns <b>true</b> when the first element must come before
	 * the second in the final order (so that, after the sort, {@code i < j} implies
	 * {@code not comp(list[j],list[i])}). If {@code comp} is not given, then the standard
	 * Lua operator {@code <} is used instead.</p>
	 *
	 * <p>Note that the {@code comp} function must define a strict partial order over the elements
	 * in the list; that is, it must be asymmetric and transitive. Otherwise, no valid sort may
	 * be possible.</p>
	 *
	 * <p>The sort algorithm is not stable; that is, elements not comparable by the given order
	 * (e.g., equal elements) may have their relative positions changed by the sort.</p>
	 * </blockquote>
	 *
	 * @return  the {@code table.sort} function
	 *
	 * @see <a href="http://www.lua.org/manual/5.3/manual.html#pdf-table.sort">
	 *     the Lua 5.3 Reference Manual entry for <code>table.sort</code></a>
	 */
	public static LuaFunction sort() {
		return SORT;
	}

	/**
	 * Returns the function {@code table.unpack}.
	 *
	 * <p>The following is the corresponding entry from the Lua Reference Manual:</p>
	 *
	 * <blockquote>
	 * {@code table.unpack (list [, i [, j]])}
	 *
	 * <p>Returns the elements from the given list. This function is equivalent to</p>
	 *
	 * <blockquote>
	 *  {@code return list[i], list[i+1], ···, list[j] }
	 * </blockquote>
	 *
	 * <p>By default, {@code i} is 1 and {@code j} is {@code #list}.</p>
	 * </blockquote>
	 *
	 * @return  the {@code table.unpack} function
	 *
	 * @see <a href="http://www.lua.org/manual/5.3/manual.html#pdf-table.unpack">
	 *     the Lua 5.3 Reference Manual entry for <code>table.unpack</code></a>
	 */
	public static LuaFunction unpack() {
		return UNPACK;
	}


	private TableLib() {
		// not to be instantiated
	}

	/**
	 * Installs the table library to the global environment {@code env} in the state
	 * context {@code context}.
	 *
	 * <p>If {@code env.package.loaded} is a table, adds the library table
	 * to it with the key {@code "table"}, using raw access.</p>
	 *
	 * @param context  the state context, must not be {@code null}
	 * @param env  the global environment, must not be {@code null}
	 *
	 * @throws NullPointerException  if {@code context} or {@code env} is {@code null}
	 */
	public static void installInto(StateContext context, Table env) {
		Objects.requireNonNull(context);
		Objects.requireNonNull(env);

		Table t = context.newTable();

		t.rawset("concat", concat());
		t.rawset("insert", insert());
		t.rawset("move", move());
		t.rawset("pack", pack());
		t.rawset("remove", remove());
		t.rawset("sort", sort());
		t.rawset("unpack", unpack());

		ModuleLib.install(env, "table", t);
	}

	static long getLength(ReturnBuffer rbuf) {
		Object o = rbuf.get0();
		Long l = Conversions.integerValueOf(o);
		if (l != null) {
			return l.longValue();
		}
		else {
			throw new LuaRuntimeException("object length is not an integer");
		}
	}

	static class Concat extends AbstractLibFunction {

		@Override
		protected String name() {
			return "concat";
		}

		private static class SuspendedState {

			public final int state;

			public final Table t;
			public final ArgumentIterator args;
			public final ByteString sep;
			public final long i;
			public final long j;
			public final long k;
			public final ByteStringBuilder bld;

			public SuspendedState(int state, Table t, ArgumentIterator args, ByteString sep, long i, long j, long k, ByteStringBuilder bld) {
				this.state = state;
				this.t = t;
				this.args = args;
				this.sep = sep;
				this.i = i;
				this.j = j;
				this.k = k;
				this.bld = bld;
			}

		}

		private static void appendToBuilder(ByteStringBuilder bld, long index, Object o) {
			ByteString s = Conversions.stringValueOf(o);
			if (s != null) {
				bld.append(s);
			}
			else {
				throw new LuaRuntimeException("invalid value ("
						+ PlainValueTypeNamer.INSTANCE.typeNameOf(o)+ ") at index " + index
						+ " in table for 'concat'");
			}

		}

		private static void concatUsingRawGet(ExecutionContext context, Table t, ByteString sep, long i, long j) {
			ByteStringBuilder bld = new ByteStringBuilder();
			for (long k = i; k <= j; k++) {
				Object o = t.rawget(k);
				appendToBuilder(bld, k, o);
				if (k + 1 <= j) {
					bld.append(sep);
				}
			}
			context.getReturnBuffer().setTo(bld.toByteString());
		}

		private static final int STATE_LEN_PREPARE = 0;
		private static final int STATE_LEN_RESUME = 1;
		private static final int STATE_BEFORE_LOOP = 2;
		private static final int STATE_LOOP = 3;

		private void run(ExecutionContext context, int state, Table t, ArgumentIterator args, ByteString sep, long i, long j, long k, ByteStringBuilder bld)
				throws ResolvedControlThrowable {

			try {
				switch (state) {
					// entry point for t with __len
					case STATE_LEN_PREPARE:
						state = STATE_LEN_RESUME;
						Dispatch.len(context, t);  // may suspend, will pass #obj through the stack

					// resume point #1
					case STATE_LEN_RESUME: {
						// pass length in k
						k = getLength(context.getReturnBuffer());
					}

					// entry point for t without __len; k == #t
					case STATE_BEFORE_LOOP: {
						long len = k;
						k = 0;  // clear k

						// process arguments
						sep = args.nextOptionalString(ByteString.empty());
						args.move(2);
						i = args.nextOptionalInteger(1);
						args.move(3);
						j = args.nextOptionalInteger(len);

						// don't need the args any more
						args = null;

						// i, j are known, k is 0

						// is this a clean table without __index?
						if (!TableUtil.hasIndexMetamethod(t)) {
							concatUsingRawGet(context, t, sep, i, j);
							return;
						}

						// generic case: go through Dispatch and be prepared to be suspended;
						// k is the index running from i to j (inclusive)

						if (i <= j) {
							k = i;
							state = STATE_LOOP;
							bld = new ByteStringBuilder();  // allocate the result accumulator
							Dispatch.index(context, t, k++);  // may suspend

							// fall-through to state == STATE_LOOP
						}
						else {
							// interval empty, we're done
							context.getReturnBuffer().setTo("");  // return the empty string
							return;
						}
					}

					case STATE_LOOP: {
						while (true) {
							// k now points to the *next* item to retrieve; we've just processed
							// (k - 1), need to add it to the results

							Object v = context.getReturnBuffer().get0();
							appendToBuilder(bld, k, v);

							// we may now continue
							if (k <= j) {
								// append the separator
								bld.append(sep);

								state = STATE_LOOP;
								Dispatch.index(context, t, k++);  // may suspend
							}
							else {
								break;
							}
						}

						assert (k > j);

						// we're done!
						context.getReturnBuffer().setTo(bld.toByteString());
						return;
					}

					default:
						throw new IllegalStateException("Illegal state: " + state);
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, t, args, sep, i, j, k, bld));
			}
		}

		@Override
		protected void invoke(ExecutionContext context, ArgumentIterator args) throws ResolvedControlThrowable {
			final Table t = args.nextTable();
			final int state;
			final long k;

			if (!TableUtil.hasLenMetamethod(t)) {
				// safe to use rawlen
				state = STATE_BEFORE_LOOP;
				k = t.rawlen();
			}
			else {
				// must use Dispatch
				state = STATE_LEN_PREPARE;
				k = 0;  // dummy value, will be overwritten
			}

			run(context, state, t, args, null, 0, 0, k, null);
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			SuspendedState ss = (SuspendedState) suspendedState;
			run(context, ss.state, ss.t, ss.args, ss.sep, ss.i, ss.j, ss.k, ss.bld);
		}

	}

	static class Insert extends AbstractLibFunction {

		@Override
		protected String name() {
			return "insert";
		}

		private static class SuspendedState {

			public final int state;
			public final Table t;
			public final ArgumentIterator args;
			public final long pos;
			public final long len;
			public final Object value;

			private SuspendedState(int state, Table t, ArgumentIterator args, long pos, long len, Object value) {
				this.state = state;
				this.t = t;
				this.args = args;
				this.pos = pos;
				this.len = len;
				this.value = value;
			}

		}

		private static final int PHASE_SHIFT = 3;
		private static final int _LEN  = 0;
		private static final int _LOOP = 1;
		private static final int _END  = 2;

		private static final int _LEN_PREPARE = (_LEN << PHASE_SHIFT) | 0;
		private static final int _LEN_RESUME  = (_LEN << PHASE_SHIFT) | 1;

		private static final int _LOOP_TEST   = (_LOOP << PHASE_SHIFT) | 0;
		private static final int _LOOP_TABGET = (_LOOP << PHASE_SHIFT) | 1;
		private static final int _LOOP_TABSET = (_LOOP << PHASE_SHIFT) | 2;

		private static final int _END_TABSET = (_END << PHASE_SHIFT) | 0;
		private static final int _END_RETURN = (_END << PHASE_SHIFT) | 1;

		private void checkValidPos(long pos, long len) {
			if (pos < 1 || pos > len + 1) {
				throw new BadArgumentException(2, name(), "position out of bounds");
			}
		}

		private static void rawInsert(Table t, long pos, long len, Object value) {
			for (long k = len + 1; k > pos; k--) {
				t.rawset(k, t.rawget(k - 1));
			}
			t.rawset(pos, value);
		}

		@Override
		protected void invoke(ExecutionContext context, ArgumentIterator args) throws ResolvedControlThrowable {
			Table t = args.nextTable();

			if (!TableUtil.hasLenMetamethod(t)) {
				run_args(context, t, args, t.rawlen());
			}
			else {
				_len(context, _LEN_PREPARE, t, args);
			}
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			SuspendedState ss = (SuspendedState) suspendedState;
			switch (ss.state >> PHASE_SHIFT) {
				case _LEN:  _len(context, ss.state, ss.t, ss.args); break;
				case _LOOP: _loop(context, ss.state, ss.t, ss.pos, ss.len, ss.value); break;
				case _END:  _end(context, ss.state, ss.t, ss.pos, ss.value); break;
				default: throw new IllegalStateException("Illegal state: " + ss.state);
			}
		}

		private void _len(ExecutionContext context, int state, Table t, ArgumentIterator args)
				throws ResolvedControlThrowable {

			// __len is defined, must go through Dispatch

			final long len;

			try {
				switch (state) {
					case _LEN_PREPARE:
						state = _LEN_RESUME;
						Dispatch.len(context, t);

					case _LEN_RESUME: {
						len = getLength(context.getReturnBuffer());
						break;
					}

					default:
						throw new IllegalStateException("Illegal state: " + state);
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, t, args, 0, 0, null));
			}

			// next: process the arguments
			run_args(context, t, args, len);
		}

		private void run_args(ExecutionContext context, Table t, ArgumentIterator args, long len)
				throws ResolvedControlThrowable {

			final long pos;
			final Object value;

			if (args.size() == 2) {
				// implicit pos (#t + 1)
				pos = len + 1;
				value = args.nextAny();
			}
			else if (args.size() == 3) {
				// explicit pos
				pos = args.nextInteger();
				checkValidPos(pos, len);
				value = args.nextAny();
			}
			else {
				throw new LuaRuntimeException("wrong number of arguments to 'insert'");
			}

			// next: start the loop
			start_loop(context, t, pos, len, value);
		}

		private void start_loop(ExecutionContext context, Table t, long pos, long len, Object value)
				throws ResolvedControlThrowable {

			// check whether we can use raw accesses instead of having to go through
			// Dispatch and potential metamethods

			if (!TableUtil.hasIndexMetamethod(t) && !TableUtil.hasNewIndexMetamethod(t)) {
				// raw case
				rawInsert(t, pos, len, value);
				context.getReturnBuffer().setTo();
			}
			else {
				// generic (Dispatch'd) case
				// initialise k = len + 1 (will be decremented in the next TEST, so add +1 here)
				_loop(context, _LOOP_TEST, t, pos, len + 2, value);
			}
		}

		private void _loop(ExecutionContext context, int state, Table t, long pos, long k, Object value)
				throws ResolvedControlThrowable {

			// came from start_loop in the invoke path

			try {
				loop: while (true) {
					switch (state) {
						case _LOOP_TEST:
							k -= 1;
							state = _LOOP_TABGET;
							if (k <= pos) {
								break loop;  // end the loop
							}

						case _LOOP_TABGET:
							state = _LOOP_TABSET;
							Dispatch.index(context, t, k - 1);

						case _LOOP_TABSET:
							state = _LOOP_TEST;
							Object v = context.getReturnBuffer().get0();
							Dispatch.setindex(context, t, k, v);
							break;  // go to next iteration

						default:
							throw new IllegalStateException("Illegal state: " + state);

					}
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, t, null, pos, k, value));
			}

			// continue into the last stage
			state = _END_TABSET;
			_end(context, state, t, pos, value);
		}

		private void _end(ExecutionContext context, int state, Table t, long pos, Object value)
				throws ResolvedControlThrowable {
			try {
				switch (state) {

					case _END_TABSET:
						state = _END_RETURN;
						Dispatch.setindex(context, t, pos, value);

					case _END_RETURN:
						break;

					default:
						throw new IllegalStateException("Illegal state: " + state);
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, t, null, pos, -1, value));
			}

			// finished!
			context.getReturnBuffer().setTo();
		}

	}

	static class Move extends AbstractLibFunction {

		@Override
		protected String name() {
			return "move";
		}

		private static class SuspendedState {

			public final int state;
			public final Table a1;
			public final Table a2;
			public final long f;
			public final long t;
			public final long idx;
			public final long num;
			public final boolean asc;

			private SuspendedState(int state, Table a1, Table a2, long f, long t, long idx, long num, boolean asc) {
				this.state = state;
				this.a1 = a1;
				this.a2 = a2;
				this.f = f;
				this.t = t;
				this.idx = idx;
				this.num = num;
				this.asc = asc;
			}

		}

		private void checkValidArgs(long f, long e, long t) {
			if (f <= e) {
				long num = e - f + 1;
				if (num < 1) {
					// overflow
					throw new BadArgumentException(3, name(), "too many elements to move");
				}
				if (t + (num - 1) < t) {
					throw new BadArgumentException(4, name(), "destination wrap around");
				}
			}
		}

		@Override
		protected void invoke(ExecutionContext context, ArgumentIterator args) throws ResolvedControlThrowable {
			if (args.size() < 2) {
				throw new BadArgumentException(2, name(), "number expected, got no value");
			}
			args.move(1);
			long f = args.nextInteger();
			long e = args.nextInteger();
			long t = args.nextInteger();

			final Table dest = args.nextOptionalTable(null);

			args.move(0);
			final Table a1 = args.nextTable();
			final Table a2 = dest != null ? dest : a1;

			checkValidArgs(f, e, t);

			if (f <= e) {
				long num = e - f + 1;
				assert (num > 0);

				boolean overlap = a1 == a2 && (f < t && t <= e);

				if (!TableUtil.hasIndexMetamethod(a1) && !TableUtil.hasNewIndexMetamethod(a2)) {
					// raw case
					if (overlap) {
						// same destination, range overlap
						for (long idx = num - 1; idx >= 0; idx--) {
							a2.rawset(t + idx, a1.rawget(f + idx));
						}
					}
					else {
						// different destination, or no range overlap
						for (long idx = 0; idx < num; idx++) {
							a2.rawset(t + idx, a1.rawget(f + idx));
						}
					}

					// done
					context.getReturnBuffer().setTo(a2);
				}
				else {
					long idx = overlap ? num - 1 : 0;
					_run(context, 0, a1, a2, f, t, idx, num, !overlap);
				}
			}
			else {
				// no work: done
				context.getReturnBuffer().setTo(a2);
			}
		}

		private void _run(ExecutionContext context, int state, Table a1, Table a2, long f, long t, long idx, long num, boolean asc)
				throws ResolvedControlThrowable {

			try {
				while (true) {
					switch (state) {
						case 0: {
							boolean done = asc ? idx >= num : idx < 0;
							if (done) {
								context.getReturnBuffer().setTo(a2);
								return;
							}
						}
						case 1:
							state = 2;
							Dispatch.index(context, a1, f + idx);
						case 2:
							Object v = context.getReturnBuffer().get0();
							state = 3;
							Dispatch.setindex(context, a2, t + idx, v);
						case 3:
							idx += (asc ? 1 : -1);
							state = 0;
							break;
						default:
							throw new IllegalStateException("Illegal state: " + state);
					}
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, a1, a2, f, t, idx, num, asc));
			}
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			SuspendedState ss = (SuspendedState) suspendedState;
			_run(context, ss.state, ss.a1, ss.a2, ss.f, ss.t, ss.idx, ss.num, ss.asc);
		}

	}

	static class Pack extends AbstractLibFunction {

		@Override
		protected String name() {
			return "pack";
		}

		@Override
		protected void invoke(ExecutionContext context, ArgumentIterator args) throws ResolvedControlThrowable {
			Table table = context.newTable();

			int n = 0;
			while (args.hasNext()) {
				table.rawset(n + 1, args.nextAny());
				n += 1;
			}
			table.rawset("n", Long.valueOf(n));

			context.getReturnBuffer().setTo(table);
		}

	}

	static class Remove extends AbstractLibFunction {

		@Override
		protected String name() {
			return "remove";
		}

		private static class SuspendedState {

			public final int state;
			public final Table t;
			public final ArgumentIterator args;
			public final long pos;
			public final long len;
			public final Object result;

			private SuspendedState(int state, Table t, ArgumentIterator args, long pos, long len, Object result) {
				this.state = state;
				this.t = t;
				this.args = args;
				this.pos = pos;
				this.len = len;
				this.result = result;
			}

		}

		private static final int PHASE_SHIFT = 4;
		private static final int _LEN  = 0;
		private static final int _GET  = 1;
		private static final int _LOOP = 2;
		private static final int _ERASE  = 4;

		private static final int _LEN_PREPARE = (_LEN << PHASE_SHIFT) | 0;
		private static final int _LEN_RESUME  = (_LEN << PHASE_SHIFT) | 1;

		private static final int _GET_PREPARE = (_GET << PHASE_SHIFT) | 0;
		private static final int _GET_RESUME  = (_GET << PHASE_SHIFT) | 1;

		private static final int _LOOP_TEST   = (_LOOP << PHASE_SHIFT) | 0;
		private static final int _LOOP_TABGET = (_LOOP << PHASE_SHIFT) | 1;
		private static final int _LOOP_TABSET = (_LOOP << PHASE_SHIFT) | 2;

		private static final int _ERASE_PREPARE = (_ERASE << PHASE_SHIFT) | 0;
		private static final int _ERASE_RESUME  = (_ERASE << PHASE_SHIFT) | 1;

		private void checkValidPos(long pos, long len) {
			if (!((len == 0 && pos == 0) || pos == len)  // the no-shift case
					&& (pos < 1 || pos > len + 1)) {
				throw new BadArgumentException(2, name(), "position out of bounds");
			}
		}

		private static Object rawRemove(Table t, long pos, long len) {
			Object result = t.rawget(pos);

			if (pos == 0 || pos == len || pos == len + 1) {
				// erase t[pos]
				t.rawset(pos, null);
			}
			else {
				// shift down t[pos+1],...,t[len]; erase t[len]
				for (long k = pos + 1; k <= len; k++) {
					t.rawset(k - 1, t.rawget(k));
				}
				t.rawset(len, null);
			}

			return result;
		}

		@Override
		protected void invoke(ExecutionContext context, ArgumentIterator args) throws ResolvedControlThrowable {
			Table t = args.nextTable();

			if (!TableUtil.hasLenMetamethod(t)) {
				run_args(context, t, args, t.rawlen());
			}
			else {
				_len(context, _LEN_PREPARE, t, args);
			}
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			SuspendedState ss = (SuspendedState) suspendedState;
			switch (ss.state >> PHASE_SHIFT) {
				case _LEN:   _len(context, ss.state, ss.t, ss.args); break;
				case _GET:   _get_result(context, ss.state, ss.t, ss.pos, ss.len); break;
				case _LOOP:  _loop(context, ss.state, ss.t, ss.pos, ss.len, ss.result); break;
				case _ERASE: _erase(context, ss.state, ss.t, 0, ss.result); break;  // index not needed any more when resuming
				default: throw new IllegalStateException("Illegal state: " + ss.state);
			}
		}

		private void _len(ExecutionContext context, int state, Table t, ArgumentIterator args)
				throws ResolvedControlThrowable {

			// __len is defined, must go through Dispatch

			final long len;

			try {
				switch (state) {
					case _LEN_PREPARE:
						state = _LEN_RESUME;
						Dispatch.len(context, t);

					case _LEN_RESUME: {
						len = getLength(context.getReturnBuffer());
						break;
					}

					default:
						throw new IllegalStateException("Illegal state: " + state);
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, t, args, 0, 0, null));
			}

			// next: process the arguments
			run_args(context, t, args, len);
		}

		private void run_args(ExecutionContext context, Table t, ArgumentIterator args, long len)
				throws ResolvedControlThrowable {

			final long pos;

			if (args.hasNext() && args.peek() != null) {
				// explicit pos
				pos = args.nextInteger();
				checkValidPos(pos, len);
			}
			else {
				// implicit pos (#t)
				pos = len;
			}

			// next: start the loop
			start_loop(context, t, pos, len);
		}

		private void start_loop(ExecutionContext context, Table t, long pos, long len)
				throws ResolvedControlThrowable {

			// check whether we can use raw accesses instead of having to go through
			// Dispatch and potential metamethods

			if (!TableUtil.hasIndexMetamethod(t) && !TableUtil.hasNewIndexMetamethod(t)) {
				// raw case
				Object result = rawRemove(t, pos, len);
				context.getReturnBuffer().setTo(result);
			}
			else {
				// generic (Dispatch'd) case
				// initialise k = len + 1 (will be decremented in the next TEST, so add +1 here)
				_get_result(context, _GET_PREPARE, t, pos, len);
			}
		}

		private void _get_result(ExecutionContext context, int state, Table t, long pos, long len)
				throws ResolvedControlThrowable {

			final Object result;
			try {
				switch (state) {
					case _GET_PREPARE:
						state = _GET_RESUME;
						Dispatch.index(context, t, pos);

					case _GET_RESUME:
						result = context.getReturnBuffer().get0();
					    break;

					default:
						throw new IllegalStateException("Illegal state: " + state);
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, t, null, pos, len, null));
			}


			if (pos == 0 || pos == len || pos == len + 1) {
				// erase t[pos]
				_erase(context, _ERASE_PREPARE, t, pos, result);
			}
			else {
				// pos now used for iteration; want (pos + 1), but it will be incremented before the test
				_loop(context, _LOOP_TEST, t, pos, len, result);
			}
		}

		private void _loop(ExecutionContext context, int state, Table t, long k, long len, Object result)
				throws ResolvedControlThrowable {

			// came from start_loop in the invoke path

			try {
				loop: while (true) {
					switch (state) {
						case _LOOP_TEST:
							k += 1;
							state = _LOOP_TABGET;
							if (k > len) {
								break loop;  // end the loop
							}

						case _LOOP_TABGET:
							state = _LOOP_TABSET;
							Dispatch.index(context, t, k);

						case _LOOP_TABSET:
							state = _LOOP_TEST;
							Object v = context.getReturnBuffer().get0();
							Dispatch.setindex(context, t, k - 1, v);
							break;  // go to next iteration

						default:
							throw new IllegalStateException("Illegal state: " + state);

					}
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, t, null, k, len, result));
			}

			// erase the last element, return the result
			_erase(context, _ERASE_PREPARE, t, len, result);
		}

		private void _erase(ExecutionContext context, int state, Table t, long idx, Object result)
				throws ResolvedControlThrowable {

			try {
				switch (state) {
					case _ERASE_PREPARE:
						state = _ERASE_RESUME;
						Dispatch.setindex(context, t, idx, null);

					case _ERASE_RESUME:
						// we're done
						break;

					default:
						throw new IllegalStateException("Illegal state: " + state);
				}
			}
			catch (UnresolvedControlThrowable ct) {
				// no need to carry any information but the result around
				throw ct.resolve(this, new SuspendedState(state, null, null, 0, 0, result));
			}

			// finished, set the result
			context.getReturnBuffer().setTo(result);
		}

	}

	static class Sort extends AbstractLibFunction {

		// implemented using heapsort

		@Override
		protected String name() {
			return "sort";
		}

		// States:
		// +-------------------------+----------------------------+---------------------+
		// |     fetching length     |         heapifying         |       sorting       |
		// +-------------------------+----------------------------+---------------------+
		// ^                         ^                            ^
		// +- STATE_OFFSET_LEN       +- STATE_OFFSET_HEAPIFY      +- STATE_OFFSET_SORT

		private static final int STATE_OFFSET_LEN = 0;
		private static final int STATE_OFFSET_HEAPIFY = STATE_OFFSET_LEN + 2;
		private static final int STATE_OFFSET_SORT = STATE_OFFSET_HEAPIFY + 2;

		private static class SuspendedState {

			// whenever siftState is >= 0, there is an unfinished siftDown operation;
			// once completed, state is to be resumed.

			public final int state;
			public final int siftState;

			public final ArgumentIterator args;
			public final Table t;
			public final LuaFunction comp;

			public final long i;
			public final long len;
			public final long j;

			public final Object o1;
			public final Object o2;
			public final Object o3;

			private SuspendedState(int state, int siftState, ArgumentIterator args, Table t, LuaFunction comp,
								   long i, long len, long j, Object o1, Object o2, Object o3) {
				this.state = state;
				this.siftState = siftState;
				this.t = t;
				this.args = args;
				this.comp = comp;
				this.i = i;
				this.len = len;
				this.j = j;

				this.o1 = o1;
				this.o2 = o2;
				this.o3 = o3;
			}

		}

		@Override
		protected void invoke(ExecutionContext context, ArgumentIterator args) throws ResolvedControlThrowable {
			Table t = args.nextTable();

			if (!TableUtil.hasLenMetamethod(t)) {
				long len = t.rawlen();
				prepareLoop(context, args, t, len);
			}
			else {
				fetchLen(context, STATE_OFFSET_LEN, args, t);
			}

		}

		private void fetchLen(ExecutionContext context, int state, ArgumentIterator args, Table t)
				throws ResolvedControlThrowable {

			long len = 0;
			try {
				switch (state) {
					case STATE_OFFSET_LEN:
						state = STATE_OFFSET_LEN + 1;
						Dispatch.len(context, t);
					case STATE_OFFSET_LEN + 1:
						len = getLength(context.getReturnBuffer());
						break;
					default:
						throw new IllegalStateException("Illegal state: " + state);
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, -1, args, t, null, 1, -1, -1, null, null, null));
			}

			prepareLoop(context, args, t, len);
		}

		private void prepareLoop(ExecutionContext context, ArgumentIterator args, Table t, long len)
			throws ResolvedControlThrowable {

			if (len >= Integer.MAX_VALUE) {
				throw new BadArgumentException(1, name(), "array too big");
			}

			if (len < 2) {
				// nothing to sort
				return;
			}
			else {
				LuaFunction comp = args.nextOptionalFunction(null);

				// can we sort it using a raw ordering?
				Ordering<Object> rawOrdering = comp == null
						&& !TableUtil.hasIndexMetamethod(t)
						&& !TableUtil.hasNewIndexMetamethod(t)
						? TableUtil.rawSequenceOrderingOf(t, 1, len) : null;

				if (rawOrdering != null) {
					rawSort(t, rawOrdering, len);
				}
				else {
					go(context, t, comp, len);
				}
			}

		}

		private static void rawSort(Table t, Ordering<Object> ordering, long len) {
			assert (len > 1);

			rawHeapify(t, ordering, len);

			long end = len;
			while (end > 1) {
				Object beginValue = t.rawget(1);
				Object endValue = t.rawget(end);

				// swap
				t.rawset(end, beginValue);
				t.rawset(1, endValue);

				end -= 1;
				rawSiftDown(t, ordering, 1, end);
			}
		}

		private static void rawHeapify(Table t, Ordering<Object> ordering, long count) {
			for (long start = count / 2; start >= 1; start--) {
				rawSiftDown(t, ordering, start, count);
			}
		}

		private void go(ExecutionContext context, Table t, LuaFunction comp, long len)
				throws ResolvedControlThrowable {

			heapify(context, STATE_OFFSET_HEAPIFY, t, comp, len / 2, len);
			sort(context, STATE_OFFSET_SORT, t, comp, len, null, null);
		}

		private void heapify(ExecutionContext context, int state, Table t, LuaFunction comp, long start, long count)
				throws ResolvedControlThrowable {

			loop:
			while (true) {
				switch (state) {
					case STATE_OFFSET_HEAPIFY:
						if (start < 1) {
							// done: [1..end] is a max-heap
							break loop;
						}
						state = STATE_OFFSET_HEAPIFY + 1;
						doSiftDown(context, state, t, comp, start, count);

					case STATE_OFFSET_HEAPIFY + 1:
						state = STATE_OFFSET_HEAPIFY;
						start -= 1;
						break;

					default: throw new IllegalStateException("Illegal state: " + state);
				}
			}
		}

		private void sort(ExecutionContext context, int state, Table t, LuaFunction comp, long end,
						  Object beginValue, Object endValue)
				throws ResolvedControlThrowable {

			while (true) {
				try {
					switch (state) {
						case STATE_OFFSET_SORT:
							if (end <= 1) {
								// we're done
								context.getReturnBuffer().setTo();
								return;
							}
							state = STATE_OFFSET_SORT + 1;
							Dispatch.index(context, t, 1);

						case STATE_OFFSET_SORT + 1:
							beginValue = context.getReturnBuffer().get0();
							state = STATE_OFFSET_SORT + 2;
							Dispatch.index(context, t, end);

						case STATE_OFFSET_SORT + 2:
							endValue = context.getReturnBuffer().get0();

							// Verify order:
							// The value at index 1 (i.e., beginValue) must be greater-than
							// or equal-to the value at index `end` (i.e., endValue), since
							// [1..end] is a max-heap. In other words, we're expecting that
							//   (beginValue >= endValue),
							// i.e.,
							//   not (beginValue < endValue)

							state = STATE_OFFSET_SORT + 3;
							_lt(context, comp, beginValue, endValue);

						case STATE_OFFSET_SORT + 3:
							if (Conversions.booleanValueOf(context.getReturnBuffer().get0())) {
								// illegal order: beginValue < endValue
								throw new IllegalStateException("invalid order function for sorting");
							}

							// next: swap begin and end values in the table
							state = STATE_OFFSET_SORT + 4;
							Dispatch.setindex(context, t, end, beginValue);

						case STATE_OFFSET_SORT + 4:
							state = STATE_OFFSET_SORT + 5;
							Dispatch.setindex(context, t, 1, endValue);

						case STATE_OFFSET_SORT + 5:
							// we don't need beginValue or endValue any longer
							end -= 1;
							state = STATE_OFFSET_SORT;
							doSiftDown(context, state, t, comp, 1, end);
							break;

						default: throw new IllegalStateException("Illegal state: " + state);
					}
				}
				catch (UnresolvedControlThrowable ct) {
					throw ct.resolve(this, new SuspendedState(state, -1, null, t, comp, -1, end, -1, beginValue, endValue, null));
				}
			}
		}

		private void _lt(ExecutionContext context, LuaFunction comp, Object a, Object b)
				throws UnresolvedControlThrowable {

			if (comp != null) {
				Dispatch.call(context, comp, a, b);
			}
			else {
				Dispatch.lt(context, a, b);
			}

		}

		private void doSiftDown(ExecutionContext context, int state, Table t, LuaFunction comp, long start, long end)
				throws ResolvedControlThrowable {
			siftDown(context, state, 0, t, comp, start, end, 0, null, null, null);
		}

		private void siftDown(ExecutionContext context, final int state, int siftState, Table t, LuaFunction comp,
							  long root, long end, long child, Object rootValue, Object childValue, Object tmp)
				throws ResolvedControlThrowable {

			try {
				while (true) {
					switch (siftState) {

						case 0:
							if (root * 2 > end) {
								// we're done
								context.getReturnBuffer().setTo();
								return;
							}
							child = root * 2;
							siftState = 1;
							Dispatch.index(context, t, root);

						case 1:
							rootValue = context.getReturnBuffer().get0();
							siftState = 2;
							Dispatch.index(context, t, child);

						case 2:
							childValue = context.getReturnBuffer().get0();

							if (child + 1 > end) {
								// skip the next steps
								siftState = 5;
								break;
							}
							else {
								siftState = 3;
								Dispatch.index(context, t, child + 1);
							}

						case 3:
							tmp = context.getReturnBuffer().get0();
							siftState = 4;
							_lt(context, comp, childValue, tmp);

						case 4:
							if (Conversions.booleanValueOf(context.getReturnBuffer().get0())) {
								// use the right child
								child += 1;
								childValue = tmp;
								tmp = null;
							}

						case 5:
							// childValue and child is up-to-date
							siftState = 6;
							_lt(context, comp, rootValue, childValue);

						case 6:
							if (!Conversions.booleanValueOf(context.getReturnBuffer().get0())) {
								// heap property satisfied -> done
								return;
							}

							// else: swap

							siftState = 7;
							Dispatch.setindex(context, t, root, childValue);

						case 7:
							siftState = 8;
							Dispatch.setindex(context, t, child, rootValue);

						case 8:
							root = child;
							siftState = 0;
							break;  // continue with the loop

						default:
							throw new IllegalStateException("Illegal state: " + state);
					}
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, siftState, null, t, comp, root, end, child, rootValue, childValue, tmp));
			}

		}

		private static void swap(Table t, long a, long b, Object va, Object vb) {
			t.rawset(a, vb);
			t.rawset(b, va);
		}

		private static void rawSiftDown(Table t, Ordering<Object> ordering, long start, long end) {
			long root = start;

			while (root * 2 <= end) {
				long child = root * 2;

				final Object rootValue = t.rawget(root);
				Object childValue = t.rawget(child);
				if (child + 1 <= end) {
					Object tmp = t.rawget(child + 1);
					if (ordering.lt(childValue, tmp)) {
						// use the right child
						child += 1;
						childValue = tmp;
					}
				}

				if (ordering.lt(rootValue, childValue)) {
					swap(t, root, child, rootValue, childValue);
					root = child;
				}
				else {
					return;
				}
			}

		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			SuspendedState ss = (SuspendedState) suspendedState;
			if (ss.state < STATE_OFFSET_HEAPIFY) {
				fetchLen(context, ss.state, ss.args, ss.t);
			}
			else {
				if (ss.siftState >= 0) {
					// resume a siftDown
					siftDown(context, ss.state, ss.siftState, ss.t, ss.comp, ss.i, ss.len, ss.j, ss.o1, ss.o2, ss.o3);
				}

				if (ss.state < STATE_OFFSET_SORT) {
					heapify(context, ss.state, ss.t, ss.comp, ss.i, ss.len);
					sort(context, STATE_OFFSET_SORT, ss.t, ss.comp, ss.len, null, null);
				}
				else {
					sort(context, ss.state, ss.t, ss.comp, ss.len, ss.o1, ss.o2);
				}

			}
		}

	}

	static class Unpack extends AbstractLibFunction {

		@Override
		protected String name() {
			return "unpack";
		}

		private static class SuspendedState {

			public final int state;

			public final Object obj;
			public final long i;
			public final long j;
			public final long k;
			public final ArrayList<Object> result;

			public SuspendedState(int state, Object obj, long i, long j, long k, ArrayList<Object> result) {
				this.state = state;
				this.obj = obj;
				this.i = i;
				this.j = j;
				this.k = k;
				this.result = result;
			}

		}

		/**
		 * The maximum number of values that may be unpacked.
		 */
		public static final long MAX_RESULTS = 1000000;

		private static void verifyNumberOfResults(long i, long j) {
			if (i < j) {
				long n = j - i;
				if (n < 0 || n > MAX_RESULTS) {
					throw new IllegalArgumentException("too many results to unpack");
				}
			}
		}

		private static void unpackUsingRawGet(ExecutionContext context, Table t, long i, long j) {
			ArrayList<Object> r = new ArrayList<>();

			// protect against overflows when j == Long.MAX_VALUE
			for (long k = i; (i <= k) && (k <= j); k++) {
				r.add(t.rawget(k));
			}
			context.getReturnBuffer().setToContentsOf(r);
		}

		private static final int STATE_LEN_PREPARE = 0;
		private static final int STATE_LEN_RESUME = 1;
		private static final int STATE_BEFORE_LOOP = 2;
		private static final int STATE_LOOP = 3;

		private void run(ExecutionContext context, int state, Object obj, long i, long j, long k, ArrayList<Object> result)
				throws ResolvedControlThrowable {

			try {
				switch (state) {

					case STATE_LEN_PREPARE:
						state = STATE_LEN_RESUME;
						Dispatch.len(context, obj);  // may suspend, will pass #obj through the stack

					case STATE_LEN_RESUME: {
						j = getLength(context.getReturnBuffer());
					}

					case STATE_BEFORE_LOOP:
						// j is known;

						verifyNumberOfResults(i, j);

						// is this a clean table without __index?
						if (obj instanceof Table && !TableUtil.hasIndexMetamethod((Table) obj)) {
							unpackUsingRawGet(context, (Table) obj, i, j);
							return;
						}

						// generic case: go through Dispatch and be prepared to be suspended;
						// k is the index running from i to j (inclusive)

						if (i <= j) {
							k = i;
							state = STATE_LOOP;
							result = new ArrayList<>();  // allocate the result accumulator
							Dispatch.index(context, obj, k++);  // may suspend

							// fall-through to state == STATE_LOOP
						}
						else {
							// interval empty, we're done
							context.getReturnBuffer().setTo();
							return;
						}

					case STATE_LOOP: {
						while (true) {
							// k now points to the *next* item to retrieve; we've just processed
							// (k - 1), need to add it to the results

							Object v = context.getReturnBuffer().get0();
							result.add(v);

							// we may now continue
							// protect against overflows when j == Long.MAX_VALUE
							if ((i <= k) && (k <= j)) {
								state = STATE_LOOP;
								Dispatch.index(context, obj, k++);  // may suspend
							}
							else {
								break;
							}
						}

						assert (k > j || k == Long.MIN_VALUE);

						// we're done!
						context.getReturnBuffer().setToContentsOf(result);
						return;
					}

					default:
						throw new IllegalStateException("Illegal state: " + state);
				}
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, new SuspendedState(state, obj, i, j, k, result));
			}
		}

		@Override
		protected void invoke(ExecutionContext context, ArgumentIterator args) throws ResolvedControlThrowable {
			Object obj = args.hasNext() ? args.peek() : null;
			args.skip();
			long i = args.nextOptionalInteger(1);

			final int state;
			final long j;

			if (args.hasNext() && args.peek() != null) {
				j = args.nextInteger();
				state = STATE_BEFORE_LOOP;
			}
			else if (obj instanceof Table && !TableUtil.hasLenMetamethod((Table) obj)) {
				j = ((Table) obj).rawlen();  // safe to use rawlen
				state = STATE_BEFORE_LOOP;
			}
			else {
				j = 0;  // placeholder, will be retrieved in the run() method
				state = STATE_LEN_PREPARE;
			}

			run(context, state, obj, i, j, 0, null);
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			SuspendedState ss = (SuspendedState) suspendedState;
			run(context, ss.state, ss.obj, ss.i, ss.j, ss.k, ss.result);
		}

	}


}
