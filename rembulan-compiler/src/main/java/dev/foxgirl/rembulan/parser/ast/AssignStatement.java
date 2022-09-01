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

import java.util.List;
import java.util.Objects;

public class AssignStatement extends BodyStatement {

	private final List<LValueExpr> vars;
	private final List<Expr> exprs;

	public AssignStatement(Attributes attr, List<LValueExpr> vars, List<Expr> exprs) {
		super(attr);
		this.vars = Objects.requireNonNull(vars);
		this.exprs = Objects.requireNonNull(exprs);
	}

	public List<LValueExpr> vars() {
		return vars;
	}

	public List<Expr> exprs() {
		return exprs;
	}

	public AssignStatement update(List<LValueExpr> vars, List<Expr> exprs) {
		if (this.vars.equals(vars) && this.exprs.equals(exprs)) {
			return this;
		}
		else {
			return new AssignStatement(attributes(), vars, exprs);
		}
	}

	@Override
	public BodyStatement accept(Transformer tf) {
		return tf.transform(this);
	}

}
