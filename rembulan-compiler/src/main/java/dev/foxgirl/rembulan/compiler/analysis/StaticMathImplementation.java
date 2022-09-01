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

package dev.foxgirl.rembulan.compiler.analysis;

import dev.foxgirl.rembulan.compiler.analysis.types.LuaTypes;
import dev.foxgirl.rembulan.compiler.analysis.types.Type;

public abstract class StaticMathImplementation {

	public static StaticMathImplementation MAY_BE_INTEGER = new MayBeInteger();

	public static StaticMathImplementation MUST_BE_FLOAT = new MustBeFloat();

	public static StaticMathImplementation MUST_BE_INTEGER = new MustBeInteger();

	public abstract NumericOperationType opType(Type left, Type right);

	public abstract NumericOperationType opType(Type arg);

	public static class MayBeInteger extends StaticMathImplementation {

		private MayBeInteger() {
			// not to be instantiated by the outside world
		}

		@Override
		public NumericOperationType opType(Type l, Type r) {
			if (l.isSubtypeOf(LuaTypes.NUMBER) && r.isSubtypeOf(LuaTypes.NUMBER)) {
				if (l.isSubtypeOf(LuaTypes.NUMBER_INTEGER) && r.isSubtypeOf(LuaTypes.NUMBER_INTEGER)) return NumericOperationType.Integer;
				else if (l.isSubtypeOf(LuaTypes.NUMBER_FLOAT) || r.isSubtypeOf(LuaTypes.NUMBER_FLOAT)) return NumericOperationType.Float;
				else return NumericOperationType.Number;
			}
			else {
				return NumericOperationType.Any;
			}
		}

		@Override
		public NumericOperationType opType(Type arg) {
			if (arg.isSubtypeOf(LuaTypes.NUMBER)) {
				if (arg.isSubtypeOf(LuaTypes.NUMBER_INTEGER)) return NumericOperationType.Integer;
				else if (arg.isSubtypeOf(LuaTypes.NUMBER_FLOAT)) return NumericOperationType.Float;
				else return NumericOperationType.Number;
			}
			else {
				return NumericOperationType.Any;
			}
		}

	}

	public static class MustBeFloat extends StaticMathImplementation {

		private MustBeFloat() {
			// not to be instantiated by the outside world
		}

		@Override
		public NumericOperationType opType(Type l, Type r) {
			if (l.isSubtypeOf(LuaTypes.NUMBER) && r.isSubtypeOf(LuaTypes.NUMBER)) return NumericOperationType.Float;
			else return NumericOperationType.Any;
		}

		@Override
		public NumericOperationType opType(Type arg) {
			if (arg.isSubtypeOf(LuaTypes.NUMBER)) return NumericOperationType.Float;
			else return NumericOperationType.Any;
		}

	}

	public static class MustBeInteger extends StaticMathImplementation {

		private MustBeInteger() {
			// not to be instantiated by the outside world
		}

		@Override
		public NumericOperationType opType(Type l, Type r) {
			if (l.isSubtypeOf(LuaTypes.NUMBER) && r.isSubtypeOf(LuaTypes.NUMBER)) return NumericOperationType.Integer;
			else return NumericOperationType.Any;
		}

		@Override
		public NumericOperationType opType(Type arg) {
			if (arg.isSubtypeOf(LuaTypes.NUMBER)) return NumericOperationType.Integer;
			else return NumericOperationType.Any;
		}

	}

}
