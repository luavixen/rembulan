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

public class GetJavaVersion {

	static class JavaVersion extends AbstractFunction0 {

		@Override
		public void invoke(ExecutionContext context) throws ResolvedControlThrowable {
			String javaVmName = System.getProperty("java.vm.name");
			String javaVersion = System.getProperty("java.version");
			context.getReturnBuffer().setTo(javaVmName, javaVersion);
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			throw new NonsuspendableFunctionException();
		}

	}


	public static void main(String[] args)
			throws InterruptedException, CallPausedException, CallException, LoaderException {

		String program = "local vmname, version = javaversion()\n"
				+ "return 'Java VM name = \"'..vmname..'\", Java version = \"'..version..'\"'";

		StateContext state = StateContexts.newDefaultInstance();
		Table env = StandardLibrary.in(RuntimeEnvironments.system()).installInto(state);
		env.rawset("javaversion", new JavaVersion());

		ChunkLoader loader = CompilerChunkLoader.of("call_from_lua");
		LuaFunction main = loader.loadTextChunk(new Variable(env), "", program);

		Object[] result = DirectCallExecutor.newExecutor().call(state, main);

		System.out.println("Result: " + Arrays.toString(result));

	}

}
