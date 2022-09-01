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

public class GenericForStatement extends BodyStatement {

	private final List<Name> names;
	private final List<Expr> exprs;
	private final Block block;

	public GenericForStatement(Attributes attr, List<Name> names, List<Expr> exprs, Block block) {
		super(attr);
		this.names = Objects.requireNonNull(names);
		this.exprs = Objects.requireNonNull(exprs);
		this.block = Objects.requireNonNull(block);
	}

	public List<Name> names() {
		return names;
	}

	public List<Expr> exprs() {
		return exprs;
	}

	public Block block() {
		return block;
	}

	public GenericForStatement update(List<Name> names, List<Expr> exprs, Block block) {
		if (this.names.equals(names) && this.exprs.equals(exprs) && this.block.equals(block)) {
			return this;
		}
		else {
			return new GenericForStatement(attributes(), names, exprs, block);
		}
	}

	public GenericForStatement withAttributes(Attributes attr) {
		if (attributes().equals(attr)) return this;
		else return new GenericForStatement(attr, names, exprs, block);
	}

	public GenericForStatement with(Object o) {
		return this.withAttributes(attributes().with(o));
	}

	@Override
	public BodyStatement accept(Transformer tf) {
		return tf.transform(this);
	}

}
