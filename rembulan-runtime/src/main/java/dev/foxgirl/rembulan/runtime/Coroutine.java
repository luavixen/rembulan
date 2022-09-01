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

package dev.foxgirl.rembulan.runtime;

import dev.foxgirl.rembulan.Ordering;
import dev.foxgirl.rembulan.util.Cons;

import java.util.Objects;

/**
 * A Lua coroutine.
 *
 * <p>This class does not expose any public API; to manipulate {@code Coroutine} objects,
 * use the following methods in {@link ExecutionContext}:</p>
 * <ul>
 *     <li>to create a new coroutine, use {@link ExecutionContext#newCoroutine(LuaFunction)};</li>
 *     <li>to get coroutine status, use {@link ExecutionContext#getCoroutineStatus(Coroutine)};</li>
 *     <li>to resume a coroutine, use {@link ExecutionContext#resume(Coroutine, Object[])};</li>
 *     <li>to yield from a coroutine, use {@link ExecutionContext#yield(Object[])}.</li>
 * </ul>
 *
 * <p><b>Note on equality:</b> according to §3.4.4 of the Lua Reference Manual,
 * coroutines {@code a} and {@code b} are expected to be equal if and only if they are
 * the same object. However, {@link Ordering#isRawEqual(Object, Object)} compares
 * coroutines using {@link Object#equals(Object)}. <b>Exercise caution when overriding
 * {@code equals()}.</b></p>
 */
public final class Coroutine {

	// paused call stack: up-to-date only iff coroutine is not running
	private Cons<ResumeInfo> callStack;
	private Status status;

	Coroutine(Object body) {
		this.callStack = new Cons<>(new ResumeInfo(BootstrapResumable.INSTANCE, body));
		this.status = Status.SUSPENDED;
	}

	/**
	 * Coroutine status.
	 */
	public enum Status {

		/**
		 * The status of a <i>suspended</i> coroutine, i.e., a coroutine that may be resumed.
		 */
		SUSPENDED,

		/**
		 * The status of a <i>running</i> coroutine, i.e., a coroutine that is currently executing.
		 */
		RUNNING,

		/**
		 * The status of a coroutine that is resuming another coroutine.
		 */
		NORMAL,

		/**
		 * The status of a <i>dead</i> coroutine, i.e., a coroutine that has finished execution.
		 */
		DEAD
	}

	synchronized Status getStatus() {
		return status;
	}

	private static class BootstrapResumable implements Resumable {

		static final BootstrapResumable INSTANCE = new BootstrapResumable();

		@Override
		public void resume(ExecutionContext context, Object target) throws ResolvedControlThrowable {
			try {
				Dispatch.call(context, target, context.getReturnBuffer().getAsArray());
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve();
			}
		}

	}

	// (RUNNING, SUSPENDED) -> (NORMAL, RUNNING)
	static Cons<ResumeInfo> _resume(final Coroutine a, final Coroutine b, Cons<ResumeInfo> cs) {
		Objects.requireNonNull(a);
		Objects.requireNonNull(b);
		Objects.requireNonNull(cs);

		synchronized (a) {
			if (a.status == Status.RUNNING) {
				synchronized (b) {
					if (b.status == Status.SUSPENDED) {
						Cons<ResumeInfo> result = b.callStack;
						a.callStack = cs;
						b.callStack = null;
						a.status = Status.NORMAL;
						b.status = Status.RUNNING;
						return result;
					}
					else {
						if (b.status == Status.DEAD) {
							throw Errors.resumeDeadCoroutine();
						}
						else {
							throw Errors.resumeNonSuspendedCoroutine();
						}
					}
				}
			}
			else {
				throw new IllegalStateException("resuming coroutine not in running state");
			}
		}
	}

	// (NORMAL, RUNNING) -> (RUNNING, SUSPENDED)
	static Cons<ResumeInfo> _yield(final Coroutine a, final Coroutine b, Cons<ResumeInfo> cs) {
		synchronized (a) {
			if (a.status == Status.NORMAL) {
				synchronized (b) {
					if (b.status == Status.RUNNING) {
						Cons<ResumeInfo> result = a.callStack;
						a.callStack = null;
						b.callStack = cs;
						a.status = Status.RUNNING;
						b.status = b.callStack != null ? Status.SUSPENDED : Status.DEAD;
						return result;
					}
					else {
						throw new IllegalCoroutineStateException("yielding coroutine not in running state");
					}
				}
			}
			else {
				throw new IllegalCoroutineStateException("yielding coroutine not in normal state");
			}
		}
	}

	// (NORMAL, RUNNING) -> (RUNNING, DEAD)
	static Cons<ResumeInfo> _return(Coroutine a, Coroutine b) {
		return _yield(a, b, null);
	}

	synchronized Cons<ResumeInfo> unpause() {
		// TODO: check status?
		status = Status.RUNNING;
		Cons<ResumeInfo> result = callStack;
		callStack = null;
		return result;
	}

	synchronized void pause(Cons<ResumeInfo> callStack) {
		// TODO: check status?
		this.callStack = callStack;
	}

}
