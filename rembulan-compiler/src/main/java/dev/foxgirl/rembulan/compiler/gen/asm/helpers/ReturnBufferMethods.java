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

import dev.foxgirl.rembulan.runtime.ReturnBuffer;
import dev.foxgirl.rembulan.util.Check;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;

public class ReturnBufferMethods {

	private ReturnBufferMethods() {
		// not to be instantiated
	}

	private static Type selfTpe() {
		return Type.getType(ReturnBuffer.class);
	}

	public static AbstractInsnNode size() {
		return new MethodInsnNode(
				INVOKEINTERFACE,
				selfTpe().getInternalName(),
				"size",
				Type.getMethodType(
						Type.INT_TYPE).getDescriptor(),
				true);
	}

	public static AbstractInsnNode get() {
		return new MethodInsnNode(
				INVOKEINTERFACE,
				selfTpe().getInternalName(),
				"get",
				Type.getMethodType(
						Type.getType(Object.class),
						Type.INT_TYPE).getDescriptor(),
				true);
	}

	public static InsnList get(int index) {
		Check.nonNegative(index);

		InsnList il = new InsnList();

		if (index <= 4) {
			String methodName = "get" + index;
			il.add(new MethodInsnNode(
					INVOKEINTERFACE,
					selfTpe().getInternalName(),
					methodName,
					Type.getMethodType(
							Type.getType(Object.class)).getDescriptor(),
					true));
		}
		else {
			il.add(ASMUtils.loadInt(index));
			il.add(get());
		}

		return il;
	}

	public final static int MAX_SETTO_KIND;
	static {
		int k = 1;
		while (setTo_method(k).exists()) k += 1;
		MAX_SETTO_KIND = k - 1;
	}

	public final static int MAX_TAILCALL_KIND;
	static {
		int k = 1;
		while (tailCall_method(k).exists()) k += 1;
		MAX_TAILCALL_KIND = k - 1;
	}

	private static ReflectionUtils.Method setTo_method(int kind) {
		String methodName = kind > 0 ? "setTo" : "setToContentsOf";
		return ReflectionUtils.virtualArgListMethodFromKind(ReturnBuffer.class, methodName, null, kind);
	}

	private static ReflectionUtils.Method tailCall_method(int kind) {
		String methodName = kind >0 ? "setToCall" : "setToCallWithContentsOf";
		return ReflectionUtils.virtualArgListMethodFromKind(ReturnBuffer.class, methodName, new Class[] { Object.class }, kind);
	}

	public static AbstractInsnNode setTo(int kind) {
		return setTo_method(kind).toMethodInsnNode();
	}

	public static AbstractInsnNode tailCall(int kind) {
		return tailCall_method(kind).toMethodInsnNode();
	}

	public static AbstractInsnNode toArray() {
		return new MethodInsnNode(
				INVOKEINTERFACE,
				selfTpe().getInternalName(),
				"getAsArray",
				Type.getMethodType(
						ASMUtils.arrayTypeFor(Object.class)).getDescriptor(),
				true);
	}

}
