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

import dev.foxgirl.rembulan.Conversions;
import dev.foxgirl.rembulan.Variable;
import dev.foxgirl.rembulan.runtime.*;

public class AsyncExample extends AbstractFunction1 {

	@Override
	public void invoke(ExecutionContext context, Object arg) throws ResolvedControlThrowable {
		final long millis = Conversions.toIntegerValue(arg);
		final Variable v = new Variable(null);
		try {
			context.resumeAfter(new AsyncTask() {
				@Override
				public void execute(final ContinueCallback callback) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								long before = System.currentTimeMillis();
								Thread.sleep(millis);
								long after = System.currentTimeMillis();
								v.set(after - before);
							}
							catch (InterruptedException ex) {
								// ignore
							}
							finally {
								callback.finished();
							}
						}
					}).start();
				}
			});

			// control should never reach this point
			throw new AssertionError();
		}
		catch (UnresolvedControlThrowable ct) {
			throw ct.resolve(this, v);
		}
	}

	@Override
	public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
		Variable v = (Variable) suspendedState;
		context.getReturnBuffer().setTo(v.get());
	}

}
