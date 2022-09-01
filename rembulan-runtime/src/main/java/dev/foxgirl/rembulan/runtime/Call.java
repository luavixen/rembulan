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

import dev.foxgirl.rembulan.StateContext;
import dev.foxgirl.rembulan.exec.CallEventHandler;
import dev.foxgirl.rembulan.exec.Continuation;
import dev.foxgirl.rembulan.exec.InvalidContinuationException;
import dev.foxgirl.rembulan.exec.OneShotContinuation;
import dev.foxgirl.rembulan.impl.AbstractStateContext;
import dev.foxgirl.rembulan.util.Cons;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An object representing a (potentially paused) Lua function call.
 */
class Call {

	private final StateContext stateContext;
	private final ReturnBuffer returnBuffer;

	private Cons<Coroutine> coroutineStack;

	private final AtomicInteger currentVersion;

	private static final int VERSION_RUNNING = 0;
	private static final int VERSION_TERMINATED = 1;

	private Call(
			StateContext stateContext,
			ReturnBuffer returnBuffer,
			Coroutine mainCoroutine) {

		this.stateContext = Objects.requireNonNull(stateContext);
		this.returnBuffer = Objects.requireNonNull(returnBuffer);

		this.coroutineStack = new Cons<>(Objects.requireNonNull(mainCoroutine));

		int startingVersion = newPausedVersion(0);
		this.currentVersion = new AtomicInteger(startingVersion);
	}

	/**
	 * Constructs a new {@code Call} object representing the call to the object {@code fn}
	 * with the arguments stored in {@code args}, in the context of the Lua state {@code state}.
	 *
	 * <p>The call will be initialised in the {@link State#PAUSED paused state}.</p>
	 *
	 * @param stateContext  state context used by the call, must not be {@code null}
	 * @param returnBufferFactory  return buffer factory used by the call, must not be {@code null}
	 * @param fn  the call target, may be any value
	 * @param args  the array of call arguments, must not be {@code null}
	 * @return  a new {@code Call} object
	 *
	 * @throws NullPointerException  if {@code stateContext}, {@code returnBufferFactory}
	 *                               or {@code args} is {@code null}
	 */
	public static Call init(
			StateContext stateContext,
			ReturnBufferFactory returnBufferFactory,
			Object fn,
			Object... args) {

		ReturnBuffer returnBuffer = returnBufferFactory.newInstance();
		Coroutine c = new Coroutine(fn);
		returnBuffer.setToContentsOf(args);
		return new Call(stateContext, returnBuffer, c);
	}

	/**
	 * An enum representing the state of the call.
	 */
	public enum State {

		/**
		 * Paused state. When a call is in a paused state, it can be resumed, and it has
		 * a current continuation accessible using {@link #getCurrentContinuation()}.
		 *
		 */
		PAUSED,

		/**
		 * Running state. The call is currently executing.
		 */
		RUNNING,

		/**
		 * Terminated state. The call has terminated.
		 */
		TERMINATED

	}

	private static int newPausedVersion(int oldVersion) {
		int v;
		Random versionSource = ThreadLocalRandom.current();
		do {
			v = versionSource.nextInt();
		} while (!isPaused(v) || v == oldVersion);
		return v;
	}

	private static boolean isPaused(int version) {
		return version != VERSION_RUNNING
				&& version != VERSION_TERMINATED;
	}

	private static State versionToState(int version) {
		switch (version) {
			case VERSION_RUNNING:    return State.RUNNING;
			case VERSION_TERMINATED: return State.TERMINATED;
			default:                 return State.PAUSED;
		}
	}

	/**
	 * Returns the current state of the call.
	 *
	 * @return  the state of the call
	 */
	public State getState() {
		return versionToState(currentVersion.get());
	}

	private class CallContinuation implements OneShotContinuation {

		private final int version;

		private CallContinuation(int version) {
			this.version = version;
		}

		private Call outer() {
			return Call.this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CallContinuation that = (CallContinuation) o;

			return this.version == that.version && this.outer().equals(that.outer());
		}

		@Override
		public int hashCode() {
			int result = outer().hashCode();
			result = 31 * result + version;
			return result;
		}

		@Override
		public Object callIdentifier() {
			return outer();
		}

		@Override
		public void resume(CallEventHandler handler, SchedulingContext schedulingContext) {
			Call.this.resume(handler, schedulingContext, version);
		}

		@Override
		public boolean isCurrent() {
			return this.version == Call.this.currentVersion.get();
		}

	}

	/**
	 * Returns the current continuation of this call.
	 *
	 * <p>The call must be in the paused state; otherwise, an {@link IllegalStateException} is
	 * thrown.</p>
	 *
	 * @return  the current continuation of the call
	 *
	 * @throws IllegalStateException  if this call is not in the paused state
	 */
	public OneShotContinuation getCurrentContinuation() {
		int version = currentVersion.get();

		if (!isPaused(version)) {
			State s = versionToState(version);
			throw new IllegalStateException("Cannot get continuation of a " + s + " call");
		}
		else {
			return new CallContinuation(version);
		}
	}

	private void resume(CallEventHandler handler, SchedulingContext schedulingContext, int version) {
		Objects.requireNonNull(handler);
		Objects.requireNonNull(schedulingContext);

		if (version == VERSION_RUNNING || version == VERSION_TERMINATED) {
			throw new IllegalArgumentException("Illegal version: " + version);
		}

		// claim this version
		if (!currentVersion.compareAndSet(version, VERSION_RUNNING)) {
			throw new InvalidContinuationException("Cannot resume call: not in the expected state (0x"
					+ Integer.toHexString(version) + ")");
		}

		int newVersion = VERSION_TERMINATED;
		ResumeResult rr = null;
		OneShotContinuation cont = null;
		try {
			Resumer resumer = new Resumer(schedulingContext);
			rr = resumer.resume();
			assert (rr != null);
			if (rr.pause || rr.asyncTask != null) {
				newVersion = newPausedVersion(version);
				cont = new CallContinuation(newVersion);
			}
		}
		finally {
			int old = currentVersion.getAndSet(newVersion);
			assert (old == VERSION_RUNNING);
		}

		assert (rr != null);

		rr.fire(handler, this, cont);
	}

	private static class ControlPayload extends ControlThrowablePayload {

		private final boolean preempted;
		private final Coroutine target;
		private final Object[] values;
		private final AsyncTask task;

		private ControlPayload(boolean preempted, Coroutine target, Object[] values, AsyncTask task) {
			if ((preempted && target == null && values == null && task == null)
					|| (!preempted && values != null && task == null)
					|| (!preempted && target == null && values == null && task != null)) {

				this.preempted = preempted;
				this.target = target;
				this.values = values;
				this.task = task;
			}
			else {
				throw new IllegalArgumentException("Illegal arguments: ("
						+ preempted + ", " + target + ", " + Arrays.toString(values) + ", " + task + ")");
			}
		}

		@Override
		public void accept(ControlThrowablePayload.Visitor visitor) {
			if (preempted) visitor.preempted();
			else if (target != null && values != null) visitor.coroutineResume(target, values);
			else if (target == null && values != null) visitor.coroutineYield(values);
			else if (task != null) visitor.async(task);
			else throw new AssertionError();
		}

	}

	private static final ControlPayload PAUSED_PAYLOAD = new ControlPayload(true, null, null, null);

	private static final class ResumeResult {

		private final boolean pause;
		private final Object[] values;
		private final Throwable error;
		private final AsyncTask asyncTask;

		private ResumeResult(boolean pause, Object[] values, Throwable error, AsyncTask asyncTask) {
			// validate: at most one may be true/non-null
			if ((pause && (values == null && error == null && asyncTask == null))
					|| (values != null && (!pause && error == null && asyncTask == null))
					|| (error != null && (!pause && values == null && asyncTask == null))
					|| (asyncTask != null && (!pause && values == null && error == null))) {
				this.pause = pause;
				this.values = values;
				this.error = error;
				this.asyncTask = asyncTask;
			}
			else {
				throw new IllegalArgumentException("Illegal arguments: ("
						+ pause + ", " + Arrays.toString(values) + ", " + error + ", " + asyncTask + ")");
			}
		}

		void fire(CallEventHandler handler, Call c, Continuation cont) {
			if (pause) {
				assert (cont != null);
				handler.paused(c, cont);
			}
			else if (values != null) {
				handler.returned(c, values);
			}
			else if (error != null) {
				handler.failed(c, error);
			}
			else if (asyncTask != null) {
				assert (cont != null);
				handler.async(c, cont, asyncTask);
			}
			else {
				throw new AssertionError();
			}
		}

	}

	private static final ResumeResult PAUSE_RESULT = new ResumeResult(true, null, null, null);

	class Resumer extends AbstractStateContext implements ExecutionContext, ControlThrowablePayload.Visitor {

		private final SchedulingContext schedulingContext;

		private ResumeResult result;
		private Throwable error;
		private Cons<ResumeInfo> callStack;

		Resumer(SchedulingContext schedulingContext) {
			super(stateContext);
			this.schedulingContext = Objects.requireNonNull(schedulingContext);
			this.result = null;
			this.error = null;
		}

		@Override
		public ReturnBuffer getReturnBuffer() {
			return returnBuffer;
		}

		@Override
		public Coroutine getCurrentCoroutine() {
			return coroutineStack.car;
		}

		@Override
		public Coroutine newCoroutine(LuaFunction function) {
			return new Coroutine(Objects.requireNonNull(function));
		}

		@Override
		public boolean isInMainCoroutine() {
			return coroutineStack.cdr != null;
		}

		@Override
		public Coroutine.Status getCoroutineStatus(Coroutine coroutine) {
			return coroutine.getStatus();
		}

		@Override
		public void resume(Coroutine coroutine, Object[] args) throws UnresolvedControlThrowable {
			Objects.requireNonNull(coroutine);
			throw new UnresolvedControlThrowable(new ControlPayload(
					false, Objects.requireNonNull(coroutine), Objects.requireNonNull(args), null));
		}

		@Override
		public void yield(Object[] args) throws UnresolvedControlThrowable {
			throw new UnresolvedControlThrowable(new ControlPayload(
					false, null, Objects.requireNonNull(args), null));
		}

		@Override
		public void resumeAfter(AsyncTask task) throws UnresolvedControlThrowable {
			throw new UnresolvedControlThrowable(new ControlPayload(
					false, null, null, Objects.requireNonNull(task)));
		}

		@Override
		public void registerTicks(int ticks) {
			schedulingContext.registerTicks(ticks);
		}

		@Override
		public void pauseIfRequested() throws UnresolvedControlThrowable {
			if (schedulingContext.shouldPause()) {
				pause();
			}
		}

		@Override
		public void pause() throws UnresolvedControlThrowable {
			throw new UnresolvedControlThrowable(PAUSED_PAYLOAD);
		}

		@Override
		public void preempted() {
			result = PAUSE_RESULT;
		}

		@Override
		public void coroutineYield(Object[] values) {
			assert (coroutineStack != null);

			if (coroutineStack.cdr == null) {
				error = Errors.illegalYieldAttempt();
			}
			else {
				Coroutine top = coroutineStack.car;
				Coroutine prev = coroutineStack.cdr.car;

				boolean yielded = false;
				try {
					callStack = Coroutine._yield(prev, top, callStack);
					yielded = true;
				}
				catch (IllegalCoroutineStateException ex) {
					error = ex;
				}

				if (yielded) {
					coroutineStack = coroutineStack.cdr;
					getReturnBuffer().setToContentsOf(values);
				}
			}
		}

		public void coroutineReturn() {
			assert (coroutineStack != null);

			if (coroutineStack.cdr == null) {
				// this was the main coroutine
				if (error == null) {
					Object[] values = getReturnBuffer().getAsArray();
					result = new ResumeResult(false, values, null, null);
				}
				else {
					result = new ResumeResult(false, null, error, null);
				}
			}
			else {
				// an implicit yield
				Coroutine top = coroutineStack.car;
				Coroutine prev = coroutineStack.cdr.car;

				boolean yielded = false;
				try {
					callStack = Coroutine._return(prev, top);
					yielded = true;
				}
				catch (IllegalCoroutineStateException ex) {
					error = ex;
				}

				if (yielded) {
					coroutineStack = coroutineStack.cdr;
				}
			}
		}

		@Override
		public void coroutineResume(Coroutine target, Object[] values) {
			assert (coroutineStack != null);

			Coroutine prev = coroutineStack.car;

			boolean resumed = false;
			try {
				callStack = Coroutine._resume(prev, target, callStack);
				resumed = true;
			}
			catch (IllegalCoroutineStateException ex) {
				error = ex;
			}

			if (resumed) {
				coroutineStack = new Cons<>(target, coroutineStack);
				getReturnBuffer().setToContentsOf(values);
			}
		}

		@Override
		public void async(AsyncTask task) {
			result = new ResumeResult(false, null, null, task);
		}

		private void saveFrames(ResolvedControlThrowable ct) {
			Iterator<ResumeInfo> it = ct.frames();
			while (it.hasNext()) {
				callStack = new Cons<>(it.next(), callStack);
			}
		}

		private void continueCurrentCoroutine() {
			while (callStack != null) {
				ResumeInfo top = callStack.car;
				callStack = callStack.cdr;

				try {
					if (top.resume(this, error)) {
						error = null;  // top was run
					}
				}
				catch (ResolvedControlThrowable ct) {
					saveFrames(ct);
					ct.payload().accept(this);
					return;
				}
				catch (Exception ex) {
					// unhandled exception: will try finding a handler in the next iteration
					error = ex;
				}
			}

			assert (callStack == null);

			coroutineReturn();
		}

		ResumeResult resume() {
			try {
				callStack = coroutineStack.car.unpause();
				do {
					continueCurrentCoroutine();
				} while (result == null);

				return result;
			}
			finally {
				if (coroutineStack != null) {
					coroutineStack.car.pause(callStack);
				}
			}
		}

	}

}
