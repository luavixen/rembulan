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

import dev.foxgirl.rembulan.compiler.IRFunc;
import dev.foxgirl.rembulan.compiler.ir.*;

import java.util.*;

public abstract class CodeSimplifier {

	private CodeSimplifier() {
		// not to be instantiated or extended
	}

	private static boolean visit(Map<Label, Integer> uses, Label l) {
		Integer n = uses.get(l);
		if (n != null) {
			uses.put(l, n + 1);
			return false;
		}
		else {
			uses.put(l, 1);
			return true;
		}
	}

	private static Map<Label, Integer> uses(Code code) {
		Map<Label, Integer> uses = new HashMap<>();
		Deque<Label> open = new ArrayDeque<>();
		open.add(code.entryLabel());

		while (!open.isEmpty()) {
			Label l = open.pop();
			if (visit(uses, l)) {
				BasicBlock b = code.block(l);
				for (Label n : b.end().nextLabels()) {
					open.add(n);
				}
			}
		}

		return uses;
	}

	static Code pruneUnreachableCode(Code code) {
		Objects.requireNonNull(code);

		Set<Label> reachable = uses(code).keySet();

		List<BasicBlock> result = new ArrayList<>();
		Iterator<BasicBlock> it = code.blockIterator();
		while (it.hasNext()) {
			BasicBlock b = it.next();
			if (reachable.contains(b.label())) {
				result.add(b);
			}
		}

		return Code.of(result);
	}

	public static IRFunc pruneUnreachableCode(IRFunc fn) {
		return fn.update(pruneUnreachableCode(fn.code()));
	}

	private static BasicBlock merge(BasicBlock a, BasicBlock b) {
		Objects.requireNonNull(a);
		Objects.requireNonNull(b);

		if (a.end() instanceof ToNext) {
			List<BodyNode> body = new ArrayList<>();
			body.addAll(a.body());
			body.addAll(b.body());
			return new BasicBlock(a.label(), body, b.end());
		}
		else {
			return null;
		}
	}

	private static <T> T nextOrNull(Iterator<T> it) {
		return it.hasNext() ? it.next() : null;
	}

	static Code mergeBlocks(Code code) {
		Objects.requireNonNull(code);

		Map<Label, Integer> uses = uses(code);
		List<BasicBlock> result = new ArrayList<>();

		Iterator<BasicBlock> it = code.blockIterator();

		BasicBlock a = it.next();  // must be non-null
		BasicBlock b = nextOrNull(it);

		while (b != null) {
			if (uses.get(b.label()) < 2) {
				BasicBlock ab = merge(a, b);
				if (ab != null) {
					a = ab;
				}
				else {
					result.add(a);
					a = b;
				}
			}
			else {
				result.add(a);
				a = b;
			}
			b = nextOrNull(it);
		}

		assert (a != null);
		assert (b == null);

		result.add(a);

		return Code.of(result);
	}

	public static IRFunc mergeBlocks(IRFunc fn) {
		return fn.update(mergeBlocks(fn.code()));
	}

}
