package miniJava;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.SyntacticAnalyzer.SourcePosition;


public class ErrorReporter {
	
	public void reportError(String prefix, String message, String tokenName, SourcePosition pos) {
		System.out.println (prefix + "ERROR: "
				+ message + " at line #: " + pos.start + " to line # " + pos.finish);
	}

	public void reportError(String prefix, String message, String tokenName,
			AST ast) {
		System.out.println (prefix + "ERROR: "
				 + message + " at line #: " + ast.posn.start + " to line # " + ast.posn.finish);
		
	}
}