package miniJava.SyntacticAnalyzer;

public enum TokenKind {	
						// literals, identifiers, operators...
						ID,
						NUM			,
						BINOP		,
						UNOP		,
						BINUNOP 	,
						
						// reserved words - must be in alphabetical order...
						BOOLEAN		,
						CLASS		,
						ELSE		,
						FALSE		,
						IF			,
						INT			,
						NEW			,
						NULL		,
						PRIVATE		,
						PUBLIC		,
						RETURN		,
						STATIC		,
						THIS		,
						TRUE		,
						VOID		,
						WHILE		,
						
						// punctuation...
						QMARK		,
						SEMICOLON	,
						COMMA	,
						ASSIGNMENT  ,
						DOT			,
						
						// brackets...
						LPAREN		,
						RPAREN		,
						LBRACKET	,
						RBRACKET	,
						LCURLY		,
						RCURLY		,
						
						// special tokens...
						EOT			,
						ERROR		;}
