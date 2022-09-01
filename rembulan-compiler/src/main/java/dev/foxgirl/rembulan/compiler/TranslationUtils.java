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

package dev.foxgirl.rembulan.compiler;

import dev.foxgirl.rembulan.compiler.ir.BinOp;
import dev.foxgirl.rembulan.compiler.ir.UnOp;
import dev.foxgirl.rembulan.parser.analysis.FunctionVarInfo;
import dev.foxgirl.rembulan.parser.analysis.ResolvedLabel;
import dev.foxgirl.rembulan.parser.analysis.ResolvedVariable;
import dev.foxgirl.rembulan.parser.analysis.VarMapping;
import dev.foxgirl.rembulan.parser.ast.*;
import dev.foxgirl.rembulan.parser.ast.util.AttributeUtils;

abstract class TranslationUtils {

	private TranslationUtils() {
		// not to be instantiated
	}

	public static BinOp.Op bop(Operator.Binary bop) {
		switch (bop) {
			case ADD:  return BinOp.Op.ADD;
			case SUB:  return BinOp.Op.SUB;
			case MUL:  return BinOp.Op.MUL;
			case DIV:  return BinOp.Op.DIV;
			case IDIV: return BinOp.Op.IDIV;
			case MOD:  return BinOp.Op.MOD;
			case POW:  return BinOp.Op.POW;

			case CONCAT: return BinOp.Op.CONCAT;

			case BAND:  return BinOp.Op.BAND;
			case BOR:   return BinOp.Op.BOR;
			case BXOR:  return BinOp.Op.BXOR;
			case SHL:   return BinOp.Op.SHL;
			case SHR:   return BinOp.Op.SHR;

			case EQ:  return BinOp.Op.EQ;
			case NEQ: return BinOp.Op.NEQ;
			case LT:  return BinOp.Op.LT;
			case LE:  return BinOp.Op.LE;

			default: return null;
		}
	}

	public static UnOp.Op uop(Operator.Unary uop) {
		switch (uop) {
			case UNM:  return UnOp.Op.UNM;
			case BNOT: return UnOp.Op.BNOT;
			case LEN:  return UnOp.Op.LEN;
			case NOT:  return UnOp.Op.NOT;

			default:  return null;
		}
	}

	public static ResolvedVariable resolved(VarExpr e) {
		ResolvedVariable rv = e.attributes().get(ResolvedVariable.class);
		if (rv == null) {
			throw new IllegalStateException("Unresolved variable '" + e.name().value() + "' at " + AttributeUtils.sourceInfoString(e));
		}
		return rv;
	}

	public static ResolvedLabel resolvedLabel(LabelStatement e) {
		ResolvedLabel rl = e.attributes().get(ResolvedLabel.class);
		if (rl == null) {
			throw new IllegalStateException("Unresolved label '" + e.labelName().value() + "' at " + AttributeUtils.sourceInfoString(e));
		}
		return rl;
	}

	public static ResolvedLabel resolvedLabel(GotoStatement e) {
		ResolvedLabel rl = e.attributes().get(ResolvedLabel.class);
		if (rl == null) {
			throw new IllegalStateException("Unresolved goto '" + e.labelName().value() + "' at " + AttributeUtils.sourceInfoString(e));
		}
		return rl;
	}

	public static FunctionVarInfo funcVarInfo(SyntaxElement e) {
		FunctionVarInfo info = e.attributes().get(FunctionVarInfo.class);
		if (info == null) {
			throw new IllegalStateException("No var info at " + AttributeUtils.sourceInfoString(e));
		}
		return info;
	}

	public static FunctionVarInfo funcVarInfo(Chunk c) {
		FunctionVarInfo info = c.attributes().get(FunctionVarInfo.class);
		if (info == null) {
			throw new IllegalStateException("No var info in chunk");
		}
		return info;
	}

	public static VarMapping varMapping(SyntaxElement e) {
		VarMapping vm = e.attributes().get(VarMapping.class);
		if (vm == null) {
			throw new IllegalStateException("No var mapping at " + AttributeUtils.sourceInfoString(e));
		}
		return vm;
	}

}
