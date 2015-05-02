package miniJava.ContextualAnalyzer;

import java.util.ArrayList;
import miniJava.ErrorReporter;
import miniJava.SyntaxError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.ClassTable;

public class Identification<A,R> implements Visitor<A,R>{
	
	private ClassTableManager tables;
	private ErrorReporter reporter;
	private boolean currentMethodIsStatic;
	int z;
	
	public Identification() {
		tables = new ClassTableManager();
		reporter = new ErrorReporter();
	}
	
	public ClassTableManager identify(AST ast) {
		ast.visit(this, null);
		return tables;
	}
	
	private boolean isStaticFieldInit = false;
	private ArrayList<String> currentlyDeclaredFields; 
	@Override
	public R visitPackage(Package prog, A arg) {
		// INITIALIZE ALL CLASSES TABLES
		// RECORD ALL FIELD / METHODS IN THESE CLASSES
		for (ClassDecl c: prog.classDeclList){
			if (c.name.equals("null")) {
				reportError("cannot name class as null", c);
			}
 			if (!tables.addNewTable(c)) {
				reportError("duplicate class name " + c.name, c);
			}
		}
		for (ClassDecl c: prog.classDeclList){
			// RECORD CLASS FIELDS
			tables.setCurrentClassTable(c.name);
			for (FieldDecl f: c.fieldDeclList) {
				visitFieldDecl(f, null);				
			}
			// RECORD CLASS METHODS
			for (MethodDecl m: c.methodDeclList) {
				recordMethodDecls(c, m);
			}
        }
		
		/*
		 * FOR STATIC FIELD INITIALIZATION
		 * CHECK IN EACH CLASS, IF A FIELD REFERENCED
		 * ANOTHER FIELD IN THE SAME CLASS BEFORE IT HAS BEEN DEFINED
		 * EXAMPLE: static int x = y;
		 *  		static int y;
		 *  	would throw an error.
		 */
		
		for (ClassDecl c: prog.classDeclList){
			tables.setCurrentClassTable(c.name);
			currentlyDeclaredFields = new ArrayList<String>();
			for (FieldDecl d : c.fieldDeclList) {
				if (d instanceof FieldDeclStmt) {
					isStaticFieldInit = true;
					((FieldDeclStmt) d).initExp.visit(this, null);
					isStaticFieldInit = false;
				}
				currentlyDeclaredFields.add(d.name);
			}
        }		
		
		for (ClassDecl c: prog.classDeclList){
			tables.setCurrentClassTable(c.name);
			c.visit(this, null);
        }
		if (!tables.checkMainMethod()) {
			reportError("error regarding the main method", prog);
		}
		return null;
	}

	private void recordMethodDecls(ClassDecl c, MethodDecl m)
			throws SyntaxError {
		String methodName = m.name; 
		if (methodName.equals("null")) {
			reportError("cannot name method as null", c);
		}
		if (m instanceof ConstructorMethodDecl) {
			if (!tables.currentClassTable.add(m)) {
				reportError("duplicate constructor with same arguments " + m.name, m);
			}
		} else {
			if (!tables.currentClassTable.containsMethod(methodName)) {
				tables.currentClassTable.add(m);
			} else {
				reportError("duplicate method name " + m.name, m);
			}
		}
	}

	private String currentClass;
	@Override
	// visit all class names, and then member names first! 
	// because declaration of class and members don't need to be preceding their use in program text 
	public R visitClassDecl(ClassDecl cd, A arg) {
		currentClass = cd.name;
		for (MethodDecl m: cd.methodDeclList) {
			m.visit(this, null);
		}
		return null;
	}

	@Override
	public R visitFieldDecl(FieldDecl fd, A arg) {
		Type fieldType = fd.type;
		String fieldName = fd.name;
	
		// CHECK FIELD NAME
		if (tables.currentClassTable.containsField(fieldName)) {
			reportError("duplicate field name " + fd.name, fd);
		} else if (fd.name.equals("null")) {
			reportError("cannot name field as null", fd);
		}
		
		if (fieldType instanceof BaseType) {
			isFromDeclClassType = true;
			fieldType.visit(this, null);
			isFromDeclClassType = false;
			tables.currentClassTable.add(fd);
		} else if (fieldType instanceof ClassType) {
			
			// CHECK IF VALID TYPE
			Identifier classId = ((ClassType) fieldType).className;
			isFromDeclClassType = true;
			classId.visit(this, null);
			isFromDeclClassType = false;
			Declaration classDecl = classId.decl;
			if (classDecl != null) {
				tables.currentClassTable.add(fd);
			} else {
				reportError("class " + classId.spelling + " was not found", classId);
			}
		} else if (fieldType instanceof ArrayType) {
			Type typeOfArray = ((ArrayType) fieldType).eltType;
			if (typeOfArray instanceof BaseType) {
				tables.currentClassTable.add(fd);
			} else if (typeOfArray instanceof ClassType) {
				
				// CHECK IF VALID TYPE
				
				Identifier classId = ((ClassType) typeOfArray).className;
				isFromDeclClassType = true;
				classId.visit(this, null);
				isFromDeclClassType = false;
				Declaration classDecl = classId.decl;
				if (classDecl != null) {
					// CLASS HAS BEEN DECLARED
					tables.currentClassTable.add(fd);
				} else {
					reportError("class " + classId.spelling + " was not found", fd);
				}
			} else {
				reportError("unknown type found", fd);
			}
		} else {
			reportError("unknown type found", fd);
		}
		return null;
	}

	@Override
	public R visitFielddeclStmt(FieldDeclStmt stmt, A arg) {
		return null;
	}
	
	@Override
	public R visitMethodDecl(MethodDecl md, A arg) {
		
		if (md.isStatic) {
			currentMethodIsStatic = true;
		} else {
			currentMethodIsStatic = false;
		}
		
		// VISIT PARAMETERS
		tables.currentClassTable.openScope();
		for (ParameterDecl p : md.parameterDeclList) {
			p.visit(this, null);
		}
		
		// VISIT TYPE
		if (!(md.type.typeKind == TypeKind.CONSTRUCTOR)) {
			isFromDeclClassType = true;
			md.type.visit(this, null);
			isFromDeclClassType = false;	
		}

		// VISIT STATEMENTS
		tables.currentClassTable.openScope();
		for (Statement s : md.statementList) {
			s.visit(this, null);
		}
		
		// VISIT RETURN STATEMENT
		if (md.returnExp != null) {
			md.returnExp.visit(this, null);	
		}
		
		tables.currentClassTable.closeScope();
		tables.currentClassTable.closeScope();
		return null;
	}

	@Override
	public R visitParameterDecl(ParameterDecl pd, A arg) {
	
		Type parType = pd.type;
		String parName = pd.name;
		if (parName.equals("null")) {
			reportError("cannot name parameter as null", pd);
		}
		
		// ONLY NEED TO CHECK CURRENT CLASS TABLE B/C IT CAN SHADOW (SCOPE LEVEL 3)
		if (tables.currentClassTable.currentTable.hasDecl(parName)) {
			reportError("duplicate parameter name " + parName, pd);
		}
		
		if (parType instanceof BaseType) {
			parType.visit(this, null);
			tables.currentClassTable.currentTable.add(pd);
		} else if (parType instanceof ClassType) {
			parType.visit(this, null);
			Identifier id = ((ClassType) parType).className;
			if (!(id.decl instanceof ClassDecl)) {
				reportError("class " + id.spelling + " not found", pd);
			}
			isFromDeclClassType = true;
			id.visit(this, null);
			isFromDeclClassType = false;
			Declaration decl = id.decl;
			if (decl != null) {
				tables.currentClassTable.currentTable.add(pd);
			} else {
				reportError("declaration " + id.spelling + " not found", pd);
			}
		} else if (parType instanceof ArrayType) {
			parType.visit(this, null);
			
			Type typeOfArray = ((ArrayType) parType).eltType;
			if (typeOfArray instanceof BaseType) {
				tables.currentClassTable.add(pd);
			} else if (typeOfArray instanceof ClassType) {
				Identifier classId = ((ClassType) typeOfArray).className;
				isFromDeclClassType = true;
				classId.visit(this, null);
				isFromDeclClassType = false;
				Declaration classDecl = classId.decl;
				if (classDecl != null) {
					tables.currentClassTable.add(pd);
				} else {
					reportError("class " + classId.spelling + " not found", pd);
				}
			} else {
				reportError("unknown array type", pd);
			}
		} else {
			reportError("unknown parameter type", pd);
		}
		return null;
	}

	private boolean isFromDeclClassType = false;
	@Override
	public R visitVarDecl(VarDecl decl, A arg) {
		Type varType = decl.type;
		if (varType instanceof BaseType) {
			if (!tables.currentClassTable.add(decl)) {
				reportError("duplicate var name " + decl.name, decl);
			}
		} else if (varType instanceof ClassType) {
			Identifier classId = ((ClassType) varType).className;
			isFromDeclClassType = true;
			classId.visit(this, null);
			isFromDeclClassType = false;
			Declaration varDecl = classId.decl;
			if (varDecl != null) {
				if (!tables.currentClassTable.add(decl)) {
					reportError("variable " + decl.name + " already declared in current scope", decl);
				}
			} else {
				reportError("class " + classId.spelling + " not found", decl);
			}
		} else if (varType instanceof ArrayType) {
			varType.visit(this, null);
			
			Type typeOfArray = ((ArrayType) varType).eltType;
			if (typeOfArray instanceof BaseType) {
				if (!tables.currentClassTable.add(decl)) {
					reportError("variable " + decl.name + " already declared in current scope", decl);
				}
			} else if (typeOfArray instanceof ClassType) {
				Identifier classId = ((ClassType) typeOfArray).className;
				isFromDeclClassType = true;
				classId.visit(this, null);
				isFromDeclClassType = false;
				Declaration classDecl = classId.decl;
				if (classDecl != null) {
					if (!tables.currentClassTable.add(decl)) {
						reportError("variable " + decl.name + " already declared in current scope", decl);
					}
				} else {
					reportError("class " + classId.spelling + " not found", decl);
				}
			} else {
				reportError("cannot have this type for an array declaration", decl);
			}
		}
		return null;
	}

	@Override
	public R visitBaseType(BaseType type, A arg) {
		return null;
	}

	@Override
	public R visitClassType(ClassType type, A arg) {
		Identifier className = type.className;
		String classNameString = className.spelling;
		ClassDecl classDecl = (ClassDecl) tables.getClassDeclaration(classNameString);
		if (classDecl != null) {
			type.className.decl = classDecl;
		} else {
			reportError("class " + className + " was not found", type);
		}
		return null;
	}

	@Override
	public R visitArrayType(ArrayType type, A arg) {
		Type arrayType = type.eltType;
		arrayType.visit(this, null);
		return null;
	}

	@Override
	public R visitBlockStmt(BlockStmt stmt, A arg) {
		tables.currentClassTable.openScope();
		for (Statement s : stmt.sl) {
			s.visit(this, null);
		}
		tables.currentClassTable.closeScope();
		return null;
	}
	
	private boolean isFromStandaloneRef = false;
	private String varDeclName;
	private boolean isInVarDecl;
	@Override
	public R visitVardeclStmt(VarDeclStmt stmt, A arg) {
		VarDecl vd = stmt.varDecl;
		Expression expr = stmt.initExp;
		varDeclName = vd.name;
		isInVarDecl = true;
		isFromStandaloneRef = true;
		expr.visit(this, null); // CHANGED ORDER: CASE - INT X = X + 3; ERRONEOUS
		isFromStandaloneRef = false;
		isInVarDecl = false;
		isFromStandaloneRef = true;
		vd.visit(this, null);
		isFromStandaloneRef = false;
		return null;
	}
	
	@Override
	public R visitAssignStmt(AssignStmt stmt, A arg) {
		isFromStandaloneRef = true;
		stmt.ref.visit(this, null);
		
		if (stmt.ref.decl instanceof MethodDecl) {
			reportError("reference is not an assignable destination", stmt);
		}
		isFromStandaloneRef = true;
		stmt.val.visit(this, null);
		isFromStandaloneRef = false;
		return null;
	}

	@Override
	public R visitCallStmt(CallStmt stmt, A arg) {
		isFromCall = true;
		stmt.methodRef.visit(this, null);
		isFromCall = false;
		ExprList argList = stmt.argList;
		for (Expression e : argList) {
			isFromStandaloneRef = true;
			e.visit(this, null);
			isFromStandaloneRef = false;
		}
		return null;
	}
	
	@Override
	public R visitIfStmt(IfStmt stmt, A arg) {
		stmt.cond.visit(this, null);
		if (stmt.thenStmt instanceof VarDeclStmt) {
			reportError("A variable declaration can not be the solitary statement in a branch of a conditional statement", stmt);
		}
		stmt.thenStmt.visit(this, null);
		Statement elseStatement = stmt.elseStmt;
		if (elseStatement != null) {
			if (elseStatement instanceof VarDeclStmt) {
				reportError("A variable declaration can not be the solitary statement in a branch of a conditional statement", stmt);
			} else {
				elseStatement.visit(this, null);
			}
		}
		return null;
	}

	@Override
	public R visitWhileStmt(WhileStmt stmt, A arg) {
		isFromStandaloneRef = true;
		stmt.cond.visit(this, null);
		isFromStandaloneRef = false;
		stmt.body.visit(this, null);
		if (stmt.body instanceof VarDeclStmt) {
			reportError("A variable declaration can not be the solitary statement in a branch of a conditional statement", stmt);
		}
		return null;
	}

	@Override
	public R visitUnaryExpr(UnaryExpr expr, A arg) {
		expr.operator.visit(this, null);
		isFromStandaloneRef = true;
		expr.expr.visit(this, null);
		isFromStandaloneRef = false;
		return null;
	}

	@Override
	public R visitBinaryExpr(BinaryExpr expr, A arg) {
		isFromStandaloneRef = true;
		expr.left.visit(this, null); 
		expr.right.visit(this, null);
		isFromStandaloneRef = false;
		expr.operator.visit(this, null);
		return null;
	}

	@Override
	public R visitRefExpr(RefExpr expr, A arg) {
		isFromStandaloneRef = true;
		expr.ref.visit(this, null);
		isFromStandaloneRef = false;
		if (expr.ref.decl instanceof MethodDecl) {
			reportError("method reference without an invocation", expr);
		} else if (expr.ref.decl instanceof FieldDecl ||
				expr.ref.decl instanceof MethodDecl) {
			Reference reference = expr.ref;
			if (!(reference instanceof QualifiedRef)) {
				if (expr.ref.decl instanceof FieldDecl) {
					FieldDecl decl = (FieldDecl) expr.ref.decl;	
					if (!(decl.isStatic) && currentMethodIsStatic) {
						reportError("cannot reference non-static field " + decl.name + " in static context", expr);
					}	
				} else if (expr.ref.decl instanceof MethodDecl) {
					MethodDecl decl = (MethodDecl) expr.ref.decl;
					if (!(decl.isStatic) && currentMethodIsStatic) {
						reportError("cannot reference non-static method " + decl.name + " in static context", expr);
					}	
				}
			}
		}
		return null;
	}

	private boolean isFromCall;
	@Override
	public R visitCallExpr(CallExpr expr, A arg) {
		isFromCall = true;
		expr.functionRef.visit(this, null);
		if (!(expr.functionRef.decl instanceof MethodDecl)) {
			reportError("cannot invoke a non method reference " + expr.functionRef.decl.name, expr);
		}
		isFromCall = false;
		Reference reference = expr.functionRef;
		if (!(reference instanceof QualifiedRef)) {
			MethodDecl decl = (MethodDecl) expr.functionRef.decl;
			if (!(decl.isStatic) && currentMethodIsStatic) {
				reportError("cannot reference non-static method " + decl.name + " in static context", expr);
			}	
		}
		ExprList argList = expr.argList;
		for (Expression e : argList) {
			e.visit(this, null);
		}
		return null;
	}

	@Override
	public R visitLiteralExpr(LiteralExpr expr, A arg) {
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public R visitNewObjectExpr(NewObjectExpr expr, A arg) {	
		expr.classtype.visit(this, null);	
		if (expr.argList != null) {
			for (Expression e : expr.argList) {
				e.visit(this, null);
			}
		}
		return null;
	}

	@Override
	public R visitNewArrayExpr(NewArrayExpr expr, A arg) {
		isFromStandaloneRef = true;
		expr.sizeExpr.visit(this, null);
		isFromStandaloneRef = false;
		expr.eltType.visit(this, null);
		
		return null;
	}

	private boolean isFromQualified = false;
	private boolean baseCaseQualifiedRef = false;
	private boolean isFromRHSQualifiedRef = false;
	private boolean isFromArrayQualifiedRef = false;
	private int qualifiedRefCounter = 0;
	
	@Override
	public R visitQualifiedRef(QualifiedRef ref, A arg) {
		// QUALIFIED REFS ARE FOR INSTANCE FIELDS, METHODS OR STATIC FIELDS, METHODS
		isFromQualified = true;
		if (!(ref.ref instanceof QualifiedRef)) {
			baseCaseQualifiedRef = true;
		} else {
			qualifiedRefCounter++; // for recursive purposes
		}
		
		// check for this.x
		if (ref.ref instanceof ThisRef) {
			if (isStaticFieldInit) {
				ref.id.visit(this, null);
			} else {
				qualifiedCurrentClassTable = tables.getCurrentClassTable();
				ref.decl = tables.getClassDeclaration(qualifiedCurrentClassTable.name);
				isFromRHSQualifiedRef = true;
				isFromQualified = true;
				baseCaseQualifiedRef = false;
				ref.id.visit(this, null);
				isFromRHSQualifiedRef = false;
				isFromQualified = false;
				Declaration identifier = ref.id.decl;
				ref.decl = identifier;
			}
		} else {
			// VISIT LHS
			ref.ref.visit(this, null); // recursion
			
			if (ref.ref.decl.type instanceof ArrayType) {
				isFromRHSQualifiedRef = true;
				isFromQualified = true;
				baseCaseQualifiedRef = false;
				isFromArrayQualifiedRef = true;
				ref.id.visit(this, null);
				isFromArrayQualifiedRef = false;
				isFromRHSQualifiedRef = false;
				isFromQualified = false;
				
				Declaration declRHS = ref.id.decl;
				ref.decl = declRHS;
				String fieldName = declRHS.name;
				if (!fieldName.equals("length")) {
					reportError("array only has one field, and that is length", ref);
				}
			} else {
				
				// VISIT RHS
				baseCaseQualifiedRef = false;
				isFromRHSQualifiedRef = true;
				isFromQualified = true;
				ref.id.visit(this, null);
				isFromRHSQualifiedRef = false;
				isFromQualified = false;
				
				Declaration declLHS = ref.ref.decl;
				Declaration declRHS = ref.id.decl;
				ref.decl = declRHS;
				String fieldName = declRHS.name;
					
				if (declLHS instanceof ClassDecl) {
					ClassTable table = tables.tables.get(declLHS.name);
					
					if (declRHS instanceof FieldDecl) {
						if (table.privateFieldDeclarations.containsKey(fieldName)) {
							reportError("cannot access private field " + fieldName, ref);
						}
						if (!(table.staticFieldDeclarations.containsKey(fieldName))) {
							reportError("cannot access non static field " + fieldName, ref);
						}
					} else if (declRHS instanceof MethodDecl) {
						if (table.privateMethodDeclarations.containsKey(fieldName)) {
							reportError("cannot access private method " + fieldName, ref);
						}
						if (!(table.staticMethodDeclarations.containsKey(fieldName))) {
							reportError("cannot access non static method " + fieldName, ref);
						}
					}
					// INSTANCE VARIABLE 
				} else if (declLHS instanceof FieldDecl) {
					// FieldDecl.XXXX, means that FieldDecl needs to be an actual classType and not primitive 
					Type type = declLHS.type;
					if (type instanceof ArrayType) {
						if (!fieldName.equals("length")) {
							reportError("array only has one field, and that is length", ref);
						}
					} else if (!(type instanceof ClassType)) {
						reportError("primitive type cannot have fields", declLHS);
					} else {
						String name = ((ClassType) type).className.spelling;
						ClassTable table = tables.tables.get(name);
						
						if (declRHS instanceof FieldDecl) {
							if (table.privateFieldDeclarations.containsKey(fieldName) && !(table.name.equals(currentClass))) {
								reportError("cannot access private field " + fieldName, ref);
							}
						} else if (declRHS instanceof MethodDecl) {
							if (table.privateMethodDeclarations.containsKey(fieldName)  && !(table.name.equals(currentClass))) {
								reportError("cannot access private method " + fieldName, ref);
							}
						}
					}
					// LOCAL VAR DECLARATION
				} else if (declLHS instanceof VarDecl) {
					Type type = declLHS.type;
					if (type instanceof ArrayType) {
						if (!fieldName.equals("length")) {
							reportError("array only has one field, and that is length", ref);
						}
					} else if (!(type instanceof ClassType)) {
						reportError("primitive type cannot have fields", ref);
					} else {
						
						String name = ((ClassType) type).className.spelling;
						ClassTable table = tables.tables.get(name);
						
						if (declRHS instanceof FieldDecl) {
							if (table.privateFieldDeclarations.containsKey(fieldName)) {
								reportError("cannot access private field " + fieldName, ref);
							}
						} else if (declRHS instanceof MethodDecl) {
							if (table.privateMethodDeclarations.containsKey(fieldName)) {
								reportError("cannot access private method " + fieldName, ref);
							}
						}
					}
				} else if (declLHS instanceof MethodDecl) {
					reportError("cannot qualify a method in a reference", ref);
				}
				isFromRHSQualifiedRef = false;
				baseCaseQualifiedRef = false;
				if (qualifiedRefCounter > 0) {
					qualifiedRefCounter--;	
				}
			}
		}
		return null;
	}

	@Override
	public R visitIndexedRef(IndexedRef ref, A arg) {
		isFromStandaloneRef = true;
		ref.indexExpr.visit(this, null);
		isFromStandaloneRef = true;
		ref.ref.visit(this, null);
		isFromStandaloneRef = false;
		ref.decl = ref.ref.decl;
		return null;
	}

	@Override
	public R visitIdRef(IdRef ref, A arg) {
		ref.id.visit(this, null);
		ref.decl = ref.id.decl;
		return null;
	}

	@Override
	public R visitThisRef(ThisRef ref, A arg) {
		ref.decl = tables.getCurrentClassDeclaration();
		return null;
	}

	private ClassTable qualifiedCurrentClassTable = null;
	@Override
	public R visitIdentifier(Identifier id, A arg) {
		String name = id.spelling;
		Declaration decl = null;
		
		// qualified references
		if (isFromQualified) {
			if (baseCaseQualifiedRef) {
				if (isStaticFieldInit) {
					decl = tables.getClassDeclaration(name);
					if (decl != null) {
						id.decl = decl;
						qualifiedCurrentClassTable = tables.getClassTable(name);
					} else {
						reportError("identifier " + name + " not found", id);
					}
				} else {
					// LHS.x
					// local, field, or field declaration, in that order
					ClassTable tempTable = tables.getCurrentClassTable();
					decl = tempTable.retrieveLocalVar(name);
					if (decl != null) {
						id.decl = decl;
						if ((((LocalDecl) decl).type instanceof ArrayType)) {
							
						} else {
							if (!(((LocalDecl) decl).type instanceof ClassType)) {
								reportError("primitive type cannot have fields", id);
							} else {
							String className = ((ClassType)((LocalDecl) decl).type).className.spelling;
							qualifiedCurrentClassTable = tables.getClassTable(className);
							}
						}
					} else {
						tempTable = tables.getCurrentClassTable();
						decl = tempTable.retrieveField(name);
						if (decl != null) {
							id.decl = decl;
							if ((((FieldDecl) decl).type instanceof ArrayType)) {
								
							} else {
								if (!(((FieldDecl) decl).type instanceof ClassType)) {
									reportError("primitive type cannot have fields", id);
								} else {
									String className = ((ClassType)((FieldDecl) decl).type).className.spelling;
									qualifiedCurrentClassTable = tables.getClassTable(className);
								}
							}
						}  else {
							decl = tables.getClassDeclaration(name);
							if (decl != null) {
								id.decl = decl;
								qualifiedCurrentClassTable = tables.getClassTable(name);
							} else {
								reportError("identifier " + name + " not found", id);
							}
							}
					}
				}
			} else if (isFromRHSQualifiedRef) {
					// x.RHS
					// need to check if from call expr
					if (isFromArrayQualifiedRef) {
						if (!id.spelling.equals("length")) {
							reportError("array only has one field, and that is length", id);
						} else {
							ClassTable classTable = tables.getClassTable("Array");
							id.decl = classTable.retrieveField("length");
						}
					} else if (qualifiedCurrentClassTable == null) {
						// x.RHS, x is a primitive
						reportError("primitive type cannot have fields", id);
					} else if (isFromCall && qualifiedRefCounter == 0) {
						if (isStaticFieldInit) {
							decl = qualifiedCurrentClassTable.retrieveStaticMethod(name);
						} else {
							decl = qualifiedCurrentClassTable.retrieveMethod(name);	
						}
						if (decl != null) {
							id.decl = decl;
							qualifiedCurrentClassTable = null;
						} else {
							reportError("method called " + name + " not found", id);
						}
					} else {
						if (isStaticFieldInit) {
							decl = qualifiedCurrentClassTable.retrieveStaticField(name);
						} else {
							decl = qualifiedCurrentClassTable.retrieveField(name);	
						}
						if (decl != null) {
							id.decl = decl;
							if ((((FieldDecl) decl).type instanceof ArrayType)) {
								
							} else {
								if (!(((FieldDecl) decl).type instanceof ClassType)) {
									qualifiedCurrentClassTable = null;
								} else {
									String className = ((ClassType)((FieldDecl) decl).type).className.spelling;
									qualifiedCurrentClassTable = tables.getClassTable(className);
								}
							}
						} else {
							reportError("field " + name + " not found", id);
						} 
					}
				}
		// from getting a class type in VarDecl like Type c = s;
		} else if (isFromDeclClassType){ 
			decl = tables.getClassDeclaration(name);
			if (decl != null) {
				id.decl = decl;
			} else {
				reportError("a type of this class " + name + " was not found", id);
			}
		// just a plain call without any qualifiers
		} else if (isFromCall){
			ClassTable currentTable = tables.currentClassTable;
			if (isStaticFieldInit) {
				decl = currentTable.retrieveStaticMethod(name);
			} else {
				decl = currentTable.retrieveMethod(name);	
			}
			if (decl != null) {
				id.decl = decl;
			} else {
				reportError("method " + name + " not found", id);
			}
		// just local variable or field y = xxxx;
		} else if (isFromStandaloneRef){
			ClassTable currentTable = tables.currentClassTable;
			if (isStaticFieldInit) {
				if (!currentlyDeclaredFields.contains(name)) {
					reportError("Cannot reference a field " + name + " before it is defined", id);
				}
				decl = currentTable.retrieveStaticField(name);
				if (decl != null) {
					id.decl = decl;
				} else {
					reportError("static field " + name + " not found", id);
				}
			} else {
				decl = currentTable.retrieveLocalVar(name);	
				if (decl != null) {
					id.decl = decl;
				} else {
					decl = currentTable.retrieveField(name);
					if (decl != null) {
						id.decl = decl;
					} else {
						reportError("local variable or field " + name + " not found", id);
					}
					} 
			}
		} else {
			decl = tables.retrieve(name);
			if (decl == null) {
				reportError("declaration " + name + " was not found", id);
			}
			id.decl = decl;
			System.out.println("nothing is supposed to reach into this conditional block");
		}
		
		if (isInVarDecl && !isFromQualified) {
			if (decl.name.equals(varDeclName)) {
				reportError("variable declaration cannot reference variable being declared", id);
			}	
		}
		
		if (!isFromQualified && id.decl instanceof FieldDecl 
				&& currentMethodIsStatic) {
			FieldDecl fieldDecl = ((FieldDecl) id.decl);
			if (!fieldDecl.isStatic) {
				reportError("cannot reference non static field in static context", id);
			}
		}
		return null;
	}

	@Override
	public R visitOperator(Operator op, A arg) {
		return null;
	}

	@Override
	public R visitIntLiteral(IntLiteral num, A arg) {
		return null;
	}

	@Override
	public R visitBooleanLiteral(BooleanLiteral bool, A arg) {
		return null;
	}
	
	@Override
	public R visitUnsupportedType(UnsupportedType type, A arg) {
		return null;
	}

	@Override
	public R visitErrorType(ErrorType type, A arg) {
		return null;
	}

	@Override
	public R visitNullLiteral(NullLiteral nullLiteral, A arg) {
		return null;
	}

	private void reportError(String message, AST ast) throws SyntaxError {
		reporter.reportError("*** IDENTIFICATION ", message , "", ast.posn);
		throw new SyntaxError();
	}
}
