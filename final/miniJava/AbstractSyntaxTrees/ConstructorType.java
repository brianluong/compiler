package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ConstructorType extends Type {

	public ConstructorType(Identifier i, SourcePosition posn) {
		super(TypeKind.CONSTRUCTOR, posn);
		className = i;
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		// TODO Auto-generated method stub
		return null;
	}

	Identifier className;
}
