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

import dev.foxgirl.rembulan.Variable;
import dev.foxgirl.rembulan.compiler.CompilerSettings;
import dev.foxgirl.rembulan.compiler.FunctionId;
import dev.foxgirl.rembulan.compiler.IRFunc;
import dev.foxgirl.rembulan.compiler.analysis.DependencyInfo;
import dev.foxgirl.rembulan.compiler.analysis.SlotAllocInfo;
import dev.foxgirl.rembulan.compiler.analysis.TypeInfo;
import dev.foxgirl.rembulan.compiler.gen.BytecodeEmitter;
import dev.foxgirl.rembulan.compiler.gen.ClassNameTranslator;
import dev.foxgirl.rembulan.compiler.gen.CompiledClass;
import dev.foxgirl.rembulan.compiler.gen.asm.helpers.ASMUtils;
import dev.foxgirl.rembulan.compiler.gen.asm.helpers.InvokableMethods;
import dev.foxgirl.rembulan.compiler.gen.asm.helpers.InvokeKind;
import dev.foxgirl.rembulan.compiler.ir.AbstractVar;
import dev.foxgirl.rembulan.compiler.ir.UpVar;
import dev.foxgirl.rembulan.compiler.ir.Var;
import dev.foxgirl.rembulan.impl.DefaultSavedState;
import dev.foxgirl.rembulan.util.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class ASMBytecodeEmitter extends BytecodeEmitter {

	public final IRFunc fn;
	public final SlotAllocInfo slots;
	public final TypeInfo types;
	public final DependencyInfo deps;

	public final CompilerSettings compilerSettings;
	public final ClassNameTranslator classNameTranslator;

	private final String sourceFile;

	private final ClassNode classNode;

	private final HashMap<UpVar, String> upvalueFieldNames;

	private final List<FieldNode> fields;

	private boolean verifyAndPrint;

	public ASMBytecodeEmitter(
			IRFunc fn,
			SlotAllocInfo slots,
			TypeInfo types,
			DependencyInfo deps,
			CompilerSettings compilerSettings,
			ClassNameTranslator classNameTranslator,
			String sourceFile) {

		this.fn = Objects.requireNonNull(fn);
		this.slots = Objects.requireNonNull(slots);
		this.types = Objects.requireNonNull(types);
		this.deps = Objects.requireNonNull(deps);

		this.compilerSettings = Objects.requireNonNull(compilerSettings);
		this.classNameTranslator = Objects.requireNonNull(classNameTranslator);
		this.sourceFile = Objects.requireNonNull(sourceFile);

		classNode = new ClassNode();

		this.fields = new ArrayList<>();

		upvalueFieldNames = new HashMap<>();

		String s = System.getProperty("dev.foxgirl.rembulan.compiler.VerifyAndPrint");
		verifyAndPrint = s != null && "true".equals(s.trim().toLowerCase());
	}

	int kind() {
		return InvokeKind.adjust_nativeKind(InvokeKind.encode(fn.params().size(), fn.isVararg()));
	}

	String thisClassName() {
		return fn.id().toClassName(classNameTranslator);
	}

	Type thisClassType() {
		return ASMUtils.typeForClassName(thisClassName());
	}

	Type superClassType() {
		return Type.getType(InvokeKind.nativeClassForKind(kind()));
	}

	Type parentClassType() {
		FunctionId parentId = fn.id().parent();
		return parentId != null
				? ASMUtils.typeForClassName(parentId.toClassName(classNameTranslator))
				: null;
	}

	public Type savedStateClassType() {
		return Type.getType(DefaultSavedState.class);
	}

	Type invokeMethodType() {
		return InvokableMethods.invoke_method(kind()).getMethodType();
	}

	public boolean hasUpvalues() {
		return !fn.upvals().isEmpty();
	}

	public int numOfParameters() {
		return fn.params().size();
	}

	public boolean isVararg() {
		return fn.isVararg();
	}

	public List<FieldNode> fields() {
		return fields;
	}

	private void addInnerClassLinks() {
		String ownInternalName = thisClassType().getInternalName();

		// parent
		if (parentClassType() != null) {
			String parentInternalName = parentClassType().getInternalName();

			// assume (parentInternalName + "$") is the prefix of ownInternalName
			String suffix = ownInternalName.substring(parentInternalName.length() + 1);

			classNode.innerClasses.add(new InnerClassNode(
					ownInternalName,
					parentInternalName,
					suffix,
					ACC_PUBLIC + ACC_STATIC));
		}

		List<FunctionId> nestedIds = new ArrayList<>(deps.nestedRefs());
		Collections.sort(nestedIds, FunctionId.LEXICOGRAPHIC_COMPARATOR);

		for (FunctionId childId : nestedIds) {
			String childClassName = childId.toClassName(classNameTranslator);
			String childInternalName = ASMUtils.typeForClassName(childClassName).getInternalName();

			// assume (ownInternalName + "$") is the prefix of childName
			String suffix = childInternalName.substring(ownInternalName.length() + 1);

			classNode.innerClasses.add(new InnerClassNode(
					childInternalName,
					ownInternalName,
					suffix,
					ACC_PUBLIC + ACC_STATIC));
		}
	}

	enum NestedInstanceKind {
		Pure,
		Closed,
		Open
	}

	protected static NestedInstanceKind functionKind(IRFunc fn) {
		if (fn.upvals().isEmpty()) {
			return NestedInstanceKind.Pure;
		}
		else {
			for (AbstractVar uv : fn.upvals()) {
				if (uv instanceof Var) {
					return NestedInstanceKind.Open;
				}
			}
			return NestedInstanceKind.Closed;
		}
	}

	public static String instanceFieldName() {
		return "INSTANCE";
	}

	private FieldNode instanceField() {
		return new FieldNode(
				ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
				instanceFieldName(),
				thisClassType().getDescriptor(),
				null,
				null);
	}

	String addFieldName(String n) {
		// TODO
		return n;
	}

	private static String toFieldName(String n) {
		return n;  // TODO
	}

	private static String ensureUnique(Collection<String> ss, String s) {
		int idx = 0;
		String prefix = s;

		while (ss.contains(s)) {
			s = prefix + "_" + (idx++);
		}

		return s;
	}

	private static String preferredUpvalueName(UpVar uv) {
		return uv.name().value();
	}

	private void addUpvalueFields() {
		for (UpVar uv : fn.upvals()) {
			String name = toFieldName(ensureUnique(upvalueFieldNames.values(), preferredUpvalueName(uv)));
			upvalueFieldNames.put(uv, name);

			FieldNode fieldNode = new FieldNode(
					ACC_PROTECTED + ACC_FINAL,
					name,
					Type.getDescriptor(Variable.class),
					null,
					null);

			classNode.fields.add(fieldNode);
		}
	}

	public String getUpvalueFieldName(UpVar uv) {
		String name = upvalueFieldNames.get(uv);
		if (name == null) {
			throw new IllegalArgumentException("upvalue field name is null for upvalue " + uv);
		}
		return name;
	}

	public ClassNode classNode() {
		classNode.version = V1_7;
		classNode.access = ACC_PUBLIC + ACC_SUPER;
		classNode.name = thisClassType().getInternalName();
		classNode.superName = superClassType().getInternalName();
		classNode.sourceFile = sourceFile;

		addInnerClassLinks();

		if (!hasUpvalues()) {
			classNode.fields.add(instanceField());
		}

		addUpvalueFields();

		RunMethod runMethod = new RunMethod(this);

		for (RunMethod.ConstFieldInstance cfi : runMethod.constFields()) {
			classNode.fields.add(cfi.fieldNode());
		}

		ConstructorMethod ctor = new ConstructorMethod(this, runMethod);

		classNode.methods.add(ctor.methodNode());
		classNode.methods.add(new InvokeMethod(this, runMethod).methodNode());
		classNode.methods.add(new ResumeMethod(this, runMethod).methodNode());
		classNode.methods.addAll(runMethod.methodNodes());

		if (runMethod.usesSnapshotMethod()) {
			classNode.methods.add(runMethod.snapshotMethodNode());
		}

		StaticConstructorMethod staticCtor = new StaticConstructorMethod(this, ctor, runMethod);
		if (!staticCtor.isEmpty()) {
			classNode.methods.add(staticCtor.methodNode());
		}

		classNode.fields.addAll(fields);

		return classNode;
	}

	private byte[] classNodeToBytes(ClassNode classNode) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);
		byte[] bytes = writer.toByteArray();

		// verify bytecode

		if (verifyAndPrint) {
			ClassReader reader = new ClassReader(bytes);
			ClassVisitor tracer = new TraceClassVisitor(new PrintWriter(System.out));
			ClassVisitor checker = new CheckClassAdapter(tracer, true);
			reader.accept(checker, 0);
		}

		return bytes;
	}

	@Override
	public CompiledClass emit() {
		ClassNode classNode = classNode();
		byte[] bytes = classNodeToBytes(classNode);
		return new CompiledClass(thisClassName(), ByteVector.wrap(bytes));
	}

}
