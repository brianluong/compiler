package miniJava.ContextualAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.ConstructorMethodDecl;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclStmt;
import miniJava.AbstractSyntaxTrees.LocalDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.Type;
import miniJava.AbstractSyntaxTrees.VarDecl;

public class ClassTable {
		public HashMap<String, Declaration> fieldDeclarations;
		public HashMap<String, Declaration> privateFieldDeclarations;
		public HashMap<String, Declaration> staticFieldDeclarations;
		
		public HashMap<String, Declaration> methodDeclarations;
		public HashMap<String, Declaration> privateMethodDeclarations;
		public HashMap<String, Declaration> staticMethodDeclarations;
		public HashMap<Type[], ConstructorMethodDecl> constructorDeclarations;
		
		public ArrayList<IdentificationTable> scopedTables;
		public IdentificationTable currentTable;
		public int currentScope;
		public int currentTableIndex;
		public String name;
		
		/*	SCOPE LEVELS
		 *  1. class names
			2. member names within a class
			3. parameters names within a method  
			4+ local variable names in successively nested scopes within a method.	
		 */
		public ClassTable(String name) {
			this.name = name;
			scopedTables = new ArrayList<IdentificationTable>();
			currentScope = 2; // starting scope of a class
			currentTable = new IdentificationTable(2);
			currentTableIndex = 0;
			scopedTables.add(currentTable);
			
			fieldDeclarations = new HashMap<String, Declaration>();
			privateFieldDeclarations = new HashMap<String, Declaration>();
			staticFieldDeclarations = new HashMap<String, Declaration>();
			
			methodDeclarations = new HashMap<String, Declaration>();
			privateMethodDeclarations = new HashMap<String, Declaration>();
			staticMethodDeclarations = new HashMap<String, Declaration>();
			constructorDeclarations = new HashMap<Type[], ConstructorMethodDecl>();
		}
		
		public boolean add(Declaration d) {
			boolean success = true;
			if (d instanceof FieldDecl || d instanceof FieldDeclStmt) {
				success = addField(d);
			}
			if (d instanceof ConstructorMethodDecl) {
				success = addConstructorMethod(d);
			} else if (d instanceof MethodDecl) {
				success = addMethod(d);
			}
			if (d instanceof ParameterDecl) {
				success = addParamDecl(d);
			}
			if (d instanceof VarDecl) {
				success = addVarDecl(d);
			}
			return success;
		}

		private boolean addConstructorMethod(Declaration d) {
			boolean success = true;
			if (hasConstructor((ConstructorMethodDecl) d)) {
				success = false;
			} else {
				constructorDeclarations.put(((ConstructorMethodDecl) d).parameterTypes, (ConstructorMethodDecl) d);
			}
			return success;
		}
		
		private boolean hasConstructor(ConstructorMethodDecl d) {
			Type[] typeArgs = d.parameterTypes;
			for (Type[] t : constructorDeclarations.keySet()) {
				ConstructorMethodDecl c = constructorDeclarations.get(t);
				Type[] typeArgs2 = c.parameterTypes;
				boolean equals = true;
				if (typeArgs.length == typeArgs2.length) {
					for (int i = 0; i < t.length; i++) {
						if (typeArgs[i].typeKind != typeArgs2[i].typeKind) {
							equals = false;
						}
					}
				} else {
					equals = false;
				}
				if (equals) {
					return true;
				}
			}
			return false;
		}

		public ConstructorMethodDecl matchConstructor(Type[] typeArgs) {
			for (Type[] t : constructorDeclarations.keySet()) {
				ConstructorMethodDecl c = constructorDeclarations.get(t);
				Type[] typeArgs2 = c.parameterTypes;
				boolean equals = true;
				if (typeArgs.length == typeArgs2.length) {
					for (int i = 0; i < t.length; i++) {
						if (typeArgs[i].typeKind != typeArgs2[i].typeKind) {
							equals = false;
						}
					}
				} else {
					equals = false;
				}
				if (equals) {
					return c;
				}
			}
			return null;
		}
			
		private boolean addMethod(Declaration d) {
			boolean success = true;
			if (!methodDeclarations.containsKey(d.name)) {
				if (((MethodDecl) d).isPrivate) {
					privateMethodDeclarations.put(d.name, d);
				}
				if (((MethodDecl) d).isStatic) {
					staticMethodDeclarations.put(d.name, d);
				}
				methodDeclarations.put(d.name, d);
			} else {
				success = false;
			}
			return success;
		}

		private boolean addField(Declaration d) {
			boolean success = true;
			if (!fieldDeclarations.containsKey(d.name)) {
				if (((FieldDecl) d).isPrivate) {
					privateFieldDeclarations.put(d.name, d);
				}
				if (((FieldDecl) d).isStatic) {
					staticFieldDeclarations.put(d.name, d);
				}
				fieldDeclarations.put(d.name, d);
				currentTable.add(d);
			} else {
				success = false;
			}
			return success;
		}
		
		private boolean addVarDecl(Declaration d) {
			boolean success = true;
			String name = d.name;
			// check from closest to farthest scope
			for (int index = currentTableIndex; index > 0; index--) {
				IdentificationTable currentTable = scopedTables.get(index);
				if (currentTable.hasDecl(name)) {
					success = false;
				}
			}
			if (success) {
				currentTable.add(d);
			}
			return success;
		}
		
		private boolean addParamDecl(Declaration d) {
			boolean success = true;
			String name = d.name;
			if (!currentTable.varDecls.containsKey(name)) {
				currentTable.add(d);
			} else {
				success = false;
			}
			return success;
		}
		
		public void openScope() {
			currentScope++;
			IdentificationTable newTable = new IdentificationTable(currentScope);
			currentTable = newTable;
			scopedTables.add(currentTable);
			currentTableIndex++;
		}
		
		public void closeScope(){
			currentScope--;
			scopedTables.remove(currentTableIndex);
			currentTableIndex--;
			currentTable = scopedTables.get(currentTableIndex);
		}
		
		public boolean containsField(String name) {
			return fieldDeclarations.containsKey(name);
		}
		
		public boolean containsMethod(String name) {
			return methodDeclarations.containsKey(name);
		}
		
		//public boolean containsConstructorMethod
		
		/*
		 * very general retrieve method
		 * doesn't take into account current class, or scope
		 * should not really be used
		 */
		public Declaration retrieve(String name) {
			// retrieve local things first
			Declaration decl = null;
			for (int index = currentTableIndex; index >= 0; index--) {
				IdentificationTable currentTable = scopedTables.get(index);
				if (currentTable.hasDecl(name)) {
					decl = currentTable.retrieve(name);
					break;
				}
			}
			return decl;
		}
		
		public FieldDecl retrieveField(String name) {
			return (FieldDecl) fieldDeclarations.get(name);
		}
		
		public FieldDecl retrieveStaticField(String name) {
			return (FieldDecl) staticFieldDeclarations.get(name);
		}
		
		public LocalDecl retrieveLocalVar(String name) {
			Declaration decl = null;
			// check from closest to farthest scope
			for (int index = currentTableIndex; index >= 0; index--) {
				IdentificationTable currentTable = scopedTables.get(index);
				if (currentTable.hasLocalVarDecl(name)) {
					decl = currentTable.retrieveLocalVar(name);
					break;
				}
			}
			return (LocalDecl) decl;
		}
		
		public MethodDecl retrieveMethod(String name) {
			return (MethodDecl) methodDeclarations.get(name);
		}
		
		public MethodDecl retrieveStaticMethod(String name) {
			return (MethodDecl) staticMethodDeclarations.get(name);
		}
}