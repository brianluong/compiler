package miniJava.ContextualAnalyzer;

import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.CodeGenerator.FieldRuntimeEntity;
import miniJava.CodeGenerator.MethodRuntimeEntity;
import miniJava.SyntacticAnalyzer.Token;

public class ClassTableManager {

	private static final String FIELD_SYSTEM = "out";
	private static final String CLASS_SYSTEM = "System";
	private static final String CLASS__PRINTSTREAM = "_PrintStream";
	private static final String CLASS_ARRAY = "Array";
	private static  final String CLASS_STRING = "String";
	private static final String PARAM_PRINTLN = "n";
	private static final String METHOD_PRINTLN = "println";
	HashMap<String, ClassTable> tables;
	HashMap<String, ClassDecl> classDeclarations;
	ClassTable currentClassTable;
	boolean hasMainMethod;
	MethodDecl mainMethod;
	
	// EACH CLASS HAS ITS OWN SCOPED IDENTIFICATION TABLE
	
	public ClassTableManager() {
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
					if (isMainMethod(md, md.parameterDeclList)) {
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
				if (id.decl.name.equals(CLASS_STRING)) {
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
		return pdl.size() == 1 && pdl.get(0) != null;
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
		//printstreamClassDecl.red = new ClassRuntimeEntity(0); // no static or instance vars
		
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
	
	public Declaration getCurrentClassDeclaration() {
		return classDeclarations.get(currentClassTable.name);
	}
	
	public boolean containsClass(String name) {
		return classDeclarations.containsKey(name);
	}
	
	/*
	 * very general retrieve method
	 * doesn't take into account current class, or scope
	 * should not really be used
	 */
	public Declaration retrieve(String name) {
		// check if it's a class declaration
		// need to check imports
		// need to check for statics (other classes)
		// Check for nearest scope first
		
		// check variables in current class
		Declaration decl = currentClassTable.retrieve(name);
		if (decl != null) {
			return decl;
		// check class declarations
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
}