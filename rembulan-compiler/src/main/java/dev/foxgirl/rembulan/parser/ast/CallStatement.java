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

package dev.foxgirl.rembulan.parser.ast;

import java.util.Objects;

public class CallStatement extends BodyStatement {

	private final CallExpr callExpr;

	public CallStatement(Attributes attr, CallExpr callExpr) {
		super(attr);
		this.callExpr = Objects.requireNonNull(callExpr);
	}

	public CallExpr callExpr() {
		return callExpr;
	}

	public CallStatement update(CallExpr callExpr) {
		if (this.callExpr.equals(callExpr)) {
			return this;
		}
		else {
			return new CallStatement(attributes(), callExpr);
		}
	}

	@Override
	public BodyStatement accept(Transformer tf) {
		return tf.transform(this);
	}

}
