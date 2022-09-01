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

import dev.foxgirl.rembulan.compiler.gen.asm.helpers.ASMUtils;
import dev.foxgirl.rembulan.impl.DefaultSavedState;
import dev.foxgirl.rembulan.impl.NonsuspendableFunctionException;
import dev.foxgirl.rembulan.runtime.ExecutionContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

class ResumeMethod {

	private final ASMBytecodeEmitter context;
	private final RunMethod runMethod;

	public ResumeMethod(ASMBytecodeEmitter context, RunMethod runMethod) {
		this.context = Objects.requireNonNull(context);
		this.runMethod = Objects.requireNonNull(runMethod);
	}

	public MethodNode methodNode() {
		MethodNode node = new MethodNode(
				ACC_PUBLIC,
				"resume",
				Type.getMethodType(
						Type.VOID_TYPE,
						Type.getType(ExecutionContext.class),
						Type.getType(Object.class)).getDescriptor(),
						null,
				runMethod.throwsExceptions());

		if (runMethod.isResumable()) {
			InsnList il = node.instructions;
			List<LocalVariableNode> locals = node.localVariables;

			LabelNode begin = new LabelNode();
			LabelNode vars = new LabelNode();
			LabelNode end = new LabelNode();

			il.add(begin);

			il.add(new VarInsnNode(ALOAD, 2));
			il.add(new TypeInsnNode(CHECKCAST, Type.getInternalName(DefaultSavedState.class)));

			il.add(vars);

			il.add(new VarInsnNode(ASTORE, 3));

			il.add(new VarInsnNode(ALOAD, 0));  // this
			il.add(new VarInsnNode(ALOAD, 1));  // context

			il.add(new VarInsnNode(ALOAD, 3));  // saved state
			il.add(new MethodInsnNode(
					INVOKEVIRTUAL,
					Type.getInternalName(DefaultSavedState.class),
					"resumptionPoint",
					Type.getMethodDescriptor(
							Type.INT_TYPE),
					false
			));  // resumption point

			// registers
			if (context.isVararg() || runMethod.numOfRegisters() > 0) {
				il.add(new VarInsnNode(ALOAD, 3));
				il.add(new MethodInsnNode(
						INVOKEVIRTUAL,
						Type.getInternalName(DefaultSavedState.class),
						"registers",
						Type.getMethodDescriptor(
								ASMUtils.arrayTypeFor(Object.class)),
						false
				));

				// varargs stored as the 0th element
				int numRegs = runMethod.numOfRegisters() + (context.isVararg() ? 1 : 0);

				for (int i = 0; i < numRegs; i++) {

					// Note: it might be more elegant to use a local variable
					// to store the array instead of having to perform SWAPs

					if (i + 1 < numRegs) {
						il.add(new InsnNode(DUP));
					}
					il.add(ASMUtils.loadInt(i));
					il.add(new InsnNode(AALOAD));
					if (i == 0 && context.isVararg()) {
						il.add(new TypeInsnNode(CHECKCAST, ASMUtils.arrayTypeFor(Object.class).getInternalName()));
					}

					if (i + 1 < numRegs) {
						il.add(new InsnNode(SWAP));
					}
				}
			}

			// call run(...)
			il.add(runMethod.methodInvokeInsn());

			il.add(new InsnNode(RETURN));
			il.add(end);

			locals.add(new LocalVariableNode("this", context.thisClassType().getDescriptor(), null, begin, end, 0));
			locals.add(new LocalVariableNode("context", Type.getDescriptor(ExecutionContext.class), null, begin, end, 1));
			locals.add(new LocalVariableNode("suspendedState", context.savedStateClassType().getDescriptor(), null, begin, end, 2));
			locals.add(new LocalVariableNode("ss", Type.getDescriptor(DefaultSavedState.class), null, vars, end, 3));

			// TODO: maxStack, maxLocals
			node.maxStack = 3 + (runMethod.numOfRegisters() > 0 ? 3: 0);
			node.maxLocals = 5;
		}
		else
		{
			InsnList il = node.instructions;
			List<LocalVariableNode> locals = node.localVariables;

			LabelNode begin = new LabelNode();
			LabelNode end = new LabelNode();

			il.add(begin);
			il.add(new TypeInsnNode(NEW, Type.getInternalName(NonsuspendableFunctionException.class)));
			il.add(new InsnNode(DUP));
			il.add(ASMUtils.ctor(NonsuspendableFunctionException.class));
			il.add(new InsnNode(ATHROW));
			il.add(end);

			locals.add(new LocalVariableNode("this", context.thisClassType().getDescriptor(), null, begin, end, 0));
			locals.add(new LocalVariableNode("context", Type.getDescriptor(ExecutionContext.class), null, begin, end, 1));
			locals.add(new LocalVariableNode("suspendedState", context.savedStateClassType().getDescriptor(), null, begin, end, 2));

			node.maxStack = 2;
			node.maxLocals = 3;
		}

		return node;
	}

}
