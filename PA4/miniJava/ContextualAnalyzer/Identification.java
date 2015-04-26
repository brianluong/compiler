package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.SyntaxError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.IdentificationTableManager.ClassTable;
import miniJava.SyntacticAnalyzer.TokenKind;

public class Identification<A,R> implements Visitor<A,R>{
	
	private IdentificationTableManager tables;
	private ErrorReporter reporter;
	private boolean currentMethodIsStatic;
	static int x;
	private int y;
	int z;
	
	public Identification() {
		tables = new IdentificationTableManager();
		reporter = new ErrorReporter();
	}
	
	public void identify(AST ast) {
		ast.visit(this, null);
	}
	
	@Override
	
	public R visitPackage(Package prog, A arg) {
		
		
		// INITIALIZE ALL CLASSES TABLES
		// RECORD ALL FIELD / METHODS IN THESE CLASSES
		for (ClassDecl c: prog.classDeclList){
			if (c.name.equals("null")) {
				reportError("cannot name class as null", c);
			}
 			if (!tables.addNewTable(c)) {
				reportError("DUPLICATE CLASS NAME", c);
			}
		}
		for (ClassDecl c: prog.classDeclList){
			// RECORD CLASS FIELDS
			tables.setCurrentClassTable(c.name);
			for (FieldDecl f: c.fieldDeclList) {
				if (f.name.equals("null")) {
					reportError("cannot name field as null", c);
				}
				f.visit(this, null);				
			}

			// RECORD CLASS METHODS
			for (MethodDecl m: c.methodDeclList) {
				String methodName = m.name; 
				if (methodName.equals("null")) {
					reportError("cannot name method as null", c);
				}
				if (!tables.currentClassTable.containsMethod(methodName) &&
						!tables.currentClassTable.containsField(methodName)) {
					tables.currentClassTable.add(m);
					
				} else {
					reportError("DUPLICATE METHOD OR FIELD NAME", m);
				}
			}
        }
		
		// GO INTO EACH CLASS ONE AT A TIME
		for (ClassDecl c: prog.classDeclList){
			tables.setCurrentClassTable(c.name);
			c.visit(this, null);
        }
		if (!tables.checkMainMethod()) {
			reportError("MAIN METHOD ERROR", prog);
		}
		
		/*
		// LOAD PREDEFINED CLASSES
		for (String predefinedClass : tables.classDeclarations.keySet()) {
			prog.classDeclList.add(tables.classDeclarations.get(predefinedClass));
		}*/
		
		return null;
	}

	
	private String currentClass;
	
	@Override
	// visit all class names, and then member names first! 
	// because declaration of class and members don't need to be preceding their use in program text 
	public R visitClassDecl(ClassDecl cd, A arg) {
		currentClass = cd.name;
		/*
		// VISIT CLASS FIELDS
		for (FieldDecl f: cd.fieldDeclList) {
			f.visit(this, null);	
		}
		
		// RECORD METHOD NAMES 
		for (MethodDecl m: cd.methodDeclList) {
			String methodName = m.name; 
			
			if (!tables.currentClassTable.containsMethod(methodName) &&
					!tables.currentClassTable.containsField(methodName)) {
				tables.currentClassTable.add(m);
			} else {
				reportError("DUPLICATE METHOD OR FIELD NAME", m);
			}
		}
		*/
		
		// VISIT EACH METHOD
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
		if (tables.currentClassTable.containsField(fieldName) ||
				tables.currentClassTable.containsMethod(fieldName)) {
			reportError("DUPLICATE FIELD NAME", fd);
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
				// CLASS HAS BEEN DECLARED
				tables.currentClassTable.add(fd);
			} else {
				reportError("CLASS UNDECLARED", fd);
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
					reportError("CLASS UNDECLARED", fd);
				}
			} else {
				reportError("UNKNOWN TYPE", fd);
			}
		} else {
			reportError("UNKNOWN TYPE", fd);
		}
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
		isFromDeclClassType = true;
		md.type.visit(this, null);
		isFromDeclClassType = false;
		
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
			reportError("DUPLICATE PARAMETER NAME", pd);
		}
		
		if (parType instanceof BaseType) {
			parType.visit(this, null);
			tables.currentClassTable.currentTable.add(pd);
		} else if (parType instanceof ClassType) {
			parType.visit(this, null);
			Identifier id = ((ClassType) parType).className;
			if (!(id.decl instanceof ClassDecl)) {
				reportError("UNDECLARED CLASS NAME", pd);
			}
			isFromDeclClassType = true;
			id.visit(this, null);
			isFromDeclClassType = false;
			Declaration decl = id.decl;
			if (decl != null) {
				tables.currentClassTable.currentTable.add(pd);
			} else {
				reportError("DECLARATION NOT FOUND", pd);
			}
		} else if (parType instanceof ArrayType) {
			parType.visit(this, null);
			
			Type typeOfArray = ((ArrayType) parType).eltType;
			if (typeOfArray instanceof BaseType) {
				tables.currentClassTable.add(pd);
			} else if (typeOfArray instanceof ClassType) {
				
				// CHECK IF VALID TYPE
				
				Identifier classId = ((ClassType) typeOfArray).className;
				isFromDeclClassType = true;
				classId.visit(this, null);
				isFromDeclClassType = false;
				Declaration classDecl = classId.decl;
				if (classDecl != null) {
					// CLASS HAS BEEN DECLARED
					tables.currentClassTable.add(pd);
				} else {
					reportError("CLASS NOT DECLARED", pd);
				}
			} else {
				reportError("ARRAY TYPE ERROR", pd);
			}
		} else {
			reportError("UNKNOWN FIELDTYPE", pd);
		}
		return null;
	}

	private boolean isFromDeclClassType = false;
	@Override
	public R visitVarDecl(VarDecl decl, A arg) {
		Type varType = decl.type;
		if (varType instanceof BaseType) {
			if (!tables.currentClassTable.add(decl)) {
				reportError("DUPLICATE VAR NAME", decl);
			}
		} else if (varType instanceof ClassType) {
			Identifier classId = ((ClassType) varType).className;
			isFromDeclClassType = true;
			classId.visit(this, null);
			isFromDeclClassType = false;
			Declaration varDecl = classId.decl;
			if (varDecl != null) {
				// CLASS HAS BEEN DECLARED
				if (!tables.currentClassTable.add(decl)) {
					reportError("INVALID NAME IN CURRENT SCOPE", decl);
				}
			} else {
				reportError("CLASS NOT DECLARED", decl);
			}
		} else if (varType instanceof ArrayType) {
			varType.visit(this, null);
			
			Type typeOfArray = ((ArrayType) varType).eltType;
			if (typeOfArray instanceof BaseType) {
				if (!tables.currentClassTable.add(decl)) {
					reportError("INVALID NAME IN CURRENT SCOPE", decl);
				}
				// success
				
			} else if (typeOfArray instanceof ClassType) {
				
				// CHECK IF VALID TYPE
				
				Identifier classId = ((ClassType) typeOfArray).className;
				isFromDeclClassType = true;
				classId.visit(this, null);
				isFromDeclClassType = false;
				Declaration classDecl = classId.decl;
				if (classDecl != null) {
					// CLASS HAS BEEN DECLARED
					if (!tables.currentClassTable.add(decl)) {
						reportError("INVALID NAME IN CURRENT SCOPE", decl);
					}
				} else {
					reportError("CLASS NOT DEFINED", decl);
				}
				// success
			} else {
				reportError("ARRAYTYPE ERROR", decl);
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
			reportError("invalid class, it doesn't exit", type);
		}
		//type.className.visit(this, null);
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
		if (expr instanceof NewArrayExpr) {
			
		}
		isInVarDecl = false;
		isFromStandaloneRef = true;
		vd.visit(this, null);
		isFromStandaloneRef = false;
		return null;
	}
	
	private boolean isFromStandaloneRef = false;
	
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
					FieldDecl decl = (FieldDecl) expr.ref.decl;		// EXAMPLE Y = X;
					// **BUG. Y = X IS STILL VALID, WHEN X IS PART OF ANOTHER CLASS.
					
					if (!(decl.isStatic) && currentMethodIsStatic) {
						reportError("cannot reference non-static symbol in static context", expr);
					}	
				} else if (expr.ref.decl instanceof MethodDecl) {
					MethodDecl decl = (MethodDecl) expr.ref.decl;
					if (!(decl.isStatic) && currentMethodIsStatic) {
						reportError("cannot reference non-static symbol in static context", expr);
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
				reportError("cannot reference non-static symbol in static context", expr);
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
	private boolean isFromLHSQualifiedRef = false;
	private boolean isFromRHSQualifiedRef = false;
	private boolean isFromArrayQualifiedRef = false;
	private int qualifiedRefCounter = 0;
	
	@Override
	public R visitQualifiedRef(QualifiedRef ref, A arg) {
		// QUALIFIED REFS ARE FOR FIELDS OR STATIC FIELDS
		// Static variable or instance variable or private variable
		isFromQualified = true;
		if (!(ref.ref instanceof QualifiedRef)) {
			baseCaseQualifiedRef = true;
		} else {
			qualifiedRefCounter++;
		}
		
		// check for this.xxxx
		if (ref.ref instanceof ThisRef) {
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
		} else {
			//isFromLHSQualifiedRef = true;
			ref.ref.visit(this, null); // recursion
			//isFromLHSQualifiedRef = false;
			if (ref.ref.decl.type instanceof ArrayType) {
				String name = ref.id.spelling;
				isFromRHSQualifiedRef = true;
				isFromQualified = true;
				baseCaseQualifiedRef = false;
				isFromArrayQualifiedRef = true;
				ref.id.visit(this, null);
				isFromArrayQualifiedRef = false;
				isFromRHSQualifiedRef = false;
				isFromQualified = false;
				Declaration identifier = ref.id.decl;
				ref.decl = identifier;
				//ref.id.
				String fieldName = identifier.name;
				if (identifier instanceof FieldDecl) {
					if (!fieldName.equals("length")) {
						reportError("array only has one field, and that is length", ref);
					}
				} else {
					reportError("array only has one field, and that is length", ref);
				}	
			} else {
				baseCaseQualifiedRef = false;
				// can't evaluate id yet!
				//ref.id.visit(this, null); // check if id is private , static, or reg field 
				
				//ref.decl = ref.id.decl; // determines if it's wanting a field or a method 
				Declaration qualifiedRef = ref.ref.decl;
				
				//Declaration identifier = ref.id.decl; // field or method 
				//String fieldName = identifier.name; // current issue: what if its like c.x and c is a VarDeclStmt
				
				//isFromQualified = true;
				// CHECK IF IT'S A THISREF
				isFromRHSQualifiedRef = true;
				isFromQualified = true;
				ref.id.visit(this, null);
				isFromRHSQualifiedRef = false;
				isFromQualified = false;
				Declaration identifier = ref.id.decl;
				ref.decl = identifier;
				String fieldName = identifier.name;
					
				if (ref.ref instanceof ThisRef) {
					ClassTable table = tables.getCurrentClassTable();

				// STATIC ACCESS 
				} else if (qualifiedRef instanceof ClassDecl) {
					ClassTable table = tables.tables.get(qualifiedRef.name);
					
					if (identifier instanceof FieldDecl) {
						if (table.privateFieldDeclarations.containsKey(fieldName)) {
							reportError("CANNOT ACCESS PRIVATE FIELD", ref);
						}
						if (!(table.staticFieldDeclarations.containsKey(fieldName))) {
							reportError("CANNOT ACCESS NON STATIC FIELD", ref);
						}
					} else if (identifier instanceof MethodDecl) {
						if (table.privateMethodDeclarations.containsKey(fieldName)) {
							reportError("CANNOT ACCESS PRIVATE METHOD", ref);
						}
						if (!(table.staticMethodDeclarations.containsKey(fieldName))) {
							reportError("CANNOT ACCESS NON STATIC METHOD", ref);
						}
					}
					// INSTANCE VARIABLE 
				} else if (qualifiedRef instanceof FieldDecl) {
					// FieldDecl.XXXX, means that FieldDecl needs to be an actual classType and not primitive 
					Type type = qualifiedRef.type;
					if (type instanceof ArrayType) {
						if (identifier instanceof FieldDecl) {
							if (!fieldName.equals("length")) {
								reportError("array only has one field, and that is length", ref);
							}
						} else {
							reportError("array only has one field, and that is length", ref);
						}
					} else if (!(type instanceof ClassType)) {
						reportError("Primitive type cannot have fields", qualifiedRef);
					} else {
						String name = ((ClassType) type).className.spelling;
						ClassTable table = tables.tables.get(name);
						
						if (identifier instanceof FieldDecl) {
							if (table.privateFieldDeclarations.containsKey(fieldName) && !(table.name.equals(currentClass))) {
								reportError("CANNOT ACCESS PRIVATE FIELD", ref);
							}
						} else if (identifier instanceof MethodDecl) {
							if (table.privateMethodDeclarations.containsKey(fieldName)  && !(table.name.equals(currentClass))) {
								reportError("CANNOT ACCESS PRIVATE METHOD", ref);
							}
						}
					}
					// VAR DECL 
				} else if (qualifiedRef instanceof VarDecl) {
					Type type = qualifiedRef.type;
					if (type instanceof ArrayType) {
						if (identifier instanceof FieldDecl) {
							if (!fieldName.equals("length")) {
								reportError("array only has one field, and that is length", ref);
							}
						} else {
							reportError("array only has one field, and that is length", ref);
						}
					} else if (!(type instanceof ClassType)) {
						reportError("Primitive type cannot have fields", ref);
					} else {
						
						String name = ((ClassType) type).className.spelling;
						ClassTable table = tables.tables.get(name);
						
						if (identifier instanceof FieldDecl) {
							if (table.privateFieldDeclarations.containsKey(fieldName)) {
								reportError("CANNOT ACCESS PRIVATE FIELD", ref);
							}
						} else if (identifier instanceof MethodDecl) {
							if (table.privateMethodDeclarations.containsKey(fieldName)) {
								reportError("CANNOT ACCESS PRIVATE METHOD", ref);
							}
						}
					}
				} else if (qualifiedRef instanceof MethodDecl) {
					reportError("Cannot qualify a method in a reference", ref);
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
				// LHS.x
				// local, field, or field decl // in that order
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
			} else if (isFromRHSQualifiedRef) {
				// x.RHS
				// need to check if from call expr
				if (isFromArrayQualifiedRef) {
					ClassTable arrayClassTable = tables.getClassTable("Array");
					decl = arrayClassTable.retrieveField(name);
					if (decl != null) {
						id.decl = decl;
						qualifiedCurrentClassTable = null;
					} else {
						reportError("array only has one field, and that is length", id);
					}
				} else if (qualifiedCurrentClassTable == null) {
					// x.RHS, x is a primitive
					reportError("Primitive type cannot have fields", id);
				} else if (isFromCall && qualifiedRefCounter == 0) {
					decl = qualifiedCurrentClassTable.retrieveMethod(name);
					if (decl != null) {
						id.decl = decl;
						qualifiedCurrentClassTable = null;
					} else {
						reportError("method called " + name + " not found", id);
					}
				} else {
					decl = qualifiedCurrentClassTable.retrieveField(name);
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
		} else if (isFromDeclClassType){ // from getting a class type in VarDecl
			decl = tables.getClassDeclaration(name);
			if (decl != null) {
				id.decl = decl;
			} else {
				reportError("a type of this class " + name + " was not found", id);
			}
		// just a plain call without any qualifiers
		} else if (isFromCall){
			ClassTable currentTable = tables.currentClassTable;
			decl = currentTable.retrieveMethod(name);
			if (decl != null) {
				id.decl = decl;
			} else {
				reportError("method " + name + " not found", id);
			}
		// just local variable or field y = xxxx;
		} else if (isFromStandaloneRef){
			ClassTable currentTable = tables.currentClassTable;
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
		} else {
			decl = tables.retrieve(name);
			if (decl == null) {
				reportError("DECLARATION NOT FOUND", id);
			}
			id.decl = decl;
			System.out.println("TESTING. NOTHING IS SUPPOSED TO GET HERE");
		}
		
		/*
		if (isFromQualified && baseCaseQualifiedRef) {
			if (id.decl instanceof FieldDecl) {
				ClassTable tempTable = tables.getCurrentClassTable();
				decl = tempTable.retrieveField(name);
				if (decl == null) {
					reportError("DECLARATION NOT FOUND", id);
				}
				id.decl = decl;
				qualifiedCurrentClassTable = tables.getClassTable(decl.name);
			} else if (id.decl instanceof VarDecl) {
				ClassTable tempTable = tables.getCurrentClassTable();
				decl = tempTable.retrieveLocalVar(name);
				if (decl == null) {
					reportError("DECLARATION NOT FOUND", id);
				}
				id.decl = decl;
				qualifiedCurrentClassTable = tables.getClassTable(decl.name);
			} else if (id.decl instanceof ClassDecl) {
				// static
				
				qualifiedCurrentClassTable = tables.getClassTable(id.decl.name);
				id.decl = id.decl; 
			}
		}
		else if (isFromCall && baseCaseQualifiedRef) {
			decl = tables.retrieveMethod(name);
			if (decl == null) {
				reportError("DECLARATION NOT FOUND", id);
			}
			id.decl = decl;
		}  else if (!isFromQualified) { // need to check isCallExpr too?
			qualifiedCurrentClassTable = tables.getCurrentClassTable();
			// just a call like foo()
			if (isFromCall) {
				if (id.decl instanceof MethodDecl) {
					decl = qualifiedCurrentClassTable.retrieveField(name);
				}
			} else {
				// this is just a variable like counter
				// need to check if it's within class. 
				//id.decl
				if (id.decl instanceof FieldDecl) {
					decl = qualifiedCurrentClassTable.retrieveField(name);
					if (!((FieldDecl) decl).isStatic){
						reportError("can't refer to a non static field", id);
					}
				} else if (id.decl instanceof LocalDecl) {
					decl = qualifiedCurrentClassTable.retrieveLocalVar(name);
				} else if(id.decl instanceof ClassDecl) {
					decl = tables.getClassDeclaration(name);
				}
			}
			if (decl == null) {
				reportError("DECLARATION NOT FOUND", id);
			}
			id.decl = decl;
		} else {
			// LOOKS FOR CLASS, FIELDS, LOCAL VARS
			decl = tables.retrieve(name);
			if (decl == null) {
				reportError("DECLARATION NOT FOUND", id);
			}
			id.decl = decl;
		}*/
		
		if (isInVarDecl && !isFromQualified) {
			if (decl.name.equals(varDeclName)) {
				reportError("variable declaration cannot reference variable being declared", id);
			}	
		}
		
		if (!isFromQualified && id.decl instanceof FieldDecl 
				&& currentMethodIsStatic) {
			FieldDecl fieldDecl = ((FieldDecl) id.decl);
			if (!fieldDecl.isStatic) {
				reportError("CANNOT REFERENCE NON STATIC FIELD IN STATIC CONTEXT", id);
			}
		}
		/*
		if (baseCaseQualifiedRef) {
			isFromQualified = false;	
		}*/
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
	
	private void reportError(String message, AST ast) throws SyntaxError {
		reporter.reportError("*** IDENTIFICATION ", message , "", ast.posn);
		throw new SyntaxError();
	}

	@Override
	public R visitUnsupportedType(UnsupportedType type, A arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public R visitErrorType(ErrorType type, A arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public R visitNullLiteral(NullLiteral nullLiteral, A arg) {
		// TODO Auto-generated method stub
		return null;
	}
}
