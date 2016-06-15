package net.sandius.rembulan.parser.ast;

import net.sandius.rembulan.util.Check;

import java.util.List;
import java.util.Objects;

public class TableConstructorExpr extends Expr {

	private final List<FieldInitialiser> fields;

	public TableConstructorExpr(SourceInfo src, Attributes attr, List<FieldInitialiser> fields) {
		super(src, attr);
		this.fields = Check.notNull(fields);
	}

	public TableConstructorExpr(SourceInfo src, List<FieldInitialiser> fields) {
		this(src, Attributes.empty(), fields);
	}

	public static class FieldInitialiser {

		private final Expr keyExpr;  // may be null
		private final Expr valueExpr;

		public FieldInitialiser(Expr keyExpr, Expr valueExpr) {
			this.keyExpr = keyExpr;
			this.valueExpr = Check.notNull(valueExpr);
		}

		public Expr key() {
			return keyExpr;
		}

		public Expr value() {
			return valueExpr;
		}

		public FieldInitialiser update(Expr keyExpr, Expr valueExpr) {
			if (Objects.equals(this.keyExpr, keyExpr) && this.valueExpr.equals(valueExpr)) {
				return this;
			}
			else {
				return new FieldInitialiser(keyExpr, valueExpr);
			}
		}

	}

	public List<FieldInitialiser> fields() {
		return fields;
	}

	public TableConstructorExpr update(List<FieldInitialiser> fields) {
		if (this.fields.equals(fields)) {
			return this;
		}
		else {
			return new TableConstructorExpr(sourceInfo(), attributes(), fields);
		}
	}

	@Override
	public Expr accept(Transformer tf) {
		return tf.transform(this);
	}

}
