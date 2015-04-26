package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class NullLiteral extends Terminal {

	public NullLiteral(Token t) {
		super(t);
		// TODO Auto-generated constructor stub
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		 return v.visitNullLiteral(this, o);
	}

}
