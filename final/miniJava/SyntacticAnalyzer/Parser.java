package miniJava.SyntacticAnalyzer;


import java.util.ArrayList;

import miniJava.ErrorReporter;
import miniJava.SyntaxError;
import miniJava.AbstractSyntaxTrees.*;

public class Parser {
	private Scanner scanner;
	private Token currentToken;
	private ErrorReporter errorReporter;
	
	public Parser(Scanner scanner) {
		this.scanner = scanner;
		this.errorReporter = new ErrorReporter();
	}
	
	public ArrayList<Token> scanAll() {
		ArrayList<Token> tokens = new ArrayList<Token>();
		currentToken = scanner.scan();
		tokens.add(currentToken);
		while (currentToken.kindInt != Token.EOT) {
			currentToken = scanner.scan();
			tokens.add(currentToken);
		}
		return tokens;
	}
	
	public AST parse() {
		currentToken = scanner.scan();
		miniJava.AbstractSyntaxTrees.Package theAST = null;
		try {
			theAST = parseProgram();
			//printSuccessMessage();
		}
		catch (SyntaxError e) {
			throw new SyntaxError();
		}
		return theAST;
	}
	/*
	private void printSuccessMessage() {
		System.out.println(MSG_PROGRAM_SUCCESS_PARSE);
	}
	*/
	
	private void acceptIt() {
		currentToken = scanner.scan();
		if (currentToken.kindInt == Token.ERROR) {
			parseError("Found an Error Token");
		}
	}
	
	private void accept(int expectedKind) {
		if (currentToken.kindInt != expectedKind ) {
			parseError(Token.getTokenName(expectedKind), 
					Token.getTokenName(currentToken.kindInt));
		} else {
			acceptIt();
		}
	}

	private miniJava.AbstractSyntaxTrees.Package parseProgram() throws SyntaxError {
		miniJava.AbstractSyntaxTrees.Package programAST = null;
		ClassDeclList classDeclList = new ClassDeclList();
		while (currentToken.kindInt == Token.CLASS) {
			classDeclList.add(parseClassDecl());
		}
		accept(Token.EOT);
		programAST = new miniJava.AbstractSyntaxTrees.Package(classDeclList, currentToken.posn);
		return programAST;
	}
	
	private boolean isParseConstructor = false;
	private Identifier idForConstructor = null;
	private ClassDecl parseClassDecl() throws SyntaxError {
		ClassDecl classDecl = null;
		accept(Token.CLASS);
		String cn = currentToken.spelling;
		accept(Token.ID);
		accept(Token.LCURLY);
		FieldDeclList fieldDeclList = new FieldDeclList();
		MethodDeclList methodDeclList = new MethodDeclList();
		
		while (currentToken.kindInt == Token.PUBLIC 
				|| currentToken.kindInt == Token.PRIVATE
				|| currentToken.kindInt == Token.STATIC
				|| currentToken.kindInt == Token.VOID
				|| currentToken.kindInt == Token.ID
				|| currentToken.kindInt == Token.INT
				|| currentToken.kindInt == Token.BOOLEAN) {
			
			boolean canBeConstructor = false;
			boolean isPrivate = false, isStatic = false;
			if (currentToken.kindInt == Token.PUBLIC ||
					currentToken.kindInt == Token.PRIVATE) {
				if (currentToken.kindInt == Token.PUBLIC) {
					isPrivate = false;
 				} else {
 					isPrivate = true;
 				}
				canBeConstructor = true;
				acceptIt();		
			}

			if (currentToken.kindInt == Token.STATIC) {
				isStatic = true;
				acceptIt();
				canBeConstructor = false;
			}
			
			Type type = null;
			boolean isVoid = false;
			String spelling = null;
			// PARSE RETURN TYPE
			switch (currentToken.kindInt) {
			
			case Token.ID: case Token.INT:
			case Token.BOOLEAN:
				if (currentToken.kindInt == Token.ID) {
					spelling = currentToken.spelling;
					canBeConstructor = true;	
				} else {
					canBeConstructor = false;
				}
				type = parseType();
				break;
			
			case Token.VOID:
				type = new BaseType(TypeKind.VOID, currentToken.posn);
				acceptIt();
				isVoid = true;
				canBeConstructor = false;
				break;
			
			default:
				parseError("id, int, or void type", currentToken.spelling);
			}
			
			MemberDecl fieldDecl = null;
			// PARSE CLASS CONSTRUCTOR
			if (canBeConstructor && currentToken.kindInt == Token.LPAREN) {
				Identifier id = new Identifier(currentToken);
				idForConstructor = id;
				type = new ConstructorType(id, currentToken.posn);
				fieldDecl = new FieldDecl(isPrivate, false, type, spelling,currentToken.posn);
				isParseConstructor = true;
				MethodDecl methodDecl = parseMethodDecl(fieldDecl);
				isParseConstructor = false;
				methodDeclList.add(methodDecl);
			} else {
				spelling = currentToken.spelling;
				accept(Token.ID);
				fieldDecl = new FieldDecl(isPrivate, isStatic, type, spelling, currentToken.posn);
				switch(currentToken.kindInt) {
				
				// (STATIC | INSTANCE) (UNINITIALIZED) FIELD
				case Token.SEMICOLON:
					if (isVoid) {
						parseError("id, int, or boolean", "void");
					}
					fieldDeclList.add((FieldDecl) fieldDecl);
					acceptIt();
					break;
					
				// METHOD DECLARATION
				case Token.LPAREN:
					MethodDecl methodDecl = parseMethodDecl(fieldDecl);
					methodDeclList.add(methodDecl);
					break;
					
				// (STATIC | INSTANCE) FIELD INITIALIZATION 
				case Token.ASSIGNMENT: 			
					if (isVoid) {
						parseError("id, int, or boolean", "void");
					}
					if (!isStatic) {
						parseError("This compiler does not support instance field initialization.");
					}
					acceptIt();
					Expression e = parseExpression();
					accept(Token.SEMICOLON);
					FieldDeclStmt fieldDeclStmt = new FieldDeclStmt(isPrivate, isStatic, 
							type, spelling, e, currentToken.posn);
					fieldDeclList.add((FieldDecl) fieldDeclStmt);
					break;
					
				default:
					parseError("; ( or =", currentToken.spelling);
				}
			}
		} 
		accept(Token.RCURLY);
		classDecl = new ClassDecl(cn, fieldDeclList, methodDeclList, currentToken.posn);
		return classDecl;
	}
	
	private MethodDecl parseMethodDecl(MemberDecl fieldDecl) throws SyntaxError  {
		MethodDecl methodDecl = null;
		MemberDecl MethodFieldDecl = fieldDecl;
		ParameterDeclList parameterListDecl = new ParameterDeclList();
		StatementList statementList = new StatementList();
		Expression expression = null;
	
		accept(Token.LPAREN);		
		switch(currentToken.kindInt) {
		
		case Token.RPAREN:
			acceptIt();
			break;
			
		case Token.ID: case Token.INT:
		case Token.BOOLEAN: 
			parameterListDecl = parseParamList(parameterListDecl);
			accept(Token.RPAREN);
			break;
		
		default:
			parseError("(, id, int, or boolean", currentToken.spelling);
		}
		accept(Token.LCURLY);
		
		while (currentToken.kindInt == Token.LCURLY
				|| currentToken.kindInt == Token.THIS
				|| currentToken.kindInt == Token.ID
				|| currentToken.kindInt == Token.INT
				|| currentToken.kindInt == Token.IF
				|| currentToken.kindInt == Token.WHILE
				|| currentToken.kindInt == Token.BOOLEAN) {
			Statement statement = parseStatement();
			statementList.add(statement);
		}
		if (currentToken.kindInt == Token.RETURN) {
			if (isParseConstructor) {
				parseError("constructors cannot have return statements");
			}
			acceptIt();
			expression = parseExpression();
			accept(Token.SEMICOLON);
		}
		accept(Token.RCURLY);
		if (isParseConstructor) {
			methodDecl = new ConstructorMethodDecl(idForConstructor, MethodFieldDecl, parameterListDecl, 
					statementList, expression, currentToken.posn);
		} else {
		methodDecl = new MethodDecl(MethodFieldDecl, parameterListDecl,
				statementList, expression, currentToken.posn);	
		}
		return methodDecl;	
	}
	
	private Type parseType() throws SyntaxError {
		Type type = null; 
		switch(currentToken.kindInt) {
	
		case Token.BOOLEAN:	
			type = new BaseType(TypeKind.BOOLEAN, currentToken.posn);
			acceptIt();
			break;
			
		case Token.INT: case Token.ID:
			if (currentToken.kindInt == Token.INT) {
				type = new BaseType(TypeKind.INT,currentToken.posn);
			}
			if (currentToken.kindInt == Token.ID) {
				Identifier id = new Identifier(currentToken);
				type = new ClassType(id, currentToken.posn);
			}
 			acceptIt();
			if (currentToken.kindInt == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
				type = new ArrayType(type, currentToken.posn);
			}
			break;
		
		default:
			parseError("boolean, int, or id", currentToken.spelling);
		}
		return type;
	}
	
	private ParameterDeclList parseParamList(ParameterDeclList parameterDeclList) throws SyntaxError{
		Type type = parseType();
		String spelling = currentToken.spelling;
		accept(Token.ID);
		ParameterDecl parameterDecl = new ParameterDecl(type, spelling, currentToken.posn);
		parameterDeclList.add(parameterDecl);
		
		while (currentToken.kindInt ==  Token.COMMA) {
			acceptIt();
			type = parseType();
			spelling = currentToken.spelling;
			accept(Token.ID);
			parameterDecl = new ParameterDecl(type, spelling, currentToken.posn);
			parameterDeclList.add(parameterDecl);
		}
		return parameterDeclList;
	}
	
	private ExprList parseArgList() throws SyntaxError {
		ExprList exprList = new ExprList();
		Expression expression = parseExpression();
		exprList.add(expression);
		while (currentToken.kindInt == Token.COMMA) {
			acceptIt();
			expression = parseExpression();
			exprList.add(expression);
		}
		return exprList;
	}
	
	private Statement parseStatement() throws SyntaxError {
		Statement statement = null;
		Expression expression = null;
		
		switch(currentToken.kindInt) {
		
		case Token.LCURLY:
			acceptIt();
			StatementList statementList = new StatementList();
			while (currentToken.kindInt == Token.INT
					|| currentToken.kindInt == Token.BOOLEAN
					|| currentToken.kindInt == Token.ID
					|| currentToken.kindInt == Token.IF
					|| currentToken.kindInt == Token.WHILE
					|| currentToken.kindInt == Token.THIS 
					|| currentToken.kindInt == Token.LCURLY) {
				statementList.add(parseStatement());
			}
			accept(Token.RCURLY);
			statement = new BlockStmt(statementList, currentToken.posn);
			break;
		
		case Token.IF:
			acceptIt();
			accept(Token.LPAREN);
			expression = parseExpression();
			accept(Token.RPAREN);
			Statement statement1 = parseStatement();
			if (currentToken.kindInt == Token.ELSE) {
				acceptIt();
				Statement statement2 = parseStatement();
				statement = new IfStmt(expression, statement1, statement2, currentToken.posn);
			} else {
				statement = new IfStmt(expression, statement1, currentToken.posn);
			}
			break;
		
		case Token.WHILE:
			acceptIt();
			accept(Token.LPAREN);
			expression = parseExpression();
			accept(Token.RPAREN);
			Statement statement2 = parseStatement();
			statement = new WhileStmt(expression, statement2, currentToken.posn);
			break;
			
		case Token.BOOLEAN:
			acceptIt();
			String spelling = currentToken.spelling;
			accept(Token.ID);
			VarDecl varDecl = new VarDecl(new BaseType(TypeKind.BOOLEAN, currentToken.posn), 
					spelling, currentToken.posn);
			accept(Token.ASSIGNMENT);
			expression = parseExpression();
			accept(Token.SEMICOLON);
			statement = new VarDeclStmt(varDecl, expression, currentToken.posn);
			break;
			
		case Token.INT:
			acceptIt();
			
			switch(currentToken.kindInt) {
			
			case Token.ID:
				spelling = currentToken.spelling;
				//Identifier id = new Identifier(currentToken);
				acceptIt();
				varDecl = new VarDecl(new BaseType(TypeKind.INT, currentToken.posn), spelling, currentToken.posn);
				accept(Token.ASSIGNMENT);
				expression = parseExpression();
				accept(Token.SEMICOLON);
				statement = new VarDeclStmt(varDecl, expression, currentToken.posn);
				break;
			
			case Token.LBRACKET:
				acceptIt();
				accept(Token.RBRACKET);
				spelling = currentToken.spelling;
				Type type = new BaseType(TypeKind.INT, currentToken.posn);
				varDecl = new VarDecl( new ArrayType(type, currentToken.posn), spelling, currentToken.posn);
				accept(Token.ID);
				accept(Token.ASSIGNMENT);
				expression = parseExpression();
				accept(Token.SEMICOLON);
				statement = new VarDeclStmt(varDecl, expression, currentToken.posn);
				break;
			
			default: 
				parseError("id, [", currentToken.spelling);
			}	
			break;
			
		case Token.THIS:	
			ThisRef thisRef = new ThisRef(currentToken.posn);
			acceptIt();
			switch(currentToken.kindInt) {
			
			case Token.ASSIGNMENT:
				acceptIt();
				expression = parseExpression();
				statement = new AssignStmt(thisRef, expression, currentToken.posn);
				break;
			
			case Token.LPAREN:
				acceptIt();
				ExprList exprList = new ExprList();
				if (currentToken.kindInt == Token.THIS 
						|| currentToken.kindInt == Token.ID
						|| currentToken.kindInt == Token.UNOP
						|| currentToken.kindInt == Token.BINUNOP
						|| currentToken.kindInt == Token.LPAREN
						|| currentToken.kindInt == Token.NUM
						|| currentToken.kindInt == Token.TRUE
						|| currentToken.kindInt == Token.FALSE
						|| currentToken.kindInt == Token.NEW
						|| currentToken.kindInt == Token.INT) {
					exprList = parseArgList();
				}
				accept(Token.RPAREN);
				statement = new CallStmt(thisRef, exprList, currentToken.posn);
				break;
				
			case Token.LBRACKET:		// edited for PA2
				acceptIt();
				expression = parseExpression();
				accept(Token.RBRACKET);
				IndexedRef indexedRef = new IndexedRef(thisRef, expression, currentToken.posn);
				
				switch (currentToken.kindInt) {
				case Token.ASSIGNMENT:
					acceptIt();
					expression = parseExpression();
					statement = new AssignStmt(indexedRef, expression, currentToken.posn);
					break;
					
				case Token.LPAREN:
					acceptIt();
					exprList = new ExprList();
					if (currentToken.kindInt == Token.THIS 
							|| currentToken.kindInt == Token.ID
							|| currentToken.kindInt == Token.UNOP
							|| currentToken.kindInt == Token.BINUNOP
							|| currentToken.kindInt == Token.LPAREN
							|| currentToken.kindInt == Token.NUM
							|| currentToken.kindInt == Token.TRUE
							|| currentToken.kindInt == Token.FALSE
							|| currentToken.kindInt == Token.NEW
							|| currentToken.kindInt == Token.INT) {
						exprList = parseArgList();
					}
					accept(Token.RPAREN);
					statement = new CallStmt(indexedRef, exprList, currentToken.posn);
					break;
					
				default:
					parseError("=, (", currentToken.spelling);
				}
				break;
			
			case Token.DOT:
				acceptIt();
				Identifier id = new Identifier(currentToken);
				accept(Token.ID);
				QualifiedRef qualifiedRef = new QualifiedRef(thisRef, id, currentToken.posn);
				
				while(currentToken.kindInt == Token.DOT) {
					acceptIt();
					id = new Identifier(currentToken);
					accept(Token.ID);
					qualifiedRef = new QualifiedRef(qualifiedRef, id, currentToken.posn);
				}
				
				switch (currentToken.kindInt) {
				
				case Token.ASSIGNMENT:
					acceptIt();
					expression = parseExpression();
					statement = new AssignStmt(qualifiedRef, expression, currentToken.posn);
					break;
				
				case Token.LPAREN:
					acceptIt();
					exprList = new ExprList();
					if (currentToken.kindInt == Token.THIS 
							|| currentToken.kindInt == Token.ID
							|| currentToken.kindInt == Token.UNOP
							|| currentToken.kindInt == Token.BINUNOP
							|| currentToken.kindInt == Token.LPAREN
							|| currentToken.kindInt == Token.NUM
							|| currentToken.kindInt == Token.TRUE
							|| currentToken.kindInt == Token.FALSE
							|| currentToken.kindInt == Token.NEW
							|| currentToken.kindInt == Token.INT) {
						exprList = parseArgList();
					}
					accept(Token.RPAREN);
					statement = new CallStmt(qualifiedRef, exprList, currentToken.posn);
					break;
					
				case Token.LBRACKET:			// edited for PA2
					acceptIt();
					expression = parseExpression();
					accept(Token.RBRACKET);
					
					indexedRef = new IndexedRef(qualifiedRef, expression, currentToken.posn);
					
					switch (currentToken.kindInt) {			
					case Token.ASSIGNMENT:
						acceptIt();
						expression = parseExpression();
						statement = new AssignStmt(indexedRef, expression, currentToken.posn);
						break;
						
					case Token.LPAREN:
						acceptIt();
						exprList = new ExprList();
						if (currentToken.kindInt == Token.THIS 
								|| currentToken.kindInt == Token.ID
								|| currentToken.kindInt == Token.UNOP
								|| currentToken.kindInt == Token.BINUNOP
								|| currentToken.kindInt == Token.LPAREN
								|| currentToken.kindInt == Token.NUM
								|| currentToken.kindInt == Token.TRUE
								|| currentToken.kindInt == Token.FALSE
								|| currentToken.kindInt == Token.NEW
								|| currentToken.kindInt == Token.INT) {
							exprList = parseArgList();
						}
						accept(Token.RPAREN);
						statement = new CallStmt(indexedRef, exprList, currentToken.posn);
						break;
						
					default:
						parseError("=, or (", currentToken.spelling);
					}
					break;
				
				default:
					parseError("=, (, or [", currentToken.spelling);
				}
				break;
				
			default:
				parseError("=, (, [, or .", currentToken.spelling);
			}
			accept(Token.SEMICOLON);
			break;
			
		case Token.ID:
			Identifier id = new Identifier(currentToken);
			IdRef idRef = new IdRef(id, currentToken.posn);
			
			Type type = new ClassType(id, currentToken.posn);
			spelling = currentToken.spelling;
			
			acceptIt();
			switch(currentToken.kindInt) {
			
			case Token.ID: // might be funking with spelling (class name and var name)
				spelling = currentToken.spelling;
				acceptIt();
				accept(Token.ASSIGNMENT);
				expression = parseExpression();
				varDecl = new VarDecl(type, spelling, currentToken.posn);
				statement = new VarDeclStmt(varDecl, expression, currentToken.posn);
				break;
				
			case Token.LBRACKET:			// edited for PA2
				acceptIt();
				
				switch (currentToken.kindInt) { // var decl 
				case Token.RBRACKET:
					acceptIt();
					type = new ArrayType(type, currentToken.posn);
					spelling = currentToken.spelling;
					varDecl = new VarDecl(type, spelling, currentToken.posn);
					
					accept(Token.ID);
					accept(Token.ASSIGNMENT);
					
					expression = parseExpression();
					
					statement = new VarDeclStmt(varDecl, expression, currentToken.posn);
					break;
					
				case Token.THIS: case Token.ID: // assign  or call 
				case Token.UNOP: case Token.LPAREN:
				case Token.NUM: case Token.TRUE: 
				case Token.FALSE: case Token.NEW: case Token.BINUNOP:
					expression = parseExpression();
					accept(Token.RBRACKET);
					
					IndexedRef indexedRef = new IndexedRef(idRef, expression, currentToken.posn);
					
					switch (currentToken.kindInt) {
					case Token.ASSIGNMENT: // assign 
						acceptIt();
						expression = parseExpression();
						statement = new AssignStmt(indexedRef, expression, currentToken.posn);
						break;
						
					case Token.LPAREN: // call 
						acceptIt();
						ExprList exprList = new ExprList();
						if (currentToken.kindInt == Token.THIS 
						|| currentToken.kindInt == Token.ID
						|| currentToken.kindInt == Token.UNOP
						|| currentToken.kindInt == Token.BINUNOP
						|| currentToken.kindInt == Token.LPAREN
						|| currentToken.kindInt == Token.NUM
						|| currentToken.kindInt == Token.TRUE
						|| currentToken.kindInt == Token.FALSE
						|| currentToken.kindInt == Token.NEW
						|| currentToken.kindInt == Token.INT) {
							exprList = parseArgList();
						}
						accept(Token.RPAREN);
						statement = new CallStmt(indexedRef, exprList, currentToken.posn);
						break;
					
					default:
						parseError("( or =", currentToken.spelling);
					}
					break;
				
				default:
					parseError("], this, id, unop, binunop (, num, boolean, or new", currentToken.spelling);
				}
				break;
				
			case Token.LPAREN:
				acceptIt();
				ExprList exprList = new ExprList();
				if (currentToken.kindInt == Token.THIS 
						|| currentToken.kindInt == Token.ID
						|| currentToken.kindInt == Token.UNOP
						|| currentToken.kindInt == Token.BINUNOP
						|| currentToken.kindInt == Token.LPAREN
						|| currentToken.kindInt == Token.NUM
						|| currentToken.kindInt == Token.TRUE
						|| currentToken.kindInt == Token.FALSE
						|| currentToken.kindInt == Token.NEW
						|| currentToken.kindInt == Token.INT) {
					exprList = parseArgList();
				}
				accept(Token.RPAREN);
				statement = new CallStmt(idRef, exprList, currentToken.posn);
				break;
				
			case Token.DOT:
				acceptIt();
				id = new Identifier(currentToken);
				accept(Token.ID);
				QualifiedRef qualifiedRef = new QualifiedRef(idRef, id, currentToken.posn);
				
				while(currentToken.kindInt == Token.DOT) {
					acceptIt();
					id = new Identifier(currentToken);
					accept(Token.ID);
					qualifiedRef = new QualifiedRef(qualifiedRef, id, currentToken.posn);
				}
				
				switch (currentToken.kindInt) {
				
				case Token.ASSIGNMENT:
					acceptIt();
					expression = parseExpression();
					statement = new AssignStmt(qualifiedRef, expression, currentToken.posn);
					break;
				
				case Token.LPAREN:
					acceptIt();
					exprList = new ExprList();
					if (currentToken.kindInt == Token.THIS 
							|| currentToken.kindInt == Token.ID
							|| currentToken.kindInt == Token.UNOP
							|| currentToken.kindInt == Token.BINUNOP
							|| currentToken.kindInt == Token.LPAREN
							|| currentToken.kindInt == Token.NUM
							|| currentToken.kindInt == Token.TRUE
							|| currentToken.kindInt == Token.FALSE
							|| currentToken.kindInt == Token.NEW
							|| currentToken.kindInt == Token.INT) {
						exprList = parseArgList();
					}
					accept(Token.RPAREN);
					statement = new CallStmt(qualifiedRef, exprList, currentToken.posn);
					break;
					
				case Token.LBRACKET:			// edited for PA2
					acceptIt();
					expression = parseExpression();
					accept(Token.RBRACKET);
					
					IndexedRef indexedRef = new IndexedRef(qualifiedRef, expression, currentToken.posn);
					
					switch (currentToken.kindInt) {
					case Token.ASSIGNMENT:
						acceptIt();
						expression = parseExpression();
						statement = new AssignStmt(indexedRef, expression, currentToken.posn);
						break;
						
					case Token.LPAREN:
						acceptIt();
						exprList = new ExprList();
						if (currentToken.kindInt == Token.THIS 
								|| currentToken.kindInt == Token.ID
								|| currentToken.kindInt == Token.UNOP
								|| currentToken.kindInt == Token.BINUNOP
								|| currentToken.kindInt == Token.LPAREN
								|| currentToken.kindInt == Token.NUM
								|| currentToken.kindInt == Token.TRUE
								|| currentToken.kindInt == Token.FALSE
								|| currentToken.kindInt == Token.NEW
								|| currentToken.kindInt == Token.INT) {
							exprList = parseArgList();
						}
						accept(Token.RPAREN);
						statement = new CallStmt(indexedRef, exprList, currentToken.posn);
						break;
						
					default:
						parseError("= or (", currentToken.spelling);
					}
					break;
				
				default:
					parseError("=, (, or [", currentToken.spelling);
				}
				break;
				
			case Token.ASSIGNMENT:
				acceptIt();
				expression = parseExpression();
				statement = new AssignStmt(idRef, expression, currentToken.posn);
				break;
				
			default:
				parseError("id, (, [, ., or =", currentToken.spelling);
			}
			accept(Token.SEMICOLON);
			break;
		
		default:
			parseError("{, if, while, boolean, int, this, or id", currentToken.spelling);
		}	
		return statement; 
	}

	/*			PRECEDENCE OF OPERATORS 
	 * 
	 *			disjunction ||
	 *			conjunction &&
	 *			equality ==, !=
	 *			relational <=, <, >, >=
	 *			additive +, -
	 * 			multiplicative *, /
	 *			unary -, !  
	 */
	
	private Expression parseExpression(){
		Expression expression = parseB();
		while (currentToken.spelling.equals("||")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expression2 = parseB();
			expression = new BinaryExpr(op, expression, expression2, currentToken.posn);
		}
		return expression;
	}
	
	private Expression parseB() {
		Expression expression = parseC();
		while (currentToken.spelling.equals("&&")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expression2 = parseC();
			expression = new BinaryExpr(op, expression, expression2, currentToken.posn);
		}
		return expression;
	}
	
	private Expression parseC() {
		Expression expression = parseD();
		while (currentToken.spelling.equals("==") ||
				currentToken.spelling.equals("!=")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expression2 = parseD();
			expression = new BinaryExpr(op, expression, expression2, currentToken.posn);
		}
		return expression;
	}
	private Expression parseD() {
		Expression expression = parseE();
		while (currentToken.spelling.equals("<")
				|| currentToken.spelling.equals(">")
				|| currentToken.spelling.equals("<=")
				|| currentToken.spelling.equals(">=")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expression2 = parseE();
			expression = new BinaryExpr(op, expression, expression2, currentToken.posn);
		}
		return expression;
	}
	
	private Expression parseE() {
		Expression expression = parseF();
		while (currentToken.spelling.equals("+")
				|| currentToken.spelling.equals("-")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expression2 = parseF();
			expression = new BinaryExpr(op, expression, expression2, currentToken.posn);
		}
		return expression;
	}
	private Expression parseF() {
		Expression expression = parseG();
		while (currentToken.spelling.equals("*") 
				|| currentToken.spelling.equals("/")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expression2 = parseG();
			expression = new BinaryExpr(op, expression, expression2, currentToken.posn);
		}
		return expression;
	}
	
	private Expression parseG() {
		while (currentToken.spelling.equals("-") 
				|| currentToken.spelling.equals("!")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expression2 = parseG();
			return new UnaryExpr(op, expression2, currentToken.posn);
		}
		Expression expression = parseH();
		return expression;
	}
	
	private Expression parseH() throws SyntaxError{
		Expression expression = null;
		switch(currentToken.kindInt) {
		
		case Token.THIS:
			Reference thisRef = new ThisRef(currentToken.posn);
			acceptIt();
			switch(currentToken.kindInt) {
			
			case Token.LPAREN:
				acceptIt();
				ExprList exprList = new ExprList();
				if (currentToken.kindInt == Token.THIS 
					|| currentToken.kindInt == Token.ID
					|| currentToken.kindInt == Token.UNOP
					|| currentToken.kindInt == Token.BINUNOP
					|| currentToken.kindInt == Token.LPAREN
					|| currentToken.kindInt == Token.NUM
					|| currentToken.kindInt == Token.TRUE
					|| currentToken.kindInt == Token.FALSE
					|| currentToken.kindInt == Token.NEW
					|| currentToken.kindInt == Token.INT) {
					exprList = parseArgList();
				}
				accept(Token.RPAREN);
				expression = new CallExpr(thisRef, exprList, currentToken.posn);
				break;
				
			case Token.LBRACKET:				// edited for PA2
				acceptIt();
				Expression expression2 = parseExpression();
				Reference indexedRef = new IndexedRef(thisRef, expression2, currentToken.posn);
				
				accept(Token.RBRACKET);
				
				if (currentToken.kindInt == Token.LPAREN) {
					acceptIt();
					exprList = new ExprList();
					if (currentToken.kindInt == Token.THIS 
							|| currentToken.kindInt == Token.ID
							|| currentToken.kindInt == Token.UNOP
							|| currentToken.kindInt == Token.BINUNOP
							|| currentToken.kindInt == Token.LPAREN
							|| currentToken.kindInt == Token.NUM
							|| currentToken.kindInt == Token.TRUE
							|| currentToken.kindInt == Token.FALSE
							|| currentToken.kindInt == Token.NEW
							|| currentToken.kindInt == Token.INT) {
							exprList = parseArgList();
						}	
					accept(Token.RPAREN);
					expression = new CallExpr(indexedRef, exprList, currentToken.posn);
				} else {
					expression = new RefExpr(indexedRef,currentToken.posn);
				}
				break;
			
			case Token.DOT:
				acceptIt();
				Identifier id = new Identifier(currentToken);
				accept(Token.ID);
				QualifiedRef qualifiedRef = new QualifiedRef(thisRef, id, currentToken.posn);
				
				while(currentToken.kindInt == Token.DOT) {
					acceptIt();
					id = new Identifier(currentToken);
					accept(Token.ID);
					qualifiedRef = new QualifiedRef(qualifiedRef, id, currentToken.posn);
				}
				
				switch(currentToken.kindInt) {
				case Token.LPAREN:
					acceptIt();
					exprList = new ExprList();
					if (currentToken.kindInt == Token.THIS 
						|| currentToken.kindInt == Token.ID
						|| currentToken.kindInt == Token.UNOP
						|| currentToken.kindInt == Token.BINUNOP
						|| currentToken.kindInt == Token.LPAREN
						|| currentToken.kindInt == Token.NUM
						|| currentToken.kindInt == Token.TRUE
						|| currentToken.kindInt == Token.FALSE
						|| currentToken.kindInt == Token.NEW
						|| currentToken.kindInt == Token.INT) {
						exprList = parseArgList();
					}
					accept(Token.RPAREN);
					expression = new CallExpr(qualifiedRef, exprList, currentToken.posn);
					break;
				
				case Token.LBRACKET:		 // edited for PA2
					acceptIt();
					expression2 = parseExpression();
					accept(Token.RBRACKET);
					indexedRef = new IndexedRef(qualifiedRef, expression2, currentToken.posn);
					
					if (currentToken.kindInt == Token.LPAREN) {
						acceptIt();
						exprList = new ExprList();
						if (currentToken.kindInt == Token.THIS 
								|| currentToken.kindInt == Token.ID
								|| currentToken.kindInt == Token.UNOP
								|| currentToken.kindInt == Token.BINUNOP
								|| currentToken.kindInt == Token.LPAREN
								|| currentToken.kindInt == Token.NUM
								|| currentToken.kindInt == Token.TRUE
								|| currentToken.kindInt == Token.FALSE
								|| currentToken.kindInt == Token.NEW
								|| currentToken.kindInt == Token.INT) {
								exprList = parseArgList();
							}					
						accept(Token.RPAREN);
						expression = new CallExpr(qualifiedRef, exprList, currentToken.posn);
					} else {
						expression = new RefExpr(indexedRef, currentToken.posn);
					}
					break;
				
				default:
					// it can end here.
					expression = new RefExpr(qualifiedRef, currentToken.posn);
				}
				break;
			
			default:
				// it can end here.
				expression = new RefExpr(thisRef, currentToken.posn);
			}
			break;
		
		case Token.ID:
			
			Identifier id = new Identifier(currentToken);
			IdRef idRef = new IdRef(id, currentToken.posn);
			
			acceptIt();
			switch(currentToken.kindInt) {
			case Token.LBRACKET:			// edited for PA2
				acceptIt();
				Expression expression2 = parseExpression();
				accept(Token.RBRACKET);
				
				IndexedRef indexedRef = new IndexedRef(idRef, expression2, currentToken.posn);
				
				if (currentToken.kindInt == Token.LPAREN) {
					acceptIt();
					ExprList exprList = new ExprList();
					if (currentToken.kindInt == Token.THIS 
							|| currentToken.kindInt == Token.ID
							|| currentToken.kindInt == Token.UNOP
							|| currentToken.kindInt == Token.BINUNOP
							|| currentToken.kindInt == Token.LPAREN
							|| currentToken.kindInt == Token.NUM
							|| currentToken.kindInt == Token.TRUE
							|| currentToken.kindInt == Token.FALSE
							|| currentToken.kindInt == Token.NEW
							|| currentToken.kindInt == Token.INT) {
							exprList = parseArgList();
						}					
					accept(Token.RPAREN);
					expression = new CallExpr(indexedRef, exprList, currentToken.posn);
				} else {
					expression = new RefExpr(indexedRef, currentToken.posn);
				}
				break;
				
			case Token.DOT:
				acceptIt();
				id = new Identifier(currentToken);
				accept(Token.ID);
				QualifiedRef qualifiedRef = new QualifiedRef(idRef, id, currentToken.posn);
				
				while(currentToken.kindInt == Token.DOT) {
					acceptIt();
					id = new Identifier(currentToken);
					accept(Token.ID);
					qualifiedRef = new QualifiedRef(qualifiedRef, id, currentToken.posn);
					
				}
				switch(currentToken.kindInt) {
				case Token.LPAREN:
					acceptIt();
					ExprList exprList = new ExprList();
					if (currentToken.kindInt == Token.THIS 
						|| currentToken.kindInt == Token.ID
						|| currentToken.kindInt == Token.UNOP
						|| currentToken.kindInt == Token.BINUNOP
						|| currentToken.kindInt == Token.LPAREN
						|| currentToken.kindInt == Token.NUM
						|| currentToken.kindInt == Token.TRUE
						|| currentToken.kindInt == Token.FALSE
						|| currentToken.kindInt == Token.NEW
						|| currentToken.kindInt == Token.INT) {
						exprList = parseArgList();
					}
					accept(Token.RPAREN);
					expression = new CallExpr(qualifiedRef, exprList, currentToken.posn);
					break;
				
				case Token.LBRACKET:			// edited for PA2
					acceptIt();
					expression2 = parseExpression();
					accept(Token.RBRACKET);
					
					indexedRef = new IndexedRef(qualifiedRef, expression2, currentToken.posn);
					
					if (currentToken.kindInt == Token.LPAREN) {
						acceptIt();
						exprList = new ExprList();
						if (currentToken.kindInt == Token.THIS 
								|| currentToken.kindInt == Token.ID
								|| currentToken.kindInt == Token.UNOP
								|| currentToken.kindInt == Token.BINUNOP
								|| currentToken.kindInt == Token.LPAREN
								|| currentToken.kindInt == Token.NUM
								|| currentToken.kindInt == Token.TRUE
								|| currentToken.kindInt == Token.FALSE
								|| currentToken.kindInt == Token.NEW
								|| currentToken.kindInt == Token.INT) {
								exprList = parseArgList();
							}
						accept(Token.RPAREN);
						expression = new CallExpr(indexedRef, exprList, currentToken.posn);
					} else {
						expression = new RefExpr(indexedRef, currentToken.posn);
					}
					break;
				
				default:
					// it can end here.
					expression = new RefExpr(qualifiedRef, currentToken.posn);
				}
				break;
				
			case Token.LPAREN:
				acceptIt();
				ExprList exprList = new ExprList();
				if (currentToken.kindInt == Token.THIS 
						|| currentToken.kindInt == Token.ID
						|| currentToken.kindInt == Token.UNOP
						|| currentToken.kindInt == Token.BINUNOP
						|| currentToken.kindInt == Token.LPAREN
						|| currentToken.kindInt == Token.NUM
						|| currentToken.kindInt == Token.TRUE
						|| currentToken.kindInt == Token.FALSE
						|| currentToken.kindInt == Token.NEW
						|| currentToken.kindInt == Token.INT) {
						exprList = parseArgList();
					}
				accept(Token.RPAREN);
				expression = new CallExpr(idRef, exprList, currentToken.posn);
				break;
				
			default:
				// it can end here.
				expression = new RefExpr(idRef, currentToken.posn);
				
			}
			break;
		
		case Token.LPAREN:
			acceptIt();
			expression = parseExpression();
			accept(Token.RPAREN);
			break;
			
		case Token.NUM: 
			Terminal terminal = new IntLiteral(currentToken);
			expression = new LiteralExpr(terminal, currentToken.posn);
			acceptIt();
			break;
			
		case Token.TRUE: case Token.FALSE:
			terminal = new BooleanLiteral(currentToken);
			expression = new LiteralExpr(terminal, currentToken.posn);
			acceptIt();
			break;
		
		case Token.NEW:
			acceptIt();
			
			Type type = null;
			switch (currentToken.kindInt) {
			
			case Token.ID:
				id = new Identifier(currentToken);
				type = new ClassType(id, currentToken.posn);
				
				acceptIt();
				switch(currentToken.kindInt) {
				
				case Token.LPAREN: // new id()
					acceptIt();
					ExprList exprList = new ExprList();
					if (currentToken.kindInt == Token.THIS 
							|| currentToken.kindInt == Token.ID
							|| currentToken.kindInt == Token.UNOP
							|| currentToken.kindInt == Token.BINUNOP
							|| currentToken.kindInt == Token.LPAREN
							|| currentToken.kindInt == Token.NUM
							|| currentToken.kindInt == Token.TRUE
							|| currentToken.kindInt == Token.FALSE
							|| currentToken.kindInt == Token.NEW
							|| currentToken.kindInt == Token.INT) {
						exprList = parseArgList();
					}
					accept(Token.RPAREN);
					if (exprList.size() > 0) {
						expression = new NewObjectExpr((ClassType) type, exprList, currentToken.posn);
					} else {
						expression = new NewObjectExpr((ClassType) type, currentToken.posn);	
					}
					break;
					
				case Token.LBRACKET: // new id[Expression]
					acceptIt();
					Expression expression2 = parseExpression();
					accept(Token.RBRACKET);
					expression = new NewArrayExpr(type, expression2, currentToken.posn);
					break;
					
				default:
					parseError("( or [", currentToken.spelling);
				}
				break;
			
			case Token.INT: // new array expression 
				acceptIt();
				accept(Token.LBRACKET);
				Expression expression2 = parseExpression();
				accept(Token.RBRACKET);
				expression = new NewArrayExpr(new BaseType(TypeKind.INT, currentToken.posn), 
						expression2, currentToken.posn);
				break;
			
			default:
				parseError("id or int", currentToken.spelling);
			}
			break;
		
		case Token.NULL:
			Terminal nullType = new NullLiteral(currentToken);
			expression = new LiteralExpr(nullType, currentToken.posn);
			acceptIt();
			break;
			
		default:
			parseError("this, id (, num, boolean, new, or null", currentToken.spelling);
		
		}
		return expression;
	}
	
	private void parseError(String s1, String s2) throws SyntaxError {
		errorReporter.reportError("@@@ PARSE ", "Expected token is of " + s1 + 
				" but received token of " + s2 , "", currentToken.posn);
		throw new SyntaxError();
	}
	
	private void parseError(String s) throws SyntaxError {
		errorReporter.reportError("@@@PARSE ", s, "", currentToken.posn);
		throw new SyntaxError();
	}
}