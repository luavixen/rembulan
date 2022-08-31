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

package dev.foxgirl.rembulan.compiler.gen.asm;

import org.objectweb.asm.tree.*;

import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

class StaticConstructorMethod {

	private final ASMBytecodeEmitter context;
	private final ConstructorMethod ctorMethod;
	private final RunMethod runMethod;

	public StaticConstructorMethod(ASMBytecodeEmitter context, ConstructorMethod ctorMethod, RunMethod runMethod) {
		this.context = Objects.requireNonNull(context);
		this.ctorMethod = Objects.requireNonNull(ctorMethod);
		this.runMethod = Objects.requireNonNull(runMethod);
	}

	public boolean isEmpty() {
		return context.hasUpvalues() && runMethod.constFields().isEmpty();
	}

	public MethodNode methodNode() {

		MethodNode node = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);

		InsnList il = node.instructions;

		LabelNode begin = new LabelNode();
		LabelNode end = new LabelNode();

		il.add(begin);

		if (!context.hasUpvalues()) {
			il.add(new TypeInsnNode(NEW, context.thisClassType().getInternalName()));
			il.add(new InsnNode(DUP));

			il.add(new MethodInsnNode(
					INVOKESPECIAL,
					context.thisClassType().getInternalName(),
					"<init>",
					ctorMethod.methodType().getDescriptor(),
					false));

			il.add(new FieldInsnNode(
					PUTSTATIC,
					context.thisClassType().getInternalName(),
					context.instanceFieldName(),
					context.thisClassType().getDescriptor()));
		}

		if (!runMethod.constFields().isEmpty()) {
			for (RunMethod.ConstFieldInstance cfi : runMethod.constFields()) {
				il.add(cfi.instantiateInsns());
			}
		}

		il.add(new InsnNode(RETURN));
		il.add(end);

		return node;
	}

}
