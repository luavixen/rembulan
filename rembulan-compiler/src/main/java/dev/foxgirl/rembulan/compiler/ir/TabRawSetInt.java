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

package dev.foxgirl.rembulan.compiler.ir;

import dev.foxgirl.rembulan.util.Check;

import java.util.Objects;

public class TabRawSetInt extends BodyNode {

	private final Val obj;
	private final long idx;
	private final Val value;

	public TabRawSetInt(Val obj, long idx, Val value) {
		this.obj = Objects.requireNonNull(obj);
		this.idx = Check.positive(idx);
		this.value = Objects.requireNonNull(value);
	}

	public Val obj() {
		return obj;
	}

	public long idx() {
		return idx;
	}

	public Val value() {
		return value;
	}

	@Override
	public void accept(IRVisitor visitor) {
		visitor.visit(this);
	}

}
