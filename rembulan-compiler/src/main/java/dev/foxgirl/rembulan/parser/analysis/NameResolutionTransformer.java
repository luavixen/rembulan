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

package dev.foxgirl.rembulan.parser.analysis;

import dev.foxgirl.rembulan.parser.ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class NameResolutionTransformer extends Transformer {

	private FunctionVarInfoBuilder fnScope;

	public NameResolutionTransformer(FunctionVarInfoBuilder fnScope) {
		this.fnScope = fnScope;
	}

	public NameResolutionTransformer() {
		this(null);
	}

	protected void enterFunction() {
		fnScope = new FunctionVarInfoBuilder(fnScope);
	}

	protected FunctionVarInfo leaveFunction() {
		FunctionVarInfoBuilder scope = fnScope;
		fnScope = scope.parent();
		return scope.toVarInfo();
	}

	@Override
	public Chunk transform(Chunk chunk) {
		enterFunction();
		Chunk c = chunk.update(transform(chunk.block()));
		FunctionVarInfo varInfo = leaveFunction();
		return c.with(varInfo);
	}

	@Override
	public Block transform(Block block) {
		fnScope.enterBlock();
		Block b = super.transform(block);
		fnScope.leaveBlock();
		return b;
	}

	@Override
	public BodyStatement transform(LocalDeclStatement node) {
		List<Expr> resolvedInitialisers = transformExprList(node.initialisers());

		List<Name> ns = transformNameList(node.names());
		List<Variable> vs = new ArrayList<>();
		for (Name n : ns) {
			Variable v = fnScope.addLocal(n);
			vs.add(v);
		}
		return node
				.update(ns, resolvedInitialisers)
				.with(new VarMapping(Collections.unmodifiableList(vs)));
	}

	@Override
	public BodyStatement transform(NumericForStatement node) {
		Name n = transform(node.name());

		Expr init = node.init().accept(this);
		Expr limit = node.limit().accept(this);
		Expr step = node.step() != null ? node.step().accept(this) : null;

		fnScope.enterBlock();
		Variable v = fnScope.addLocal(n);
		node = node.update(n, init, limit, step, transform(node.block()));
		node = node.with(new VarMapping(v));
		fnScope.leaveBlock();

		return node;
	}

	@Override
	public BodyStatement transform(GenericForStatement node) {
		List<Name> ns = transformNameList(node.names());
		List<Expr> es = transformExprList(node.exprs());

		List<Variable> vs = new ArrayList<>();

		fnScope.enterBlock();
		for (Name n : ns) {
			Variable v = fnScope.addLocal(n);
			vs.add(v);
		}
		node = node.update(ns, es, transform(node.block()));
		node = node.with(new VarMapping(Collections.unmodifiableList(vs)));
		fnScope.leaveBlock();

		return node;
	}

	@Override
	public BodyStatement transform(RepeatUntilStatement node) {
		fnScope.enterBlock();
		Block b = super.transform(node.block());
		Expr c = node.condition().accept(this);
		node = node.update(c, b);
		fnScope.leaveBlock();

		return node;
	}


	@Override
	public FunctionDefExpr transform(FunctionDefExpr e) {
		enterFunction();

		fnScope.enterBlock();
		FunctionDefExpr.Params ps = transform(e.params());
		for (Name n : ps.names()) {
			fnScope.addParam(n);
		}

		e = e.update(ps, transform(e.block()));
		fnScope.leaveBlock();

		FunctionVarInfo varInfo = leaveFunction();
		e = e.with(varInfo);

		if (!ps.isVararg() && varInfo.isVararg()) {
			throw new IllegalStateException("cannot use '...' outside a vararg function");
		}

		return e;
	}

	@Override
	public LValueExpr transform(VarExpr e) {
		if (e.attributes().has(ResolvedVariable.class)) {
			throw new IllegalStateException("variable already resolved: " + e.name() + " -> " + e.attributes().get(ResolvedVariable.class));
		}

		ResolvedVariable bound = fnScope.resolve(e.name());
		if (bound != null) {
			return e.with(bound);
		}
		else {
			// not resolved: translate to _ENV[name]
			ResolvedVariable env = fnScope.resolve(Variable.ENV_NAME);

			assert (env != null);

			Attributes attr = e.attributes();
			return new IndexExpr(attr,
					new VarExpr(attr.with(env), Variable.ENV_NAME),
					new LiteralExpr(attr, StringLiteral.fromName(e.name())));
		}
	}

	@Override
	public Expr transform(VarargsExpr e) {
		fnScope.setVararg();
		return super.transform(e);
	}

}
