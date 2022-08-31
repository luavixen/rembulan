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

package dev.foxgirl.rembulan.lib;

import dev.foxgirl.rembulan.impl.NonsuspendableFunctionException;
import dev.foxgirl.rembulan.runtime.AbstractFunctionAnyArg;
import dev.foxgirl.rembulan.runtime.Dispatch;
import dev.foxgirl.rembulan.runtime.ExecutionContext;
import dev.foxgirl.rembulan.runtime.ResolvedControlThrowable;

/**
 * An abstract function that takes an arbitrary number of arguments passed wrapped
 * in an {@link ArgumentIterator} object.
 */
public abstract class AbstractLibFunction extends AbstractFunctionAnyArg {

	/**
	 * Returns the name of the function for error-reporting.
	 *
	 * @return  the function name
	 */
	protected abstract String name();

	/**
	 * Invokes the function in the context {@code context} with arguments passed in
	 * the iterator {@code args}.
	 *
	 * <p>This is the method that is meant to be implemented by the function implementation.
	 * The function should not retain a reference to {@code context} or {@code args}
	 * beyond the scope of its invocation. In particular, {@code context} and {@code args}
	 * should not be part of suspended state if this method throws a control throwable.</p>
	 *
	 * <p>{@code context} and {@code args} are guaranteed to be non-{@code null} when
	 * the function is invoked via {@link Dispatch Dispatch}.</p>
	 *
	 * @param context  execution context, must not be {@code null}
	 * @param args  call arguments, must not be {@code null}
	 *
	 * @throws ResolvedControlThrowable  if the call initiates a non-local control change
	 */
	protected abstract void invoke(ExecutionContext context, ArgumentIterator args) throws ResolvedControlThrowable;

	@Override
	public void invoke(ExecutionContext context, Object[] args) throws ResolvedControlThrowable {
		invoke(context, new ArgumentIterator(context, name(), args));
	}

	@Override
	public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
		throw new NonsuspendableFunctionException(this.getClass());
	}

}
