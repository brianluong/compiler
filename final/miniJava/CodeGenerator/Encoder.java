package miniJava.CodeGenerator;

import java.util.ArrayList;
import java.util.HashMap;

import mJAM.*;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import miniJava.ErrorReporter;
import miniJava.SyntaxError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Encoder implements Visitor<Integer, Integer> {

	int main_offset;
	HashMap<Integer, MethodDecl> methodPatches;
	private ErrorReporter reporter;
	private String fileName;
	
	public Encoder() {
		reporter = new ErrorReporter();
	}
	
	public Encoder(String fileName) {
		reporter = new ErrorReporter();
		this.fileName = fileName;
	}
	
	public void encode(AST ast) {
		methodPatches = new HashMap<Integer, MethodDecl>();
		ast.visit(this, null);
		//writeToFileAndRun(fileName.substring(0,fileName.length() - 5) + ".mJAM"); // using tester
		writeToFileAndRun("testing"); // for running 1 test case
		//writeToFileAndRun(fileName.split("[.]")[0] + ".mJAM"); // for submission
	}
	
	@Override
	public Integer visitPackage(Package prog, Integer arg) {
		Machine.initCodeGen();
		
		// CREATE RUNTIME ENTITY DESCRIPTIONS FOR FIELDS AND CLASSES
		int classOffset_SB = 0;
		boolean packageHasStaticFields = false;
		boolean needToInitStaticFields = false;
		for (ClassDecl c: prog.classDeclList) {
			int staticFieldDisplacement = 0;
			int instanceFieldDisplacement = 0;
			boolean classHasStaticFields = false;
			for (FieldDecl f: c.fieldDeclList) {
				if (f.isStatic) {
					f.red = new FieldRuntimeEntity(staticFieldDisplacement + classOffset_SB);
					classHasStaticFields = true;
					packageHasStaticFields = true;
					staticFieldDisplacement++;
				} else {
					f.red = new FieldRuntimeEntity(instanceFieldDisplacement);
					instanceFieldDisplacement++;
				}
				if (f instanceof FieldDeclStmt) {
					needToInitStaticFields = true;
				}
			}
			if (classHasStaticFields) {
				c.red = new ClassRuntimeEntity(classOffset_SB, staticFieldDisplacement, instanceFieldDisplacement);	
			} else {
				c.red = new ClassRuntimeEntity(instanceFieldDisplacement);
			}
			classOffset_SB += staticFieldDisplacement;
		}
		// ALLOCATE ROOM FOR STATIC FIELDS
		if (packageHasStaticFields) {
			Machine.emit(Op.PUSH, classOffset_SB);		
		}
		
		if (needToInitStaticFields) {
			// INITIALIZE STATIC FIELDS
			int initStaticFields = Machine.nextInstrAddr() + 1;
			Machine.emit(Op.CALL, Reg.CB, initStaticFields);
			int initStaticFieldsRA = classOffset_SB + 2;
			for (ClassDecl c: prog.classDeclList) {
				for (FieldDecl f: c.fieldDeclList) {
					if (f instanceof FieldDeclStmt) {
						fetch = true;
						((FieldDeclStmt) f).initExp.visit(this, 3);
						int d = f.red.location;
						Machine.emit(Op.STORE, Reg.SB, d);
					}
				}
			}
			int addr_initStaticFieldsEnd = Machine.nextInstrAddr() + 3;
			Machine.emit(Op.LOADL, addr_initStaticFieldsEnd);
			Machine.emit(Op.STORE,Reg.CB, initStaticFieldsRA );
			Machine.emit(Op.RETURN, 0, 0, 0);
		}
		
		// PREAMBLE
		Machine.emit(Op.LOADL,0);
		int patchAddr_Call_main = Machine.nextInstrAddr();
		// MAIN METHOD CALL
		Machine.emit(Op.CALL, Reg.CB, -1);
		Machine.emit(Op.HALT, 0, 0, 0);
		
		// VISIT CLASS METHODS AND INSTANCE FIELDS
		for (ClassDecl cd : prog.classDeclList) {
			cd.visit(this, cd.red.instanceFieldSize + 3);
		}
		
		// POSTAMBLE, PATCH ALL METHOD CALLS
		Machine.patch(patchAddr_Call_main, main_offset);
		for (int address : methodPatches.keySet()) {
			Machine.patch(address, methodPatches.get(address).red.address);
		}
		return null;
	}

	@Override
	public Integer visitClassDecl(ClassDecl cd, Integer arg) {
		int startOfMethod = arg;
		for (MethodDecl m: cd.methodDeclList) {
			startOfMethod = m.visit(this, startOfMethod);
		}
		return startOfMethod;
	}
	
	@Override
	public Integer visitFieldDecl(FieldDecl fd, Integer arg) {
		return null;
	}
	
	private boolean methodIsVoid = false;
	@Override
	public Integer visitMethodDecl(MethodDecl md, Integer arg) { // arg is address where its being called from?
		// MARK LOCATION OF METHOD 
		Integer codeAddr_Method = Machine.nextInstrAddr(); 
		if (isMain(md)) {
			main_offset = codeAddr_Method; 
		}
		md.red = new MethodRuntimeEntity(codeAddr_Method);
		
		// NUMBER OF PARAMETERS REQUIRED
		int num_Params = md.parameterDeclList.size();
		md.red.addParam(num_Params);
		
		int index_Param = 0;
		if (!isMain(md)) {
			for (ParameterDecl pd : md.parameterDeclList) {
				pd.red = new LocalVariableRuntimeEntity(index_Param + 3); // OFFEST TO ACCESS IN FRAME
				Machine.emit(Op.LOAD, Reg.LB, index_Param - num_Params); // OFFSET TO RETRIEVE ARGUMENTS
				index_Param++;
			}
		}
		
		int start = 3 + index_Param;
		for (Statement s : md.statementList) {
			start = s.visit(this, start);
		}
	
		if (md.returnExp != null) {
			fetch = true;
			md.returnExp.visit(this, arg);
			fetch = false;
		}
		
		// POP ARGUMENTS OFF STACK
		if (md.type.typeKind.equals(TypeKind.VOID) || md.type.typeKind.equals(TypeKind.CONSTRUCTOR)) {
			Machine.emit(Op.RETURN, 0, 0, index_Param);
		} else {
			Machine.emit(Op.RETURN, 1, 0, index_Param);
		}
		return start;
	}

	@Override
	public Integer visitParameterDecl(ParameterDecl pd, Integer arg) {
		return null;
	}

	@Override
	public Integer visitVarDecl(VarDecl decl, Integer arg) { // offset from LB
		// ALLOCATE ONE SPACE TO STORE NEW VAR
		Machine.emit(Op.PUSH, 1);
		decl.red = new LocalVariableRuntimeEntity(arg);
		return null;
	}
	
	@Override
	public Integer visitBaseType(BaseType type, Integer arg) {
		return null;
	}
	
	@Override
	public Integer visitClassType(ClassType type, Integer arg) {
		return null;
	}
	
	@Override
	public Integer visitArrayType(ArrayType type, Integer arg) {
		return null;
	}
	
	@Override
	public Integer visitUnsupportedType(UnsupportedType type, Integer arg) {
		return null;
	}
	
	@Override
	public Integer visitErrorType(ErrorType type, Integer arg) {
		return null;
	}
	
	@Override
	public Integer visitBlockStmt(BlockStmt stmt, Integer arg) {
		int initialArg = arg;
		for (Statement s : stmt.sl) {
			arg = s.visit(this, arg);
		}
		// POP OFF NESTED VARIABLE DECLARATIONS (LOCAL SCOPE)
		Machine.emit(Op.POP, arg - initialArg);
		return initialArg;
	}
	
	@Override
	public Integer visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		stmt.varDecl.visit(this, arg);
		fetch = true;
		stmt.initExp.visit(this, arg);
		fetch = false;
		Machine.emit(Op.STORE, Reg.LB, arg);
		arg++;
		return arg;
	}
	
	private boolean fetch = false;
	
	@Override
	public Integer visitAssignStmt(AssignStmt stmt, Integer arg) {
		Declaration decl = stmt.ref.decl;
		// LOCAL VARIABLE 
		if (decl instanceof VarDecl || decl instanceof ParameterDecl) {
			if (stmt.ref instanceof IndexedRef) {
				stmt.ref.visit(this, arg);
				fetch = true;
				stmt.val.visit(this, null);
				fetch = false;
				Machine.emit(Prim.arrayupd);
			} else {
				LocalVariableRuntimeEntity red = null;
				if (decl instanceof VarDecl) {
					red = ((VarDecl) stmt.ref.decl).red;
				} else {
					red = ((ParameterDecl) stmt.ref.decl).red;
				}
				int disp = red.displacement;
				fetch = true;
				stmt.val.visit(this, null);
				fetch = false;
				Machine.emit(Op.STORE, Reg.LB, disp);
			}
		
		// INSTANCE OR STATIC VARIABLE
		} else if (decl instanceof FieldDecl) {
			// STATIC FIELD
			if (((FieldDecl) decl).isStatic) {
				if (stmt.ref instanceof IndexedRef) {
					stmt.ref.visit(this, arg);
					fetch = true;
					stmt.val.visit(this, null);
					fetch = false;
					Machine.emit(Prim.arrayupd);
				} else {
					int d = ((FieldDecl) decl).red.location;
					fetch = true;
					stmt.val.visit(this, null);
					fetch = false;
					Machine.emit(Op.STORE, Reg.SB, d);
				}
				
			// INSTANCE FIELD
			} else {
				stmt.ref.visit(this, null);
				fetch = true;
				stmt.val.visit(this, null);
				fetch = false;
				if (stmt.ref instanceof IndexedRef) {
					Machine.emit(Prim.arrayupd);
				} else {
					Machine.emit(Prim.fieldupd);	
				}
			}
		}
		return arg;
	}
	
	@Override
	public Integer visitCallStmt(CallStmt stmt, Integer arg) {
		Reference methodRef = stmt.methodRef;
		if (((MethodDecl) stmt.methodRef.decl).red != null &&
				((MethodDecl) stmt.methodRef.decl).red.address == -2) {
			// (HARDCODED) CHECKING IF ITS PREFINED SYSTEM.OUT.PRINTLN
			fetch = true;
			stmt.argList.get(0).visit(this, null);
			fetch = false;
			Machine.emit(Prim.putintnl);
		} else {
			// STATIC CALL or INSTANCE CALL
			for (Expression argExpr: stmt.argList){
				fetch = true;
				argExpr.visit(this, null); 
				fetch = false;
			}
			methodRef.visit(this, arg);
			if (!methodIsVoid) {
				// POP RETURN VALUE BECAUSE IT ISN'T BOUND TO ANY REFERENCE
				Machine.emit(Op.POP, 1);
			}
		}
		return arg;
	}
	
	@Override
	public Integer visitIfStmt(IfStmt stmt, Integer arg) {
		fetch = true;
		stmt.cond.visit(this, arg); 
		fetch = false;
		
		int patchBegIf = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);
		int d = stmt.thenStmt.visit(this, arg);

		if (stmt.elseStmt != null) { 
			int patchEndIf = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, 0);
			int patchStartElse = Machine.nextInstrAddr();
			d = stmt.elseStmt.visit(this, d);
			Machine.patch(patchBegIf, patchStartElse);
			int patchEndElse = Machine.nextInstrAddr();
			Machine.patch(patchEndIf, patchEndElse);
		} else {
			int endInstr = Machine.nextInstrAddr();
			Machine.patch(patchBegIf, endInstr);
		}
		return d;
	}

	@Override
	public Integer visitWhileStmt(WhileStmt stmt, Integer arg) {
		
		fetch = true;
		stmt.cond.visit(this, arg); 
		fetch = false;
		
		int patchCond = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);
		int patchBegWhile = Machine.nextInstrAddr();
		stmt.body.visit(this, arg);
		
		fetch = true;
		stmt.cond.visit(this, arg); 
		fetch = false;
	
		int patchEndWhile = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 1, Reg.CB, 0);
		Machine.patch(patchEndWhile, patchBegWhile);
		Machine.patch(patchCond, Machine.nextInstrAddr());
		return arg;
	}
	
	@Override
	public Integer visitUnaryExpr(UnaryExpr expr, Integer arg) {
		Operator op = expr.operator;
		fetch = true;
		expr.expr.visit(this, arg);
		fetch = false;
		if (op.spelling.equals("-")) {
			Machine.emit(Prim.neg);
		} else if (op.spelling.equals("!")) {
			Machine.emit(Prim.not);
		}
		return arg;
	}

	@Override
	public Integer visitBinaryExpr(BinaryExpr expr, Integer arg) {
		Operator op = expr.operator;
		if (op.spelling.equals("&&")) {
			expr.left.visit(this, null);
			int patchShortCircuitMiddle = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, 0, Reg.CB, 0); 
			expr.right.visit(this, null);
			int patchShortCircuitEnd = Machine.nextInstrAddr();
			//Machine.emit(Op.JUMPIF, 0, Reg.CB, 0); 
			Machine.patch(patchShortCircuitMiddle, patchShortCircuitEnd);
		} else {
			fetch = true;
			expr.left.visit(this, null); 
			fetch = true;
			expr.right.visit(this, null);
			fetch = false;
			
			if (op.spelling.equals("||")){
				Machine.emit(Prim.or);
			} else if (op.spelling.equals("&&")){
				Machine.emit(Prim.and);
			} else if (op.spelling.equals("==")){
				Machine.emit(Prim.eq);
			} else if (op.spelling.equals("!=")){
				Machine.emit(Prim.ne);
			} else if (op.spelling.equals("<=")){
				Machine.emit(Prim.le);
			} else if (op.spelling.equals("<")){
				Machine.emit(Prim.lt);
			} else if (op.spelling.equals(">=")){
				Machine.emit(Prim.ge);
			} else if (op.spelling.equals(">")){
				Machine.emit(Prim.gt);
			} else if (op.spelling.equals("+")){
				Machine.emit(Prim.add);
			} else if (op.spelling.equals("-")){
				Machine.emit(Prim.sub);
			} else if (op.spelling.equals("*")){
				Machine.emit(Prim.mult);
			} else if (op.spelling.equals("/")){
				Machine.emit(Prim.div);
			} else {
				reportError("invalid operator found " + " op.spelling", op);
			}
		}
		return arg;
	}

	@Override
	public Integer visitRefExpr(RefExpr expr, Integer arg) {
		return expr.ref.visit(this, null);
	}

	@Override
	public Integer visitCallExpr(CallExpr expr, Integer arg) {
		for (Expression e: expr.argList) {
			fetch = true;
			e.visit(this, arg);
			fetch = false;
		}
		expr.functionRef.visit(this, arg);
		return null;
	}

	@Override
	public Integer visitLiteralExpr(LiteralExpr expr, Integer arg) {
		if (expr.lit instanceof IntLiteral) {
			int lit = expr.lit.visit(this, null);
			expr.intValue = lit;
			Machine.emit(Op.LOADL, lit);
		} else if (expr.lit instanceof BooleanLiteral) {
			int bool = expr.lit.visit(this, null);
			expr.intValue = bool;
			Machine.emit(Op.LOADL, bool);
		} else if (expr.lit instanceof NullLiteral) {
			int nullLiteral = expr.lit.visit(this, null);
			expr.intValue = 0;
			Machine.emit(Op.LOADL, nullLiteral);
		}
		return 1;
	}

	@Override
	public Integer visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
		Machine.emit(Op.LOADL, -1); // class descriptor?
	
		// number of fields in that class
		ClassDecl classDeclaration = (ClassDecl) expr.classtype.className.decl;
		int sizeFields = classDeclaration.red.instanceFieldSize;
		
		Machine.emit(Op.LOADL, sizeFields);
		Machine.emit(Prim.newobj);
		
		// constructor, need to check if it has one
		if (expr.argList != null) {
			for (Expression e : expr.argList) {
				fetch = true;
				e.visit(this, arg);
				fetch = false;
			}	
			Machine.emit(Op.LOAD, Reg.LB, arg + 1);
			methodPatches.put(Machine.nextInstrAddr(), expr.constructor);
			Machine.emit(Op.CALLI, Reg.CB, -1);
		}
		return arg;
	}

	@Override
	public Integer visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
		fetch = true;
		expr.sizeExpr.visit(this, null); 			//push value on its stack
		fetch = false;
		Machine.emit(Prim.newarr);
		return arg;
	}


	private boolean baseCaseQualifiedRef = false;
	private boolean isFromQualified = false;
	private boolean justGetAddress = false;
	private int recursionCount = 0;
	@Override
	// FOR STORE
	public Integer visitQualifiedRef(QualifiedRef ref, Integer arg) {
		// EX. a.n
		Identifier identifier = ref.id; // the .n.n.n.X
		Reference qualifiedRef = ref.ref;
		
		isFromQualified = true;
		if (!(qualifiedRef instanceof QualifiedRef)) {
			baseCaseQualifiedRef = true;
		} else {
			justGetAddress = true;
		}
		Declaration decl = identifier.decl;
		
		if (decl instanceof ParameterDecl) {
			
		} else if (decl instanceof VarDecl) {
			
		} else if (decl instanceof FieldDecl) { // field 
			// either static or instance access
			// need address of a (id)
			
			FieldRuntimeEntity red = ((FieldDecl) identifier.decl).red;  // offset within instance
			int fieldOffset = red.location;
			
			// MUST BE this.X;
			if (qualifiedRef instanceof ThisRef) {
				
				qualifiedRef.visit(this, arg);
				Machine.emit(Op.LOADL, Reg.LB, fieldOffset);
				if (fetch) {
					Machine.emit(Prim.fieldref);
				} 
				baseCaseQualifiedRef = false;
			// NEED TO ALSO DO MORE IF DECL IS METHOD 
			} else if (qualifiedRef.decl instanceof FieldDecl) {
				if (baseCaseQualifiedRef) {
					if (justGetAddress) {
						FieldRuntimeEntity red2 = ((FieldDecl) qualifiedRef.decl).red;
						Machine.emit(Op.LOAD, Reg.OB, red2.location);
						Machine.emit(Op.LOADL, fieldOffset);
						Machine.emit(Prim.add);
						Machine.emit(Op.LOADI);
						//Machine.emit(Op.LOAD, Reg.OB, 0);
						//Machine.emit(Op.LOADL, Reg.LB, fieldOffset);
						//Machine.emit(Prim.fieldref);
						//Machine.emit(Op.LOAD, Reg.LB, red2.location + 1);
					} else {
						if (((FieldDecl) qualifiedRef.decl).type.typeKind == TypeKind.ARRAY) {
							if (identifier.spelling.equals("length")) {
								if (fetch) {
									FieldRuntimeEntity localDisp = ((FieldDecl) qualifiedRef.decl).red;
									Machine.emit(Op.LOAD, Reg.OB, localDisp.location); // get address
									Machine.emit(Op.LOADL, -1);
									Machine.emit(Prim.add);
									// only can fetch these
									Machine.emit(Op.LOADI);
								} else {
									// can't assign length field
									reportError("can't use array's length field to assign", ref);
								}
							} else {
								reportError("invalid field for arrays", ref);
							} 
						} else {
							FieldRuntimeEntity localDisp = ((FieldDecl) qualifiedRef.decl).red;
							//Machine.emit(Op.LOAD, Reg.LB, localDisp.location); // get address
							Machine.emit(Op.LOAD, Reg.OB, localDisp.location);
							//Machine.emit(Op.LOADA, Reg.OB, fieldOffset);
							Machine.emit(Op.LOADL, Reg.LB, fieldOffset);
							if (fetch == true) {
								Machine.emit(Prim.fieldref);
							}
						}
					}
					justGetAddress = false;
					baseCaseQualifiedRef = false;
				} else {
					recursionCount++;
					ref.ref.visit(this, arg);
					recursionCount--;
					//Machine.emit(Op.LOADL, Reg.OB, fieldOffset);
					Machine.emit(Op.LOADL, fieldOffset);
					if (recursionCount > 0) { 		// need to find addresses 
						Machine.emit(Prim.fieldref);
					} else {
						if (fetch == true) {
							Machine.emit(Prim.fieldref);
					}
					}
				}
			} else if (qualifiedRef.decl instanceof VarDecl) {
				if (baseCaseQualifiedRef) {
					if (justGetAddress) {
						LocalVariableRuntimeEntity red2 = ((VarDecl) qualifiedRef.decl).red;
						Machine.emit(Op.LOAD, Reg.LB, red2.displacement);
						Machine.emit(Op.LOADL, Reg.LB, fieldOffset);
						Machine.emit(Prim.fieldref);
						//Machine.emit(Op.LOADI, Reg.LB, red2.displacement + 1);
					} else {
						if (((VarDecl) qualifiedRef.decl).type.typeKind == TypeKind.ARRAY) {
							if (identifier.spelling.equals("length")) {
								if (fetch) {
									LocalVariableRuntimeEntity localDisp = ((VarDecl) qualifiedRef.decl).red;
									Machine.emit(Op.LOAD, Reg.LB, localDisp.displacement); // get address
									Machine.emit(Op.LOADL, -1);
									Machine.emit(Prim.add);
									// only can fetch these
									Machine.emit(Op.LOADI);
								} else {
									// can't assign length field
									reportError("can't use array's length field to assign", ref);
								}
							} else {
								// error 
								reportError("invalid field for arrays", ref);
							}
						} else {
							LocalVariableRuntimeEntity localDisp = ((VarDecl) qualifiedRef.decl).red;
							Machine.emit(Op.LOAD, Reg.LB, localDisp.displacement); // get address
							Machine.emit(Op.LOADL, Reg.LB, fieldOffset);
							if (fetch == true) {
								Machine.emit(Prim.fieldref);
							}
						}
					}
					baseCaseQualifiedRef = false;
					justGetAddress = false;
				} else {
					ref.ref.visit(this, arg);
					Machine.emit(Op.LOADL, Reg.LB, fieldOffset);
					if (fetch == true) {
						Machine.emit(Prim.fieldref);
					}
				}
				
			} else if (qualifiedRef.decl instanceof ParameterDecl) {
				// param.X
				if (baseCaseQualifiedRef) {
					if (justGetAddress) {
						LocalVariableRuntimeEntity red2 = ((ParameterDecl) qualifiedRef.decl).red;
						//int d = red2.displacement + 3;
						Machine.emit(Op.LOAD, Reg.LB, red2.displacement);	
						Machine.emit(Op.LOADL, Reg.LB, fieldOffset);
						Machine.emit(Prim.fieldref);
					} else {
							if (((ParameterDecl) qualifiedRef.decl).type.typeKind == TypeKind.ARRAY) {
								if (identifier.spelling.equals("length")) {
									if (fetch) {
										LocalVariableRuntimeEntity localDisp = ((ParameterDecl) qualifiedRef.decl).red;
										Machine.emit(Op.LOAD, Reg.LB, localDisp.displacement); // get address
										Machine.emit(Op.LOADL, -1);
										Machine.emit(Prim.add);
										// only can fetch these
										Machine.emit(Op.LOADI);
									} else {
										// can't assign length field
										reportError("can't use array's length field to assign", ref);
									}
								} else {
									reportError("invalid field for arrays", ref);
								}
							} else {
								LocalVariableRuntimeEntity localDisp = ((ParameterDecl) qualifiedRef.decl).red;
								Machine.emit(Op.LOAD, Reg.LB, localDisp.displacement); // get address
								Machine.emit(Op.LOADL, Reg.LB, fieldOffset);
								if (fetch == true) {
									Machine.emit(Prim.fieldref);
								}
							}
					}
				} else {
					// it has to be base case? 
					reportError("cannot reach?", ref);
				}
			
			// static
			// DOESN'T WORK?
			} else if (qualifiedRef.decl instanceof ClassDecl) {
				//Machine.emit(Op.LOADL, 0); // get address
				Machine.emit(Op.LOADL, fieldOffset);
				if (fetch) {
					Machine.emit(Op.LOADI);
					return arg;
				}
				
			}
			baseCaseQualifiedRef = false;
			return arg;
		} else if (decl instanceof MethodDecl) {
			
			if (qualifiedRef instanceof ThisRef) {
				qualifiedRef.visit(this, arg);
				// just an instance method call 
				methodPatches.put(Machine.nextInstrAddr(), (MethodDecl) decl);
				Machine.emit(Op.CALLI, Reg.CB, -1);
				if (((MethodDecl) decl).type.typeKind == TypeKind.VOID) {
					methodIsVoid = true;
				} else {
					methodIsVoid = false;
				}
				
			// NEED TO ALSO DO MORE IF DECL IS METHOD 
			} else if (qualifiedRef.decl instanceof VarDecl) {
				if (baseCaseQualifiedRef) {
					// need address of instance
					LocalVariableRuntimeEntity localDisp = ((VarDecl) qualifiedRef.decl).red;
					Machine.emit(Op.LOAD, Reg.LB, localDisp.displacement); // get address
					methodPatches.put(Machine.nextInstrAddr(), (MethodDecl) decl);
					Machine.emit(Op.CALLI, Reg.CB, -1); // patch it
					if (((MethodDecl) decl).type.typeKind == TypeKind.VOID) {
						methodIsVoid = true;
					} else {
						methodIsVoid = false;
					}
				}
			} else if (qualifiedRef.decl instanceof FieldDecl){
				// need address...
				if (baseCaseQualifiedRef) {
					
				} else {
					justGetAddress = true;
					ref.ref.visit(this, arg); // recursion
					methodPatches.put(Machine.nextInstrAddr(), (MethodDecl) decl);
					Machine.emit(Op.CALLI, Reg.CB, -1); // patch it
					if (((MethodDecl) decl).type.typeKind == TypeKind.VOID) {
						methodIsVoid = true;
					} else {
						methodIsVoid = false;
					}
					//Machine.emit(Op.LOAD, Reg.LB, -1); // get address
					//Machine.emit(Op.LOADL, Reg.LB, fieldOffset);
					/*if (fetch == true) {
						Machine.emit(Prim.fieldref);
					}*/
				}
				
				// STATIC METHOD CALL 
			} else if (qualifiedRef.decl instanceof MethodDecl) {
				// can't do qualified nested calls
			} else if (qualifiedRef.decl instanceof ClassDecl) {
				// should already be base case qualified ref...
				if (baseCaseQualifiedRef) {
					methodPatches.put(Machine.nextInstrAddr(), (MethodDecl) decl);
					Machine.emit(Op.CALL, Reg.CB, -1); // patch it
					if (((MethodDecl) decl).type.typeKind == TypeKind.VOID) {
						methodIsVoid = true;
					} else {
						methodIsVoid = false;
					}
				}
			}
			baseCaseQualifiedRef = false;
		}
		return null;
	}

	private boolean fromIndexedRef = false;
	@Override
	public Integer visitIndexedRef(IndexedRef ref, Integer arg) {
		if (fetch) {
			fetch = false;
			fromIndexedRef = true;
			ref.ref.visit(this, arg);
			fromIndexedRef = false;
			fetch = true;
			ref.indexExpr.visit(this, arg);
			fetch = false;
			Machine.emit(Prim.arrayref);
		} else {
			fromIndexedRef = true;
			ref.ref.visit(this, arg);
			fromIndexedRef = false;
			fetch = true;
			ref.indexExpr.visit(this, arg);
			fetch = false;
		}
		return null;
	}

	@Override
	public Integer visitIdRef(IdRef ref, Integer arg) {
		// REF CAN BE LOCALDECL (PARAM & VAR DECL), FIELD DECL, CLASS DECL, METHOD DECL
		Declaration decl = ref.decl;
		int d = 0;
		// PARAMETER OR VAR DECL
		// BASED ON LB
		if (decl instanceof LocalDecl) {
			// need both var and param for type casting 
			if (decl instanceof VarDecl) {
				d = ((VarDecl) decl).red.displacement;
			} else if (decl instanceof ParameterDecl) {
				d = ((ParameterDecl) decl).red.displacement;
			}
			Machine.emit(Op.LOAD, Reg.LB, d);
		} else if (decl instanceof FieldDecl) {
			if (((FieldDecl) decl).isStatic) {
				d = ((FieldDecl) decl).red.location;
				if (fetch) {
					Machine.emit(Op.LOADL, d);
					Machine.emit(Op.LOADI);
				} else {
					Machine.emit(Op.LOADL, d);
				}
			} else {
				// need offset and address
				d = ((FieldDecl) decl).red.location;
				Machine.emit(Op.LOADA, Reg.OB, 0);
				Machine.emit(Op.LOADL, d);
			
					if (fetch || fromIndexedRef) {
						Machine.emit(Prim.fieldref);
					}
				
			}
		} else if (decl instanceof ClassDecl) {
			
		} else if (decl instanceof MethodDecl) {
			if (((MethodDecl) decl).isStatic) {
				methodPatches.put(Machine.nextInstrAddr(), (MethodDecl) decl);
				Machine.emit(Op.CALL, -1);
				if (((MethodDecl) decl).type.typeKind == TypeKind.VOID) {
					methodIsVoid = true;
				} else {
					methodIsVoid = false;
				}
			} else {
				Machine.emit(Op.LOADA, Reg.OB, 0); // does this get the address for this?
				//Machine.emit(Op.LOAD, Reg.LB, 0); // get address for THIS
				methodPatches.put(Machine.nextInstrAddr(), (MethodDecl) decl);
				Machine.emit(Op.CALLI, Reg.CB, -1); // patch it
				if (((MethodDecl) decl).type.typeKind == TypeKind.VOID) {
					methodIsVoid = true;
				} else {
					methodIsVoid = false;
				}
			}
		} else {
			
		}
		return arg;
	}

	@Override
	public Integer visitThisRef(ThisRef ref, Integer arg) {
		Machine.emit(Op.LOADA, Reg.OB, 0);
		return null;
	}

	@Override
	public Integer visitIdentifier(Identifier id, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visitOperator(Operator op, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visitIntLiteral(IntLiteral num, Integer arg) {
		return Integer.parseInt(num.spelling.toString());
	}

	@Override
	public Integer visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
		return bool.spelling.equals("true") ? 1 : 0;
	}

	@Override
	public Integer visitNullLiteral(NullLiteral nullLiteral, Integer arg) {
		return 0;
	}	
	
	public boolean isMain(MethodDecl md) {
		if (md.name.equals("main")) {
			return true;
		} else {
			return false;	
		}
	}
	
	private void writeToFileAndRun(String fileName) {
		//String objectCodeFileName = fileName;
		//String asmCodeFileName = fileName;
		String objectCodeFileName = fileName + ".mJAM";
		String asmCodeFileName = fileName + ".asm";
		ObjectFile objF = new ObjectFile(objectCodeFileName);
		System.out.print("Writing object code file " + objectCodeFileName + " ... ");
		if (objF.write()) {
			System.out.println("FAILED!");
			return;
		} else {
			System.out.println("SUCCEEDED");
			/* create asm file using disassembler */
			System.out.print("Writing assembly file ... " + asmCodeFileName);
			Disassembler d = new Disassembler(objectCodeFileName);
			if (d.disassemble()) {
				System.out.println("FAILED!");
				return;
			} else {
				System.out.println("SUCCEEDED");
			}
		}
		//Interpreter.interpret(objectCodeFileName);
		debug(objectCodeFileName, asmCodeFileName);
	}
	
	private void debug(String objectCodeFileName, String asmFileName) {
		System.out.println("Running code ... ");
		Interpreter.debug(objectCodeFileName, asmFileName);
		System.out.println("*** mJAM execution completed");
	}
	
	private void reportError(String message, AST ast) throws SyntaxError {
		reporter.reportError("*** CODE GENERATION ", message , "", ast.posn);
		throw new SyntaxError();
	}

	@Override
	public Integer visitFielddeclStmt(FieldDeclStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}
}