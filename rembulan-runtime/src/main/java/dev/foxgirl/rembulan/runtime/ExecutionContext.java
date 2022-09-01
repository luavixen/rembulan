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

/**
 * An interface to the execution context of a Lua call.
 *
 * <p>This is the interface to the runtime environment in which Lua calls are executed,
 * providing the mechanisms for
 * including coroutine switching and the (optional) cooperative scheduler.</p>
 *
 * <p><b>Note</b>: Lua functions are <b>not</b> guaranteed to receive the same
 * {@code ExecutionContext} instance between an invoke and a subsequent resume.
 * Functions should therefore not retain the reference to the {@code ExecutionContext}
 * that outlives an {@code invoke} or {@code resume}.</p>
 *
 * <p><b>Note</b>: The behaviour of the methods in this interface is <i>undefined</i> when
 * invoked outside a Lua function invoke/resume.</p>
 */
public interface ExecutionContext extends StateContext {

	/**
	 * Returns the return buffer used in this execution context.
	 *
	 * <p>This is the mechanism by which Lua functions may return values (see e.g.
	 * {@link ReturnBuffer#setToContentsOf(Object[])}), or indicate that the return
	 * value is the result of a tail call (by e.g.
	 * {@link ReturnBuffer#setToCallWithContentsOf(Object, Object[])}).</p>
	 *
	 * <p>The return values of Lua calls initiated from this execution context will
	 * also be stored in a return buffer accessible by this method (e.g. by
	 * {@link ReturnBuffer#getAsArray()} or {@link ReturnBuffer#get(int)}).</p>
	 *
	 * <p><b>Note</b>: the return buffer instance may change between subsequent invokes
	 * and resumes of the same function. The reference to the {@code ReturnBuffer} instance
	 * should therefore not be retained by the executed function beyond the scope of a single
	 * invoke or resume.</p>
	 *
	 * @return  the return buffer used in this execution context
	 */
	@SuppressWarnings("unused")
	ReturnBuffer getReturnBuffer();

	/**
	 * Returns the current coroutine.
	 *
	 * @return  the current coroutine
	 */
	@SuppressWarnings("unused")
	Coroutine getCurrentCoroutine();

	/**
	 * Returns {@code true} if the current coroutine (as returned
	 * by {@link #getCurrentCoroutine()}) is the main coroutine.
	 *
	 * @return  {@code true} if the current coroutine is the main coroutine
	 */
	@SuppressWarnings("unused")
	boolean isInMainCoroutine();

	/**
	 * Returns the status of {@code coroutine} as seen from the perspective of this execution
	 * context.
	 *
	 * @param coroutine  the target coroutine, must not be {@code null}
	 * @return  the status of {@code coroutine} from the perspective of this execution context
	 *
	 * @throws NullPointerException  if {@code coroutine} is {@code null}
	 */
	Coroutine.Status getCoroutineStatus(Coroutine coroutine);

	/**
	 * Returns a new coroutine with the body {@code function}.
	 *
	 * <p>The coroutine will be initialised in the {@linkplain Coroutine.Status#SUSPENDED
	 * suspended state}. To resume the coroutine, use {@link #resume(Coroutine, Object[])}.</p>
	 *
	 * @param function  coroutine body, must not be {@code null}
	 * @return  a new (suspended) coroutine with the body {@code function}
	 *
	 * @throws NullPointerException  if {@code function} is {@code null}
	 */
	@SuppressWarnings("unused")
	Coroutine newCoroutine(LuaFunction function);

	/**
	 * Resumes the given coroutine {@code coroutine}, passing the arguments {@code args}
	 * to it.
	 * <b>This method throws an {@link UnresolvedControlThrowable}</b>: non-local control
	 * changes are expected to be resolved by the caller of this method.
	 *
	 * <p>The reference to the array {@code args} is not retained by the execution context;
	 * {@code args} may therefore be freely re-used by the caller.</p>
	 *
	 * @param coroutine  the coroutine to be resumed, must not be {@code null}
	 * @param args  arguments to be passed to {@code coroutine}, must not be {@code null}
	 *
	 * @throws UnresolvedControlThrowable  the control throwable for this coroutine switch
	 * @throws NullPointerException  if {@code coroutine} or {@code args} is {@code null}
	 * @throws IllegalCoroutineStateException  when {@code coroutine} cannot be resumed
	 */
	@SuppressWarnings("unused")
	void resume(Coroutine coroutine, Object[] args) throws UnresolvedControlThrowable;

	/**
	 * Yields control to the coroutine resuming the current coroutine, passing the
	 * arguments {@code args} to it.
	 * <b>This method throws an {@link UnresolvedControlThrowable}</b>: non-local control
	 * changes are expected to be resolved by the caller of this method.
	 *
	 * <p>The reference to the array {@code args} is not retained by the execution context;
	 * {@code args} may therefore be freely re-used by the caller.</p>
	 *
	 * @param args  arguments to be passed to the resuming coroutine, must not be {@code null}
	 *
	 * @throws UnresolvedControlThrowable  the control throwable for this coroutine switch
	 * @throws NullPointerException  if {@code args} is {@code null}
	 * @throws IllegalCoroutineStateException  when yielding from a non-yieldable coroutine
	 */
	@SuppressWarnings("unused")
	void yield(Object[] args) throws UnresolvedControlThrowable;

	/**
	 * Resumes the current call after the asynchronous task {@code task} has been completed.
	 * <b>This method throws an {@link UnresolvedControlThrowable}</b>: non-local control
	 * changes are expected to be resolved by the caller of this method.
	 *
	 * <p>In order to mark {@code task} as completed, the task must call
	 * {@link AsyncTask.ContinueCallback#finished()}.</p>
	 *
	 * @param task  the task to be executed, must not be {@code null}
	 *
	 * @throws UnresolvedControlThrowable  the control throwable for this control change
	 * @throws NullPointerException  if {@code task} is {@code null}
	 */
	@SuppressWarnings("unused")
	void resumeAfter(AsyncTask task) throws UnresolvedControlThrowable;

	/**
	 * Informs the scheduler that the current task is about to consume or has consumed
	 * {@code ticks} virtual ticks.
	 *
	 * <p>This method only registers {@code ticks} with the scheduler. In order to pause
	 * the execution if the scheduler indicates that it should be paused, use
	 * {@link #pauseIfRequested()}.</p>
	 *
	 * <p>The behaviour of this method is undefined if {@code ticks} is negative.</p>
	 *
	 * @param ticks  number of ticks to be registered with the scheduler, must not be negative
	 */
	@SuppressWarnings("unused")
	void registerTicks(int ticks);

	/**
	 * Pauses the execution if the according to the scheduler this call should be paused.
	 * <b>This method throws an {@link UnresolvedControlThrowable}</b>: non-local control
	 * changes are expected to be resolved by the caller of this method.
	 *
	 * <p>To pause execution unconditionally, use {@link #pause()}.</p>
	 *
	 * @throws UnresolvedControlThrowable  the control throwable for this control change
	 */
	@SuppressWarnings("unused")
	void pauseIfRequested() throws UnresolvedControlThrowable;

	/**
	 * (Unconditionally) pauses the execution.
	 * <b>This method throws an {@link UnresolvedControlThrowable}</b>: non-local control
	 * changes are expected to be resolved by the caller of this method.
	 *
	 * @throws UnresolvedControlThrowable  the control throwable for this control change
	 */
	@SuppressWarnings("unused")
	void pause() throws UnresolvedControlThrowable;

}
