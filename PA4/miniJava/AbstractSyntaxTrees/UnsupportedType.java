package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class UnsupportedType extends Type {

	public UnsupportedType(TypeKind typ, SourcePosition posn) {
		super(typ, posn);
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitUnsupportedType(this, o);
	}
}
