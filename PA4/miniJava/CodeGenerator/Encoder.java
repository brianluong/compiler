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
		//writeToFileAndRun(fileName.substring(0,fileName.length() - 5) + ".mJAM");
		//writeToFileAndRun("lol");
		writeToFileAndRun(fileName.split("[.]")[0] + ".mJAM");
	}
	
	@Override
	public Integer visitPackage(Package prog, Integer arg) {
		Machine.initCodeGen();
		
		// ALLOCATE MEMORY FOR STATIC FIELDS
		int classOffset_SB = 0;
		boolean packageHasStaticFields = false;
		HashMap<ClassDecl, Integer> classInstanceField = new HashMap<ClassDecl, Integer>();
		for (ClassDecl c: prog.classDeclList) {
			//c.red = new ClassRuntimeEntity(0, 0);
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
			}
			if (classHasStaticFields) {
				c.red = new ClassRuntimeEntity(classOffset_SB, staticFieldDisplacement, instanceFieldDisplacement);
				//classOffset_SB += staticFieldDisplacement;	
			} else {
				c.red = new ClassRuntimeEntity(instanceFieldDisplacement);
			}
			classOffset_SB += staticFieldDisplacement;
		}
		
		if (packageHasStaticFields) {
			Machine.emit(Op.PUSH, classOffset_SB);	
			
		}
		
		// PREAMBLE
		Machine.emit(Op.LOADL,0);
		int patchAddr_Call_main = Machine.nextInstrAddr();
		Machine.emit(Op.CALL, Reg.CB, -1); // MAIN METHOD CALL
		Machine.emit(Op.HALT, 0, 0, 0);
		
		int startOfClassLocation = classOffset_SB;
		
		// VISIT CLASS METHODS AND INSTANCE FIELDS
		for (ClassDecl cd : prog.classDeclList) {
			//startOfClassLocation = cd.visit(this, startOfClassLocation);
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
		
		// NEED SIZE OF INSTANCE VARIABLES (don't need parameters bc they are already on the stack?)
		int startOfClass = arg;
	/*
		int instanceFieldOffset  = 0;
		for (FieldDecl f : cd.fieldDeclList) {
			if (!(f.isStatic)) {
				instanceFieldOffset++;
				f.red = new FieldRuntimeEntity(instanceFieldOffset);
			}
		}
		
		if (instanceFieldOffset > 0) {
			if (cd.red.hasStaticFields) {
				cd.red.setInstanceFieldSize(instanceFieldOffset);
			} else {
				cd.red = new ClassRuntimeEntity(instanceFieldOffset);
			}
		}
		*/
		int startOfMethod = startOfClass;
		for (MethodDecl m: cd.methodDeclList) {
			startOfMethod = m.visit(this, startOfMethod);
		}
		
		// NEED TO ALLOCATE SIZE FOR OB, LB, RA (of the caller)
		
		
		return startOfMethod;
	}
	
	@Override
	public Integer visitFieldDecl(FieldDecl fd, Integer arg) {
		// TODO Auto-generated method stub
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
		
		// HOW MANY PARAMETERS
		int num_Params = md.parameterDeclList.size();
		md.red.addParam(num_Params);
		
		int index_Param = 0;
		if (!isMain(md)) {
			for (ParameterDecl pd : md.parameterDeclList) {
				pd.red = new LocalVariableRuntimeEntity(index_Param + 3);
				Machine.emit(Op.LOAD, Reg.LB, index_Param - num_Params);
				index_Param++;
			}
		}
		// NEED TO RECORD PARAMETER OFFSET IN THE MD RED
		
		// RECORD LOCALS?
		//int start = 3;
		int start = 3 + index_Param;
		
		for (Statement s : md.statementList) {
			start = s.visit(this, start);
		}
	
		if (md.returnExp != null) {
			fetch = true;
			md.returnExp.visit(this, arg);
			fetch = false;
		}
		
		
		// POP NUMBER OF ARGS
		
		
		if (md.type.typeKind.equals(TypeKind.VOID)) {
			Machine.emit(Op.RETURN, 0, 0, index_Param);
		} else {
			Machine.emit(Op.RETURN, 1, 0, index_Param);
		}
		return start;
	}

	@Override
	public Integer visitParameterDecl(ParameterDecl pd, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visitVarDecl(VarDecl decl, Integer arg) { // offset from SB
		// ALLOCATE ONE SPACE TO STORE NEW VAR
		Machine.emit(Op.PUSH, 1);
		decl.red = new LocalVariableRuntimeEntity(arg);
		return null;
	}
	
	@Override
	public Integer visitBaseType(BaseType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Integer visitClassType(ClassType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Integer visitArrayType(ArrayType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Integer visitUnsupportedType(UnsupportedType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Integer visitErrorType(ErrorType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Integer visitBlockStmt(BlockStmt stmt, Integer arg) {
		int initialArg = arg;
		for (Statement s : stmt.sl) {
			arg = s.visit(this, arg);
		}
		// need to pop off var decls (nested local vars)
		Machine.emit(Op.POP, arg - initialArg);
		return initialArg;
	}
	
	@Override
	public Integer visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		// LOCALS BASE 
		// EXAMPLE INT Y = X;
		// DO I NEED TO EVEN CARE ABOUT Y?
		//Integer addr_VarDecl = Machine.nextInstrAddr();
		//System.out.println(addr_VarDecl);
		VarDecl varDecl = stmt.varDecl;
		varDecl.visit(this, arg);
		
		Expression initExp = stmt.initExp;
		fetch = true;
		initExp.visit(this, arg); // fetch
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
		if (decl instanceof VarDecl) {
			// ARRAYS ; ex. a[3] = xxx
			if (stmt.ref instanceof IndexedRef) {
				stmt.ref.visit(this, arg);
				fetch = true;
				stmt.val.visit(this, null); // fetch
				fetch = false;
				Machine.emit(Prim.arrayupd);
			} else {
				LocalVariableRuntimeEntity red = ((VarDecl) stmt.ref.decl).red;
				int disp = red.displacement;
				fetch = true;
				stmt.val.visit(this, null); // fetch
				fetch = false;
				Machine.emit(Op.STORE, Reg.LB, disp);
			}
			return arg;
		
		// INSTANCE OR STATIC VARIABLE
		} else if (decl instanceof FieldDecl) {
			//dont' forget need to do arrays
			// STATIC FIELD
			if (((FieldDecl) decl).isStatic) {
				int d = ((FieldDecl) decl).red.location;
				fetch = true;
				stmt.val.visit(this, null); // fetch
				fetch = false;
				Machine.emit(Op.STORE, Reg.SB, d);
				return arg;
				
			// INSTANCE FIELD
			} else {
				//int disp = ((FieldDecl) decl).red.location;
				Reference ref = stmt.ref;
				ref.visit(this, null); // store, load the address and offset of field
				//stmt.val.visit(this, null); // fetch
				fetch = true;
				stmt.val.visit(this, null);
				fetch = false;
				Machine.emit(Prim.fieldupd);
				return arg;
			}
		}
		return null;
	}
	
	private boolean isFromCallStmt = false;
	@Override
	public Integer visitCallStmt(CallStmt stmt, Integer arg) {
		Reference methodRef = stmt.methodRef;
		//if (methodRef.decl.)
		if (((MethodDecl) stmt.methodRef.decl).red != null &&
				((MethodDecl) stmt.methodRef.decl).red.address == -2) {
			fetch = true;
			stmt.argList.get(0).visit(this, null);
			fetch = false;
			Machine.emit(Prim.putintnl);
		} else {
			// STATIC CALL, INSTANCE
			for (Expression argExpr: stmt.argList){
				argExpr.visit(this, null); 			//put value on the stack
			}
			isFromCallStmt = true;
			methodRef.visit(this, arg);
			isFromCallStmt = false;
			if (!methodIsVoid) {
				Machine.emit(Op.POP, 1);
			}
		}
		return arg;
	}
	
	@Override
	public Integer visitIfStmt(IfStmt stmt, Integer arg) {
		Expression condition = stmt.cond;
		fetch = true;
		condition.visit(this, arg); // fetch 
		fetch = false;
		
		//int c = condition.intValue;
		//Machine.emit(Op.LOADL, c);
		
		int patchBegIf = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);
		
		int d = stmt.thenStmt.visit(this, arg);

		if (stmt.elseStmt != null) { // has else statement 
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
		Expression condition = stmt.cond;
		fetch = true;
		condition.visit(this, arg); // fetch 
		fetch = false;
		
		int patchCond = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 0, Reg.CB, 0); // break out of while loop
		int patchBegWhile = Machine.nextInstrAddr();
		stmt.body.visit(this, arg);
		
		fetch = true;
		condition.visit(this, arg); // fetch 
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
			expr.left.visit(this, null); // automatically a fetch? because like these binary expr... 
			fetch = true;
			expr.right.visit(this, null);
			fetch = false;
			/*int l = expr.left.intValue;
			int r = expr.right.intValue;*/
			
			if (op.spelling.equals("||")){
				/*if ((l == 0 || l == 1) && ( r == 0 || r == 1)) {
					expr.intValue = (l + r) > 0 ? 1 : 0; 
				} else {
					expr.intValue = 0;
				}*/
				Machine.emit(Prim.or);
			} else if (op.spelling.equals("&&")){
				/*if ((l == 0 || l == 1) && ( r == 0 || r == 1)) {
					expr.intValue = (l + r) == 2 ? 1 : 0; 
				} else {
					expr.intValue = 0;
				}*/
				Machine.emit(Prim.and);
			} else if (op.spelling.equals("==")){
				/*if (l == r) {
					expr.intValue = 1;
				} else {
					expr.intValue = 0;
				}*/
				Machine.emit(Prim.eq);
			} else if (op.spelling.equals("!=")){
				/*if (l != r) {
					expr.intValue = 1;
				} else {
					expr.intValue = 0;
				}*/
				Machine.emit(Prim.ne);
			} else if (op.spelling.equals("<=")){
				/*if (l <= r) {
					expr.intValue = 1;
				} else {
					expr.intValue = 0;
				}*/
				Machine.emit(Prim.le);
			} else if (op.spelling.equals("<")){
				/*if (l < r) {
					expr.intValue = 1;
				} else {
					expr.intValue = 0;
				}*/
				Machine.emit(Prim.lt);
			} else if (op.spelling.equals(">=")){
				/*if (l >= r) {
					expr.intValue = 1;
				} else {
					expr.intValue = 0;
				}*/
				Machine.emit(Prim.ge);
			} else if (op.spelling.equals(">")){
				/*if (l > r) {
					expr.intValue = 1;
				} else {
					expr.intValue = 0;
				}*/
				Machine.emit(Prim.gt);
			} else if (op.spelling.equals("+")){
				//expr.intValue = l + r;
				Machine.emit(Prim.add);
			} else if (op.spelling.equals("-")){
				//expr.intValue = l - r;
				Machine.emit(Prim.sub);
			} else if (op.spelling.equals("*")){
				//expr.intValue = l * r;
				Machine.emit(Prim.mult);
			} else if (op.spelling.equals("/")){
				//expr.intValue = l / r;
				Machine.emit(Prim.div);
			}
			else {
				
			}
		}
		
		return arg;
	}

	@Override
	public Integer visitRefExpr(RefExpr expr, Integer arg) {
		// REFERENCE CAN BE A PRIMITIVE OR IF AN OBJECT, A CODE ADDRESS 
		expr.intValue = -1;
		return expr.ref.visit(this, null);
	}

	private boolean isFromMethodCall = false;
	@Override
	public Integer visitCallExpr(CallExpr expr, Integer arg) {
		isFromMethodCall = true;
		for (Expression e: expr.argList) {
			e.visit(this, arg);				// get values on the stack
		}
		expr.functionRef.visit(this, arg);
		isFromMethodCall = false;
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
		
		// no int value?
		Machine.emit(Op.LOADL, sizeFields);
		Machine.emit(Prim.newobj);
		return 1;
	}

	@Override
	public Integer visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
		fetch = true;
		expr.sizeExpr.visit(this, null); 			//push value on its stack
		fetch = false;
		Machine.emit(Prim.newarr);
		return null;
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
									// error 
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
			// DOESN'T WORK
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
			
			// need to fux with thisref?
			
			//MethodRuntimeEntity methodred = ((MethodDecl) identifier.decl).red;
			//int methodoffset = methodred.address;
			
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
					isFromMethodCall = true;
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

	@Override
	public Integer visitIndexedRef(IndexedRef ref, Integer arg) {
		if (fetch) {
			fetch = false;
			ref.ref.visit(this, arg);
			fetch = true;
			ref.indexExpr.visit(this, arg);
			fetch = false;
			Machine.emit(Prim.arrayref);
		} else {
			ref.ref.visit(this, arg);
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
				if (fetch) {
					Machine.emit(Prim.fieldref);
				} else {
					
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
		//if (fetch) {
			Machine.emit(Op.LOADA, Reg.OB, 0);
	//	} else {
			
		//}
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
		String objectCodeFileName = fileName;
		String asmCodeFileName = fileName;
		//String objectCodeFileName = fileName + ".mJAM";
		//String asmCodeFileName = fileName + ".asm";
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
		//debug(objectCodeFileName, asmCodeFileName);
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

}
	