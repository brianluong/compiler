package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.SyntaxError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;

public class TypeChecker implements Visitor<Object, miniJava.AbstractSyntaxTrees.Type>{

	ErrorReporter reporter;
	BaseType booleanType;
	BaseType voidType;
	BaseType intType;
	ErrorType errorType;
	BaseType nullType;
	UnsupportedType unsupportedType;
	ClassTableManager tables;
	
	public TypeChecker() {
		reporter = new ErrorReporter();
		booleanType = new BaseType(TypeKind.BOOLEAN, null);
		voidType = new BaseType(TypeKind.VOID, null);
		intType = new BaseType(TypeKind.INT, null);
		errorType = new ErrorType(TypeKind.ERROR, null);
		nullType = new BaseType(TypeKind.NULL, null);
		unsupportedType = new UnsupportedType(TypeKind.UNSUPPORTED, null);
	}
	
	public boolean typeCheck(AST ast, ClassTableManager c) {
		tables = c;
		Type type = ast.visit(this, null);
		if (type instanceof ErrorType || type instanceof UnsupportedType) {
			return false;
		} else {
			return true;
		}
	}

	public Type visitPackage(Package prog, Object o ) {
		for (ClassDecl c: prog.classDeclList){
			c.visit(this, null);
        }
		return null;
	}

	public Type visitClassDecl(ClassDecl cd, Object o ) {
		for (FieldDecl f: cd.fieldDeclList) {
			f.visit(this, null );	
		}
		for (MethodDecl m: cd.methodDeclList) {
			m.visit(this, null );
		}
		return cd.type;
	}

	public Type visitFieldDecl(FieldDecl fd, Object o ) {
		return fd.type;
	}
	
	@Override
	public Type visitFielddeclStmt(FieldDeclStmt stmt, Object arg) {
		Type t = stmt.initExp.visit(this, null);
		Type t2 = stmt.type;
		Type t3 = equals(t, t2);
		if (t3.typeKind == TypeKind.ERROR 
				|| t3.typeKind == TypeKind.UNSUPPORTED) {
			reportError("assignment type incompatible with variable type", stmt);
		}
		return t3;
	}

	public Type visitMethodDecl(MethodDecl md, Object o) {
		for (ParameterDecl p : md.parameterDeclList) {
			p.visit(this , null);
		}
		for (Statement s : md.statementList) {
			s.visit(this, null );
		}
		
		Expression returnExpression = md.returnExp;
		Type methodHeaderType = null;
		if (md.type.typeKind != TypeKind.CONSTRUCTOR) {
			methodHeaderType = md.type.visit(this, null);	
		}
		
		// CHECK METHOD RETURN TYPE WITH RETURN EXPRESSION TYPE
		if (returnExpression != null) {
			Type returnType = returnExpression.visit(this, null);
			Type t = equals(methodHeaderType, returnType);
			if (t.typeKind == TypeKind.ERROR || 
					t.typeKind == TypeKind.UNSUPPORTED) {
				reportError("return type incompatible with declared type", md);
			}
			if (methodHeaderType.typeKind == TypeKind.VOID) {
				reportError("a void method cannot have a return statement", md);
			}
		} else {
			if (methodHeaderType != null && methodHeaderType.typeKind != TypeKind.VOID) {
				reportError("method expected a return statement", md);
			}
		}
		return methodHeaderType;
	}

	@Override
	public Type visitParameterDecl(ParameterDecl pd, Object o) {
		return pd.type;
	}

	@Override
	public Type visitVarDecl(VarDecl decl, Object o) {
		return decl.type;
	}

	@Override
	public Type visitBaseType(BaseType type, Object o) {
		return type;
	}

	@Override
	public Type visitClassType(ClassType type, Object o) {
		return type;
	}

	@Override
	public Type visitArrayType(ArrayType type, Object o) {
		return type;
	}
	
	public Type visitErrorType(ErrorType errorType, Object o) {
		return this.errorType;
	}

	public Type visitUnsupportedType(UnsupportedType unsupportedType, Object o) {
		return this.unsupportedType;
	}
	
	@Override
	public Type visitBlockStmt(BlockStmt stmt, Object o) {
		for (Statement s : stmt.sl) {
			s.visit(this, null );
		}
		return null;
	}

	@Override
	public Type visitVardeclStmt(VarDeclStmt stmt, Object o) {
		Type t = stmt.varDecl.visit(this, null);
		Type t4 = t;
		if (t instanceof ArrayType) {
			Type type1 = ( (ArrayType) t).eltType;
			t = type1;	
		}
		Type t2 = stmt.initExp.visit(this, null);
		if (t2 instanceof BaseType) {
			if (t2.typeKind.equals(TypeKind.NULL)) {
				if (!(t instanceof ClassType)) {
					System.out.println(t.typeKind);
					reportError("null can only be assigned to objects", stmt);
				} 
			}
		}
		
		// ARRAY LENGTH 
		if (t4 instanceof ArrayType) {
			((ArrayType) stmt.varDecl.type).length = stmt.initExp.intValue;
		}
		
		Type t3 = equals(t, t2);
		if (t3.typeKind == TypeKind.ERROR 
				|| t3.typeKind == TypeKind.UNSUPPORTED) {
			reportError("variable declaration incompatible with initial value type", stmt);
		}
		return t3;
	}

	@Override
	public Type visitAssignStmt(AssignStmt stmt, Object o ) {
		Type t = stmt.ref.visit(this, null );
		if (t instanceof ArrayType) {
			Type type1 = ( (ArrayType) t).eltType;
			t = type1;
		}
		
		Type t2 = stmt.val.visit(this, null );

		if (t2 instanceof BaseType) {
			if (t2.typeKind.equals(TypeKind.NULL)) {
				if (!(t instanceof ClassType)) {
					System.out.println(t.typeKind);
					reportError("null can only be assigned to objects", stmt);
				} 
			}
		}
		
		Type t3 = equals(t, t2);
		if (t3.typeKind == TypeKind.ERROR 
				|| t3.typeKind == TypeKind.UNSUPPORTED) {
			reportError("assignment type incompatible with variable type", stmt);
		}
		return t3;
	}

	@Override
	public Type visitCallStmt(CallStmt stmt, Object o ) {
		Type t = stmt.methodRef.visit(this, null );
		// need to check type of args
		Declaration d = stmt.methodRef.decl;
		ParameterDeclList methodParameters = ((MethodDecl) d).parameterDeclList;
		ExprList methodArgs = stmt.argList;
		if (methodParameters.size() == methodArgs.size()) {
			for (int i = 0; i < methodParameters.size(); i++) {
				ParameterDecl parameter = methodParameters.get(i);
				Expression argument = methodArgs.get(i);
				Type parameterType = parameter.type;
				Type argType = argument.visit(this, null);
				if (equals(parameterType, argType) instanceof ErrorType) {
					reportError("WRONG TYPE ARGUMENT(S)", stmt);
				}
			}
		} else {
			reportError("INVALID NUMBER OF ARGUMENTS", stmt);
		}
		return t;
	}

	@Override
	public Type visitIfStmt(IfStmt stmt, Object o ) {
		Type t = stmt.cond.visit(this, null );
		if (t.typeKind != TypeKind.BOOLEAN) {
			reportError("expression in conditional statement does not yield a boolean result", stmt);
		}
		stmt.thenStmt.visit(this, null );
		Statement elseStatement = stmt.elseStmt;
		if (elseStatement != null) {
			elseStatement.visit(this, null );
		}
		return t;
	}

	@Override
	public Type visitWhileStmt(WhileStmt stmt, Object o ) {
		Type t = stmt.cond.visit(this, null );
		stmt.body.visit(this, null );
		return t;
	}

	@Override
	public Type visitUnaryExpr(UnaryExpr expr, Object o ) {
		Type t = expr.expr.visit(this, null );
		Operator op = expr.operator;
		Type t2 = unaryOperator(op, t);
		return t2;
	}

	@Override
	public Type visitBinaryExpr(BinaryExpr expr, Object o ) {
		Type t = expr.left.visit(this, null );
		Type t2 = expr.right.visit(this, null );
		Operator op = expr.operator;
		Type t3 = binaryOperator(op, t, t2);
		return t3;
	}

	@Override
	public Type visitRefExpr(RefExpr expr, Object o ) {
		Type t  = expr.ref.visit(this, null );
		return t;
	}

	@Override
	public Type visitCallExpr(CallExpr expr , Object o) {
		ParameterDeclList methodParameters  = ((MethodDecl) expr.functionRef.decl).parameterDeclList;
		ExprList methodArgs = expr.argList;
		// COMPARE METHOD DECLARATION PARAMETERS WITH SUPPLIED ARGUMENTS
		if (methodParameters.size() == methodArgs.size()) {
			for (int i = 0; i < methodParameters.size(); i++) {
				ParameterDecl parameter = methodParameters.get(i);
				Expression argument = methodArgs.get(i);
				Type parameterType = parameter.type;
				Type argumentType = argument.visit(this, null);
				if (equals(parameterType, argumentType) instanceof ErrorType) {
					reportError("WRONG TYPE ARGUMENT(S)", expr);
				}
			}
		} else {
			reportError("INVALID NUMBER OF ARGUMENTS", expr);
		}	
		Type functionType = expr.functionRef.visit(this, null );
		return functionType;
	}

	@Override
	public Type visitLiteralExpr(LiteralExpr expr, Object o ) {
		Type t = expr.lit.visit(this, null );
		if (t.typeKind == TypeKind.INT) {
			expr.intValue = Integer.parseInt(expr.lit.spelling);
		}
		return t;
	}

	@Override
	public Type visitNewObjectExpr(NewObjectExpr expr , Object o) {
		Type t = expr.classtype.visit(this, null ); 
		if (expr.argList != null) {
			Type[] typeArgs = new Type[expr.argList.size()];
			int counter = 0;
			if (expr.argList != null) {
				for (Expression e : expr.argList) {
					typeArgs[counter] = e.visit(this, null);
					counter++;
				}	
			}
			// need to match expression with its constructor
			String name = expr.classtype.className.spelling;
			ClassTable classTable = tables.getClassTable(name);
			ConstructorMethodDecl constructor = classTable.matchConstructor(typeArgs);
			if (constructor == null && expr.argList.size() > 0) {
				reportError("appropriate constructor not found", expr);
			}
			if (constructor != null) {
				expr.constructor = constructor;
			}
		}
		return t; 
	}

	@Override
	public Type visitNewArrayExpr(NewArrayExpr expr , Object o) {
		Type t = expr.eltType.visit(this, null );
		
		// ARRAY INDEX MUST BE TYPE INT
		Type t2 = expr.sizeExpr.visit(this, null );
		if (t2 instanceof BaseType) {
			if (((BaseType) t2).typeKind != TypeKind.INT) {
				reportError("can only have an integer value in a new array expression", expr);
			}
		} else {
			reportError("can only have an integer value in a new array expression", expr);
		}
		return t;
	}

	@Override
	public Type visitQualifiedRef(QualifiedRef ref, Object o ) {
		return ref.decl.type;
	}

	@Override
	public Type visitIndexedRef(IndexedRef ref, Object o ) {
		Type t = ((ArrayType) ref.decl.type).eltType;
		
		// ARRAY INDEX SHOULD BE TYPE INT
		Type t2 = ref.indexExpr.visit(this, null ); 
		if (t2 instanceof BaseType) {
			if (((BaseType) t2).typeKind != TypeKind.INT) {
				reportError("array index should be an int", ref);
			} else {
				return t;
			}
		} else {
			reportError("array index should be an int", ref);
		}
		return t;
	}

	@Override
	public Type visitIdRef(IdRef ref, Object o ) {
		return ref.decl.type;
	}

	@Override
	public Type visitThisRef(ThisRef ref, Object o ) {
		return new ClassType( new Identifier( new Token(0, ref.decl.name, null)), ref.posn);
	}

	@Override
	public Type visitIdentifier(Identifier id , Object o) {
		return id.decl.type;
	}

	@Override
	public Type visitOperator(Operator op, Object o ) {
		return this.errorType;
	}

	@Override
	public Type visitIntLiteral(IntLiteral num, Object o) {
		return this.intType;
	}

	@Override
	public Type visitBooleanLiteral(BooleanLiteral bool, Object o) {
		return this.booleanType;
	}
		
	@Override
	public Type visitNullLiteral(NullLiteral nullLiteral, Object arg) {
		return this.nullType;
	}
	
	private Type equals(Type t1, Type t2) {
		if (t1 instanceof ErrorType || t2 instanceof ErrorType) {
			return this.errorType;
		} else if (t1 instanceof BaseType && t2 instanceof BaseType) {
			if (t1.typeKind == t2.typeKind) {
				return t1;
			} else {
				return this.errorType;
			}
		} else if (t1 instanceof ClassType && t2 instanceof ClassType) {
			if ( ((ClassType) t1).className.spelling.equals( ((ClassType) t2).className.spelling) ) {
				return t1;
			} else {
				return this.errorType;
			}
		} else if (t1 instanceof ArrayType && t2 instanceof ArrayType) {
			Type type1 = ( (ArrayType) t1).eltType;
			Type type2 = ( (ArrayType) t2).eltType;
			return equals(type1, type2);
		}
		return this.errorType;
	}
	
	private Type unaryOperator(Operator op, Type t) {
		if (op.spelling.equals("!")) {
			if (t instanceof BaseType) {
				if (t.typeKind == TypeKind.BOOLEAN) {
					return this.booleanType;
				}
			} else if (t instanceof ArrayType) {
				Type type1 = ( (ArrayType) t).eltType;
				if (type1 instanceof BaseType) {
					if (t.typeKind == TypeKind.BOOLEAN) {
						return this.booleanType;
					}
				}
			}
			reportError("Unary Operator cannot be used on " + t.typeKind.toString(),  op);
			return this.errorType;
		} else if (op.spelling.equals("-")){
			if (t instanceof BaseType) {
				if (t.typeKind == TypeKind.INT) {
					return this.intType;
				}
			} else if (t instanceof ArrayType) {
				Type type1 = ( (ArrayType) t).eltType;
				if (type1 instanceof BaseType) {
					if (t.typeKind == TypeKind.INT) {
						return this.intType;
					}
				}
			}
			reportError("Unary Operator cannot be used on type " + t.typeKind.toString(),  op);
			return this.errorType;
		}
		return this.errorType;
	}
	
	private Type binaryOperator(Operator op, Type t1, Type t2) {
		if (op.spelling.equals("+") 
				|| op.spelling.equals("-")
				|| op.spelling.equals("/")
				|| op.spelling.equals("*")) {
			if (t1 instanceof BaseType && t2 instanceof BaseType) {
				if (t1.typeKind == TypeKind.INT 
						&& t2.typeKind == TypeKind.INT) {
					return this.intType;
				}
			} else if (t1 instanceof ArrayType && t2 instanceof ArrayType) {
				Type type1 = ( (ArrayType) t1).eltType;
				Type type2 = ( (ArrayType) t2).eltType;
				if (type1 instanceof BaseType && type2 instanceof BaseType) {
					if (t1.typeKind == TypeKind.INT 
							&& t2.typeKind == TypeKind.INT) {
						return this.intType;
					}
				}
			}
			reportError("CONTEXTUAL ANALYSIS TYPE MATCHING FAIL using " + op.spelling + " operator",  op);
			return this.errorType;
		} else if (op.spelling.equals(">") 
				|| op.spelling.equals(">=")
				|| op.spelling.equals("<")
				|| op.spelling.equals("<=")) {
			if (t1 instanceof BaseType && t2 instanceof BaseType) {
				if (t1.typeKind == TypeKind.INT 
						&& t2.typeKind == TypeKind.INT) {
					return this.booleanType;
				}
			} else if (t1 instanceof ArrayType && t2 instanceof ArrayType) {
				Type type1 = ( (ArrayType) t1).eltType;
				Type type2 = ( (ArrayType) t2).eltType;
				if (type1 instanceof BaseType && type2 instanceof BaseType) {
					if (t1.typeKind == TypeKind.INT 
							&& t2.typeKind == TypeKind.INT) {
						return this.intType;
					}
				}
			}
			reportError("CONTEXTUAL ANALYSIS TYPES DOESN'T MATCH",  op);
			return this.errorType;
			// == REQUIRES THEM TO BE SAME TYPE, or can compare against null 
		} else if (op.spelling.equals("==")
				|| op.spelling.equals("!=")) {
			if (t1 instanceof BaseType && t2 instanceof BaseType) {
				if (t1.typeKind == TypeKind.INT 
						&& t2.typeKind == TypeKind.INT) {
					return this.booleanType;
				} else if (t1.typeKind == TypeKind.BOOLEAN 
						&& t2.typeKind == TypeKind.BOOLEAN) {
					return this.booleanType;
				} else if (t1.typeKind == TypeKind.NULL
						&& t2.typeKind == TypeKind.NULL) {
					return this.booleanType;
				}
			} else if (t1 instanceof ClassType && t2 instanceof ClassType) {
				if ( ((ClassType) t1).className.spelling.equals( ((ClassType) t2).className.spelling) ) {
					return this.booleanType;
				} else if (t1.typeKind == TypeKind.NULL
						&& t2.typeKind == TypeKind.NULL) {
					return this.booleanType;
				} else {
					reportError("CONTEXTUAL ANALYSIS TYPES DOESN'T MATCH", op);
					return this.errorType;
				} 
			} else if (t1 instanceof ArrayType && t2 instanceof ArrayType) {
				Type type1 = ( (ArrayType) t1).eltType;
				Type type2 = ( (ArrayType) t2).eltType;
				if (type1 instanceof BaseType && type2 instanceof BaseType) {
					if (t1.typeKind == TypeKind.INT 
							&& t2.typeKind == TypeKind.INT) {
						return this.booleanType;
					} else if (t1.typeKind == TypeKind.BOOLEAN 
							&& t2.typeKind == TypeKind.BOOLEAN) {
						return this.booleanType;
					}
				} else if (t1 instanceof ClassType && t2 instanceof ClassType) {
					if ( ((ClassType) t1).className.spelling.equals( ((ClassType) t2).className.spelling) ) {
						return this.booleanType;
					} else {
						reportError("CONTEXTUAL ANALYSIS TYPES DOESN'T MATCH", op);
						return this.errorType;
					} 
				} else if (t1.typeKind == TypeKind.NULL
						&& t2.typeKind == TypeKind.NULL) {
					return this.booleanType;
				} else {
					reportError("CONTEXTUAL ANALYSIS TYPES DOESN'T MATCH", op);
					return this.errorType;
				}
			} else if (t1.typeKind == TypeKind.NULL
					&& t2.typeKind == TypeKind.NULL) {
				return this.booleanType;
			}
			reportError("CONTEXTUAL ANALYSIS TYPES DOESN'T MATCH", op);
			return this.errorType;
			} else if (op.spelling.equals("||") ||
				op.spelling.equals("&&")) {
				if (t1 instanceof BaseType && t2 instanceof BaseType) {
					if (t1.typeKind == TypeKind.BOOLEAN
							&& t2.typeKind == TypeKind.BOOLEAN) {
						return this.booleanType;
					}
				} else if (t1 instanceof ArrayType && t2 instanceof ArrayType) {
					Type type1 = ( (ArrayType) t1).eltType;
					Type type2 = ( (ArrayType) t2).eltType;
					if (type1 instanceof BaseType && type2 instanceof BaseType) {
						if (t1.typeKind == TypeKind.BOOLEAN 
								&& t2.typeKind == TypeKind.BOOLEAN) {
							return this.booleanType;
						}
					}
				}
				
			}
		reportError("TYPES DOESN'T MATCH, CANNOT PERFORM BINARY EXPR " + op.spelling + " " , op);
		return this.errorType;
	}
	
	private void reportError(String message, AST ast) throws SyntaxError {
		reporter.reportError("*** TYPE CHECKING ", message , "", ast);
		throw new SyntaxError();
	}
}