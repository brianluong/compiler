package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.LocalDecl;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.VarDecl;

public class IdentificationTable {
		HashMap<String, Declaration> decls;
		HashMap<String, Declaration> fieldDecls;
		HashMap<String, Declaration> varDecls;
		int scopeLevel;
		
		public IdentificationTable(int scopeLevel) {
			this.scopeLevel = scopeLevel;
			decls = new HashMap<String, Declaration>();
			fieldDecls = new HashMap<String, Declaration>();
			varDecls = new HashMap<String, Declaration>();
		}
		
		public void add(Declaration decl) {
			if (!decls.containsKey(decl.name)) {
				decls.put(decl.name, decl);
			}
			if (decl instanceof FieldDecl) {
				fieldDecls.put(decl.name, decl);
			} else if (decl instanceof VarDecl ||
					decl instanceof ParameterDecl) {
				varDecls.put(decl.name, (LocalDecl) decl);
			}
		}
		
		public boolean hasDecl(String name) {
			return decls.containsKey(name);
		}

		public boolean hasFieldDecl(String name) {
			return fieldDecls.containsKey(name);
		}
	
		public boolean hasLocalVarDecl(String name) {
			return varDecls.containsKey(name);
		}
		
		public Declaration retrieve(String name) {
			return decls.get(name);
		}
		
		public FieldDecl retrieveField(String name) {
			return (FieldDecl) fieldDecls.get(name);
		}
		
		public LocalDecl retrieveLocalVar(String name) {
			return (LocalDecl) varDecls.get(name);
		}
}