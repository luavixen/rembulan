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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class UtilMethods {

	private UtilMethods() {
		// not to be instantiated
	}

	public static AbstractInsnNode StringBuilder_append(Type t) {
		return new MethodInsnNode(
				INVOKEVIRTUAL,
				Type.getInternalName(StringBuilder.class),
				"append",
				Type.getMethodDescriptor(
						Type.getType(StringBuilder.class),
						t),
				false);
	}

	public static AbstractInsnNode StringBuilder_toString() {
		return new MethodInsnNode(
				INVOKEVIRTUAL,
				Type.getInternalName(StringBuilder.class),
				"toString",
				Type.getMethodDescriptor(
						Type.getType(String.class)),
				false);
	}

	public static AbstractInsnNode String_compareTo() {
		return new MethodInsnNode(
				INVOKEVIRTUAL,
				Type.getInternalName(String.class),
				"compareTo",
				Type.getMethodDescriptor(
						Type.INT_TYPE,
						Type.getType(String.class)),
				false);
	}

}
