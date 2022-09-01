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

public interface Operator {

	int precedence();

	enum Binary implements Operator {

		ADD    (8, true),
		SUB    (8, true),
		MUL    (9, true),
		DIV    (9, true),
		IDIV   (9, true),
		MOD    (9, true),
		POW   (11, false),

		CONCAT  (7, false),

		BAND  (5, true),
		BOR   (3, true),
		BXOR  (4, true),
		SHL   (6, true),
		SHR   (6, true),

		EQ   (2, true),
		NEQ  (2, true),
		LT   (2, true),
		LE   (2, true),
		GT   (2, true),
		GE   (2, true),

		AND  (1, true),
		OR   (0, true);

		private final int precedence;
		private final boolean leftAssoc;

		Binary(int precedence, boolean leftAssoc) {
			this.precedence = precedence;
			this.leftAssoc = leftAssoc;
		}

		@Override
		public int precedence() {
			return precedence;
		}

		public boolean isLeftAssociative() {
			return leftAssoc;
		}

		public Binary swap() {
			switch (this) {
				case LT: return GT;
				case LE: return GE;
				case GT: return LT;
				case GE: return LE;
				default: return this;
			}
		}

	}

	enum Unary implements Operator {

		UNM  (10),
		BNOT (10),
		LEN  (10),
		NOT  (10);

		private final int precedence;

		Unary(int precedence) {
			this.precedence = precedence;
		}

		@Override
		public int precedence() {
			return precedence;
		}

	}

}
