package net.sandius.rembulan.parser.ast;

import net.sandius.rembulan.util.Check;

public class BinaryOperationExpr extends Expr {

	private final Operator.Binary op;
	private final Expr left;
	private final Expr right;

	public BinaryOperationExpr(SourceInfo src, Attributes attr, Operator.Binary op, Expr left, Expr right) {
		super(src, attr);
		this.op = Check.notNull(op);
		this.left = Check.notNull(left);
		this.right = Check.notNull(right);
	}

	public BinaryOperationExpr(SourceInfo src, Operator.Binary op, Expr left, Expr right) {
		this(src, Attributes.empty(), op, left, right);
	}

	public Operator.Binary op() {
		return op;
	}

	public Expr left() {
		return left;
	}

	public Expr right() {
		return right;
	}

	public BinaryOperationExpr update(Expr left, Expr right) {
		if (this.left.equals(left) && this.right.equals(right)) {
			return this;
		}
		else {
			return new BinaryOperationExpr(sourceInfo(), attributes(), op, left, right);
		}
	}

	@Override
	public Expr accept(Transformer tf) {
		return tf.transform(this);
	}

}
