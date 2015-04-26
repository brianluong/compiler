
package miniJava.AbstractSyntaxTrees;

/**
 * An implementation of the Visitor interface provides a method visitX
 * for each non-abstract AST class X.  
 */
public interface TypeCheckVisitor {

  // Package
    public Type visitPackage(Package prog);

  // Declarations
    public Type visitClassDecl(ClassDecl cd);
    public Type visitFieldDecl(FieldDecl fd  );
    public Type visitMethodDecl(MethodDecl md  );
    public Type visitParameterDecl(ParameterDecl pd  );
    public Type visitVarDecl(VarDecl decl  );
 
  // Types
    public Type visitBaseType(BaseType type  );
    public Type visitClassType(ClassType type  );
    public Type visitArrayType(ArrayType type  );
    public Type visitErrorType(ErrorType errorType);
    public Type visitUnsupportedType(UnsupportedType unsupportedType);
    
  // Statements
    public Type visitBlockStmt(BlockStmt stmt  );
    public Type visitVardeclStmt(VarDeclStmt stmt  );
    public Type visitAssignStmt(AssignStmt stmt  );
    public Type visitCallStmt(CallStmt stmt  );
    public Type visitIfStmt(IfStmt stmt  );
    public Type visitWhileStmt(WhileStmt stmt  );
    
  // Expressions
    public Type visitUnaryExpr(UnaryExpr expr  );
    public Type visitBinaryExpr(BinaryExpr expr  );
    public Type visitRefExpr(RefExpr expr  );
    public Type visitCallExpr(CallExpr expr  );
    public Type visitLiteralExpr(LiteralExpr expr  );
    public Type visitNewObjectExpr(NewObjectExpr expr  );
    public Type visitNewArrayExpr(NewArrayExpr expr  );
    
  // References
    public Type visitQualifiedRef(QualifiedRef ref  );
    public Type visitIndexedRef(IndexedRef ref  );
    public Type visitIdRef(IdRef ref  );
    public Type visitThisRef(ThisRef ref  );

  // Terminals
    public Type visitIdentifier(Identifier id  );
    public Type visitOperator(Operator op  );
    public Type visitIntLiteral(IntLiteral num  );
    public Type visitBooleanLiteral(BooleanLiteral bool  );
}
