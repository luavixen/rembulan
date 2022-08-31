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
import dev.foxgirl.rembulan.exec.CallException;
import dev.foxgirl.rembulan.exec.CallPausedException;
import dev.foxgirl.rembulan.exec.DirectCallExecutor;
import dev.foxgirl.rembulan.impl.StateContexts;
import dev.foxgirl.rembulan.load.LoaderException;
import dev.foxgirl.rembulan.runtime.*;

public class SimpleBlockingAsync {

	static class ExampleFunction extends AbstractFunction0 {

		@Override
		public void invoke(ExecutionContext context) throws ResolvedControlThrowable {
			System.out.println("invoke");

			try {
				context.resumeAfter(new AsyncTask() {
					@Override
					public void execute(ContinueCallback callback) {
						System.out.println("in task");
						callback.finished();
					}
				});
			}
			catch (UnresolvedControlThrowable ct) {
				throw ct.resolve(this, null);
			}

			// control never reaches this point
			System.out.println("after async -- not!");
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			System.out.println("resume");
			context.getReturnBuffer().setTo();
		}

	}

	public static void main(String[] args)
			throws InterruptedException, CallPausedException, CallException, LoaderException {

		StateContext state = StateContexts.newDefaultInstance();
		DirectCallExecutor.newExecutor().call(state, new ExampleFunction());
	}

}
