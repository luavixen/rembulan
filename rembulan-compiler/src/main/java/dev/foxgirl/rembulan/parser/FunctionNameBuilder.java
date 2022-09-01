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

package dev.foxgirl.rembulan.parser;

import dev.foxgirl.rembulan.parser.ast.LValueExpr;
import dev.foxgirl.rembulan.parser.ast.Name;
import dev.foxgirl.rembulan.parser.ast.SourceInfo;
import dev.foxgirl.rembulan.parser.ast.StringLiteral;

import java.util.Objects;

class FunctionNameBuilder {

	private LValueExpr lv;
	private boolean method;

	public FunctionNameBuilder(SourceElement<Name> srcName) {
		Objects.requireNonNull(srcName);
		this.lv = Exprs.var(srcName.sourceInfo(), srcName.element());
		this.method = false;
	}

	public void addDotName(SourceInfo srcDot, SourceElement<Name> srcName) {
		Objects.requireNonNull(srcDot);
		Objects.requireNonNull(srcName);
		lv = Exprs.index(srcDot, lv, Exprs.literal(srcName.sourceInfo(), StringLiteral.fromName(srcName.element())));
	}

	public void addColonName(SourceInfo srcColon, SourceElement<Name> srcName) {
		addDotName(srcColon, srcName);
		method = true;
	}

	public boolean isMethod() {
		return method;
	}

	public LValueExpr get() {
		return lv;
	}

}
