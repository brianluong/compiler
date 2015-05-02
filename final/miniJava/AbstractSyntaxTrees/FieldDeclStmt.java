package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class FieldDeclStmt extends FieldDecl{

	public FieldDeclStmt(boolean isPrivate, boolean isStatic, Type t, String name, Expression e, SourcePosition posn) {
		super(isPrivate, isStatic, t, name, posn);
		initExp = e;
	}
	
	// I don't think I use the other FieldDel constructor at all in the parser

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitFielddeclStmt(this, o);
	}
	
    public Expression initExp;
	
}
