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

import java.util.Objects;

public class PhiLoad extends BodyNode {

	private final Val dest;
	private final PhiVal src;

	public PhiLoad(Val dest, PhiVal src) {
		this.dest = Objects.requireNonNull(dest);
		this.src = Objects.requireNonNull(src);
	}

	public Val dest() {
		return dest;
	}

	public PhiVal src() {
		return src;
	}

	@Override
	public void accept(IRVisitor visitor) {
		visitor.visit(this);
	}

}
