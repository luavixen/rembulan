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

package dev.foxgirl.rembulan.compiler.tf;

import dev.foxgirl.rembulan.compiler.ir.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class CodeTransformerVisitor extends CodeVisitor {

	private final List<BasicBlock> basicBlocks;

	private Label label;
	private List<BodyNode> body;
	private BlockTermNode end;

	public CodeTransformerVisitor(IRVisitor visitor) {
		super(visitor);
		this.basicBlocks = new ArrayList<>();
	}

	public CodeTransformerVisitor() {
		this(null);
	}

	public Code result() {
		return Code.of(basicBlocks);
	}

	@Override
	public void visit(Code code) {
		basicBlocks.clear();
		super.visit(code);
	}

	@Override
	public void visit(BasicBlock block) {
		label = block.label();
		body = new ArrayList<>(block.body());
		end = block.end();

		BasicBlock bb = block;
		try {
			preVisit(block);
			super.visit(block);
			postVisit(block);
			bb = new BasicBlock(label, Collections.unmodifiableList(body), end);
		}
		finally {
			label = null;
			body = null;
			end = null;
		}

		basicBlocks.add(block.equals(bb) ? block : bb);
	}

	protected Label currentLabel() {
		return label;
	}

	protected void setLabel(Label l) {
		Objects.requireNonNull(l);
		label = l;
	}

	protected List<BodyNode> currentBody() {
		return body;
	}

	protected BlockTermNode currentEnd() {
		return end;
	}

	protected void setEnd(BlockTermNode node) {
		Objects.requireNonNull(node);
		end = node;
	}

	protected void preVisit(BasicBlock block) {

	}

	protected void postVisit(BasicBlock block) {

	}

}
