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

package dev.foxgirl.rembulan.compiler.gen.asm.helpers;

import dev.foxgirl.rembulan.runtime.ExecutionContext;
import dev.foxgirl.rembulan.runtime.LuaFunction;
import org.objectweb.asm.tree.AbstractInsnNode;

public class InvokableMethods {

	public static int adjustKind_invoke(int kind) {
		return kind > 0 ? (invoke_method(kind).exists() ? kind : 0) : 0;
	}

	public static ReflectionUtils.Method invoke_method(int kind) {
		return ReflectionUtils.virtualArgListMethodFromKind(
				LuaFunction.class, "invoke", new Class[] { ExecutionContext.class }, kind);
	}

	public static AbstractInsnNode invoke(int kind) {
		return invoke_method(kind).toMethodInsnNode();
	}

}
