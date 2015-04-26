package miniJava.ContextualAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.CodeGenerator.ClassRuntimeEntity;
import miniJava.CodeGenerator.FieldRuntimeEntity;
import miniJava.CodeGenerator.MethodRuntimeEntity;
import miniJava.SyntacticAnalyzer.Token;

public class IdentificationTableManager {

	private static final String FIELD_SYSTEM = "out";
	private static final String CLASS_SYSTEM = "System";
	private static final String CLASS__PRINTSTREAM = "_PrintStream";
	private static  final String CLASS_STRING = "String";
	private static  final String CLASS_ARRAY = "Array";
	private static final String PARAM_PRINTLN = "n";
	private static final String METHOD_PRINTLN = "println";
	// each class has its own scoped ID table 
	HashMap<String, ClassTable> tables;
	HashMap<String, ClassDecl> classDeclarations;
	ClassTable currentClassTable;
	boolean hasMainMethod;
	MethodDecl mainMethod;
	
	public IdentificationTableManager() {
		tables = new HashMap<String, ClassTable>();
		currentClassTable = null;
		classDeclarations = new HashMap<String, ClassDecl>();
		addPredefinedClasses();
	}
	
	public boolean checkMainMethod() {
		int mainMethodCount = 0;
		for (ClassDecl cd : classDeclarations.values()) {
			MethodDeclList mdl = cd.methodDeclList;
			if (mdl != null) {
				for (MethodDecl md : mdl) {
					ParameterDeclList pdl = md.parameterDeclList;
					if (isMainMethod(md, pdl)) {
						mainMethodCount++;
					}
				}
			}
		}
		return mainMethodCount == 1;
	}

	private boolean isMainMethod(MethodDecl md, ParameterDeclList pdl) {
		if (hasOneParameter(pdl)) {
			ParameterDecl pd = pdl.get(0);
			if (hasMainMethodHeader(md) && hasMainMethodArgs(pd))  {
				return true;
			}
		}
		return false;
	}

	private boolean hasMainMethodArgs(ParameterDecl pd) {
		Type paramType = pd.type;
		if (paramType instanceof ArrayType) {
			Type typeOfArray = ((ArrayType) paramType).eltType;
			if (typeOfArray instanceof ClassType) {
				Identifier id = ((ClassType) typeOfArray).className;
				if (id.decl.name.equals("String")) {
					return true;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
		return false;
	}

	private boolean hasMainMethodHeader(MethodDecl md) {
		return md.name.equals("main") && (!md.isPrivate) 
				&& md.isStatic && md.type.typeKind == TypeKind.VOID;
	}

	private boolean hasOneParameter(ParameterDeclList pdl) {
		return pdl.size()  == 1 && pdl.get(0) != null;
	}
	
	
	private void addPredefinedClasses() {
		
		// CLASS _PRINTSTREAM
		MethodDecl md = new MethodDecl(
				new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), METHOD_PRINTLN, null), 
				new ParameterDeclList(), 
				null, 
				null, 
				null);
		md.parameterDeclList.add(new ParameterDecl(new BaseType(TypeKind.INT, null), PARAM_PRINTLN, null));
		// make runtime entity description for method
		md.red = new MethodRuntimeEntity(-2); // special code for predefined class
		md.red.addParam(1);
		
		MethodDeclList mdl = new MethodDeclList();
		FieldDeclList fdl = new FieldDeclList();
		mdl.add(md);
		ClassDecl printstreamClassDecl = new ClassDecl(CLASS__PRINTSTREAM, fdl, mdl, null);
		// make runtime entity descriptions for class _PrintStream
		printstreamClassDecl.red = new ClassRuntimeEntity(0); // no static or instance vars
		
		addNewTable(printstreamClassDecl);
		currentClassTable.add(md);
		
		
		// CLASS SYSTEM
		FieldDecl fd = new FieldDecl(false, true, new ClassType(new Identifier(
						new Token(0, CLASS__PRINTSTREAM, null)), null), FIELD_SYSTEM, null);
		fdl = new FieldDeclList();
		mdl = new MethodDeclList();
		fdl.add(fd);
		ClassDecl systemClassDecl = new ClassDecl(CLASS_SYSTEM, fdl, mdl, null);
		addNewTable(systemClassDecl);
		currentClassTable.add(fd);
		
		// CLASS STRING
		fdl = new FieldDeclList();
		mdl = new MethodDeclList();
		ClassDecl stringClassDecl = new ClassDecl(CLASS_STRING, null, null, null);
		addNewTable(stringClassDecl);
		
		// CLASS ARRAY
		fd = new FieldDecl(false, false, new BaseType(TypeKind.INT, null), "length", null);
		fd.red = new FieldRuntimeEntity(0);
		fdl = new FieldDeclList();
		mdl = new MethodDeclList();
		fdl.add(fd);
		ClassDecl arrayClassDecl = new ClassDecl(CLASS_ARRAY, fdl, mdl, null);
		addNewTable(arrayClassDecl);
		currentClassTable.add(fd);
		
		currentClassTable = null;
	}
	
	
	public boolean addNewTable(ClassDecl classDecl) {
		String className = classDecl.name;
		if (!classDeclarations.containsKey(className)) {
			ClassTable classTable = new ClassTable(classDecl.name);
			tables.put(className, classTable);
			classDeclarations.put(className, classDecl);
			currentClassTable = classTable;
			return true;
		} else {
			return false;
		}
	}	
	
	public Declaration getClassDeclaration(String name) {
		return classDeclarations.get(name);
		
	}
	public ClassTable getClassTable(String name) {
		return tables.get(name);
	}
	
	public void setCurrentClassTable(String name) {
		if (containsClass(name)) {
			currentClassTable = tables.get(name);
		}
	}
	
	public ClassTable getCurrentClassTable() {
		return currentClassTable;
	}
	
	public boolean containsClass(String name) {
		return classDeclarations.containsKey(name);
	}
	
	public Declaration retrieve(String name) {
		// check if it's a class declaration
		// need to check imports
		// need to check for statics (other classes)
		// Check for nearest scope first
		
		// look in current Class with closest scope first
		Declaration decl = currentClassTable.retrieve(name);
		if (decl != null) {
			return decl;
		// look tosee if it's a Class name 
		} else if (classDeclarations.containsKey(name)) {
			decl = classDeclarations.get(name);
		} else {
			// look for fields / method declarations
			for (String key : tables.keySet()) {
				ClassTable classTable = tables.get(key);
				decl = classTable.retrieve(name);
				if (decl != null) {
					break;
				}
			}
		}
		return decl;
	}
	
	public Declaration retrieveMethod(String methodName) {
		Declaration decl = null;
		for (String key : tables.keySet()) {
			ClassTable classTable = tables.get(key);
			decl = classTable.retrieveMethod(methodName);
			if (decl instanceof MethodDecl) {
				break;
			} else {
				decl = null;
			}
		}
		return decl;
	}
	
	public Declaration retrieve(Identifier id) {
		
		return null;
	}
	
	public Declaration getCurrentClassDeclaration() {
		return classDeclarations.get(currentClassTable.name);
	}
	
	class ClassTable {
		public HashMap<String, Declaration> privateFieldDeclarations;
		public  HashMap<String, Declaration> staticFieldDeclarations;
		public  HashMap<String, Declaration> fieldDeclarations;
		public  HashMap<String, Declaration> privateMethodDeclarations;
		public  HashMap<String, Declaration> staticMethodDeclarations;
		public  HashMap<String, Declaration> methodDeclarations;
		public  ArrayList<IdentificationTable> scopedTables;
		public IdentificationTable currentTable;
		public  int currentScope;
		public  int currentTableIndex;
		public String name;
		
		public ClassTable(String name) {
			this.name = name;
			scopedTables = new ArrayList<IdentificationTable>();
			currentScope = 2;
			currentTable = new IdentificationTable(2);
			currentTableIndex = 0;
			scopedTables.add(currentTable);
			
			fieldDeclarations = new HashMap<String, Declaration>();
			privateFieldDeclarations = new HashMap<String, Declaration>();
			staticFieldDeclarations = new HashMap<String, Declaration>();
			
			methodDeclarations = new HashMap<String, Declaration>();
			privateMethodDeclarations = new HashMap<String, Declaration>();
			staticMethodDeclarations = new HashMap<String, Declaration>();
		}
		
		public boolean canAddVarDecl(Declaration d) {
			String name = d.name;
			// check if its in the closest tables until member names
			// go from previous table until gets to field declarations 
			for (int index = currentTableIndex; index > 0; index--) {
				IdentificationTable currentTable = scopedTables.get(index);
				/*if (currentTable.scopeLevel <= 2) {
					break;
				}*/
				if (currentTable.hasDecl(name)) {
					return false;
				}
			}
			return true;
		}
		
		public boolean add(Declaration d) {
			if (d instanceof FieldDecl) {
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
					return false;
					// ERROR, DUPLICATE FIELD NAME
				}
				
			}
			if (d instanceof MethodDecl) {
				if (!methodDeclarations.containsKey(d.name)) {
					if (((MethodDecl) d).isPrivate) {
						privateMethodDeclarations.put(d.name, d);
					}
					if (((MethodDecl) d).isStatic) {
						staticMethodDeclarations.put(d.name, d);
					}
					methodDeclarations.put(d.name, d);
					currentTable.add(d);
				} else {
					return false;
					// ERROR, DUPLICATE METHOD NAME
				}
			}
			if (d instanceof ParameterDecl) {
				currentTable.add(d);
			}
			if (d instanceof VarDecl) {
				if (canAddVarDecl(d)) {
					currentTable.add(d);
				} else {
					return false;
				}
			}
			return true;
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
		
		public Declaration retrieve(String name) {
			
			// retrieve local things first
			Declaration decl = null;
			for (int index = currentTableIndex; index >= 0; index--) {
				IdentificationTable currentTable = scopedTables.get(index);
				/*if (currentTable.scopeLevel <= 2) {
					break;
				}*/
				if (index == 2) {
					
				}
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
		
		public LocalDecl retrieveLocalVar(String name) {
			// retrieve local things first
			Declaration decl = null;
			for (int index = currentTableIndex; index >= 0; index--) {
				IdentificationTable currentTable = scopedTables.get(index);
				/*if (currentTable.scopeLevel <= 2) {
					break;
				}*/
				if (index == 2) {
					
				}
				if (currentTable.hasLocalVarDecl(name)) {
					decl = currentTable.retrieveLocalVar(name);
					break;
				}
			}
			return (LocalDecl) decl;
		}
		
		
		public MethodDecl retrieveMethod(String name) {
			// retrieve local things first
			Declaration decl = null;
			for (int index = currentTableIndex; index >= 0; index--) {
				IdentificationTable currentTable = scopedTables.get(index);
				/*if (currentTable.scopeLevel <= 2) {
					break;
				}*/
				if (index == 2) {
					
				}
				if (currentTable.hasMethodDecl(name)) {
					decl = currentTable.retrieveMethod(name);
					break;
				}
			}
			return (MethodDecl) decl;
		}
		
		
		
		
		
		class IdentificationTable {
			HashMap<String, Declaration> decls;
			HashMap<String, Declaration> fieldDecls;
			HashMap<String, Declaration> varDecls;
			HashMap<String, Declaration> methodDecls;
			int scopeLevel;
			
			public IdentificationTable(int scopeLevel) {
				this.scopeLevel = scopeLevel;
				decls = new HashMap<String, Declaration>();
				fieldDecls = new HashMap<String, Declaration>();
				varDecls = new HashMap<String, Declaration>();
				methodDecls = new HashMap<String, Declaration>();
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
				} else if (decl instanceof MethodDecl) {
					methodDecls.put(decl.name, decl);
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

			public boolean hasMethodDecl(String name) {
				return methodDecls.containsKey(name);
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
			
			public MethodDecl retrieveMethod(String name) {
				return (MethodDecl) methodDecls.get(name);
			}
		}
	}
	
}
