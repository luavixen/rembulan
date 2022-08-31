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

package dev.foxgirl.rembulan.examples;

import dev.foxgirl.rembulan.StateContext;
import dev.foxgirl.rembulan.Table;
import dev.foxgirl.rembulan.Variable;
import dev.foxgirl.rembulan.compiler.CompilerChunkLoader;
import dev.foxgirl.rembulan.env.RuntimeEnvironments;
import dev.foxgirl.rembulan.exec.CallException;
import dev.foxgirl.rembulan.exec.CallPausedException;
import dev.foxgirl.rembulan.exec.DirectCallExecutor;
import dev.foxgirl.rembulan.impl.NonsuspendableFunctionException;
import dev.foxgirl.rembulan.impl.StateContexts;
import dev.foxgirl.rembulan.lib.StandardLibrary;
import dev.foxgirl.rembulan.load.ChunkLoader;
import dev.foxgirl.rembulan.load.LoaderException;
import dev.foxgirl.rembulan.runtime.AbstractFunction0;
import dev.foxgirl.rembulan.runtime.ExecutionContext;
import dev.foxgirl.rembulan.runtime.LuaFunction;
import dev.foxgirl.rembulan.runtime.ResolvedControlThrowable;

import java.util.Arrays;

public class SimpleGlobalFunction {

	// A simple function that returns the result of System.currentTimeMillis()
	static class Now extends AbstractFunction0 {

		@Override
		public void invoke(ExecutionContext context) throws ResolvedControlThrowable {
			context.getReturnBuffer().setTo(System.currentTimeMillis());
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			throw new NonsuspendableFunctionException();
		}

	}

	public static void main(String[] args)
			throws InterruptedException, CallPausedException, CallException, LoaderException {

		// initialise state
		StateContext state = StateContexts.newDefaultInstance();

		// load the standard library; env is the global environment
		Table env = StandardLibrary.in(RuntimeEnvironments.system()).installInto(state);
		env.rawset("now", new Now());

		// load the main function
		ChunkLoader loader = CompilerChunkLoader.of("example");
		LuaFunction main = loader.loadTextChunk(new Variable(env), "example", "return now()");

		// run the main function
		Object[] result = DirectCallExecutor.newExecutor().call(state, main);

		// prints the number of milliseconds since 1 Jan 1970
		System.out.println("Result: " + Arrays.toString(result));

	}

}
