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

/**
 * Abstract function of an arbitrary number of arguments.
 */
public abstract class AbstractFunctionAnyArg extends LuaFunction {

	@Override
	public void invoke(ExecutionContext context) throws ResolvedControlThrowable {
		invoke(context, new Object[] { });
	}

	@Override
	public void invoke(ExecutionContext context, Object arg1) throws ResolvedControlThrowable {
		invoke(context, new Object[] { arg1 });
	}

	@Override
	public void invoke(ExecutionContext context, Object arg1, Object arg2) throws ResolvedControlThrowable {
		invoke(context, new Object[] { arg1, arg2 });
	}

	@Override
	public void invoke(ExecutionContext context, Object arg1, Object arg2, Object arg3) throws ResolvedControlThrowable {
		invoke(context, new Object[] { arg1, arg2, arg3 });
	}

	@Override
	public void invoke(ExecutionContext context, Object arg1, Object arg2, Object arg3, Object arg4) throws ResolvedControlThrowable {
		invoke(context, new Object[] { arg1, arg2, arg3, arg4 });
	}

	@Override
	public void invoke(ExecutionContext context, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) throws ResolvedControlThrowable {
		invoke(context, new Object[] { arg1, arg2, arg3, arg4, arg5 });
	}

}
