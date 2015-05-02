package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ConstructorMethodDecl extends MethodDecl {

	public ConstructorMethodDecl(MemberDecl md, ParameterDeclList pl,
			StatementList sl, Expression e, SourcePosition posn) {
		super(md, pl, sl, e, posn);
	}
	
	public ConstructorMethodDecl(Identifier c, MemberDecl md, ParameterDeclList pl,
			StatementList sl, Expression e, SourcePosition posn) {
		super(md, pl, sl, e, posn);
		className = c;
		extractTypes();
	}
	
	public void addClassName(Identifier c) {
		className = c;
	}
	
	public Type[] extractTypes() {
		parameterTypes = new Type[parameterDeclList.size()];
		int counter = 0;
		for (ParameterDecl p : parameterDeclList) {
			parameterTypes[counter] = p.type;
			counter++;
		}
		return parameterTypes;
	}
	
	public Identifier className;
	public Type[] parameterTypes;

}
