package miniJava.SyntacticAnalyzer;

public class Token {

	public int kindInt;
	public TokenKind kind;
	public String spelling;
	public int lineNum;
	public SourcePosition posn;
	private static final int STARTRANGEID = 5;
	private static final int ENDRANGEID = 20;
	
	public Token(int inputKind, String spelling, SourcePosition position) {
		if (inputKind == 0) {
			int currentKind = STARTRANGEID;
			boolean searching = true;
			while (searching) {
				int comparison = tokenTable[currentKind].compareTo(spelling);
				if (comparison == 0) {
					kindInt = currentKind;
					convertTokenIntToTokenKind(kindInt);
					searching = false;
				} else if (comparison > 0 || currentKind == ENDRANGEID) {
			          kindInt = ID;
			          kind = TokenKind.ID;
			          searching = false;
			    } else {
			          currentKind++;
			    }
			}
		} else {
			kindInt = inputKind;
			convertTokenIntToTokenKind(kindInt);
		}
		this.spelling = spelling;
		this.posn = position;
	}
	
	public static String getTokenName(Token token) {
		return tokenTable[token.kindInt];
	}
	
	public static String getTokenName(int token) {
		return tokenTable[token];
	}
	
	public static final int

	    // literals, identifiers, operators...
	 	ID			= 0,
	    NUM			= 1,
	    BINOP		= 2,
	    UNOP		= 3,
	    BINUNOP 	= 4,

	    // reserved words - must be in alphabetical order...
	    BOOLEAN		= 5,
	    CLASS		= 6,
	    ELSE		= 7,
	    FALSE 		= 8,
	    IF			= 9,
	    INT			= 10,
	    NEW			= 11,
	    NULL		= 12,
	    PRIVATE		= 13,
	    PUBLIC		= 14,
	    RETURN		= 15,
	    STATIC		= 16,
	    THIS		= 17,
	    TRUE		= 18,
	    VOID		= 19,
	    WHILE		= 20,
	    
	    // punctuation...
	    QMARK		= 21,
	    SEMICOLON	= 22,
	    COMMA		= 23,
	    ASSIGNMENT  = 24,
	    DOT			= 25,

	    // brackets...
	    LPAREN		= 26,
	    RPAREN		= 27,
	    LBRACKET	= 28,
	    RBRACKET	= 29,
	    LCURLY		= 30,
	    RCURLY		= 31,

	    // special tokens...
	    EOT			= 31,
	    ERROR		= 32;

	private static String[] tokenTable = new String[] {
		"<id>",
		"<num>",
		"<binop>",
		"<unop>",
		"<binunop>",
		"boolean",
		"class",
		"else",
		"false",
		"if",
		"int",
		"new",
		"null",
		"private",
		"public",
		"return",
		"static",
		"this",
		"true",
		"void",
		"while",
		"?",
		";",
		",",
		"=",
		".",
		"(",
		")",
		"[",
		"]",
		"{",
		"}",
		"",
		"<error>"
	  };
	
	private void convertTokenIntToTokenKind(int currentKind) {
		 	if (currentKind == 1) {
				kind = TokenKind.NUM;
			} else if (currentKind== 2) {
				kind = TokenKind.BINOP;
			} else if (currentKind== 3) {
				kind = TokenKind.UNOP;
			} else if (currentKind== 4) {
				kind = TokenKind.BINUNOP;
			} else if (currentKind == 5) {
				kind = TokenKind.BOOLEAN;
			} else if (currentKind == 6) {
				kind = TokenKind.CLASS;
			} else if (currentKind == 7) {
				kind = TokenKind.FALSE;
			} else if (currentKind == 8) {
				kind = TokenKind.ELSE;
			} else if (currentKind == 9) {
				kind = TokenKind.IF;
			} else if (currentKind == 10) {
				kind = TokenKind.INT;
			} else if (currentKind == 11) {
				kind = TokenKind.NEW;
			} else if (currentKind == 12) {
				kind = TokenKind.NULL;
			} else if (currentKind == 13) {
				kind = TokenKind.PRIVATE;
			} else if (currentKind == 14) {
				kind = TokenKind.PUBLIC;
			} else if (currentKind == 15) {
				kind = TokenKind.RETURN;
			} else if (currentKind == 16) {
				kind = TokenKind.STATIC;
			} else if (currentKind == 17) {
				kind = TokenKind.THIS;
			} else if (currentKind == 18) {
				kind = TokenKind.TRUE;
			} else if (currentKind == 19) {
				kind = TokenKind.VOID;
			} else if (currentKind == 20) {
				kind = TokenKind.WHILE;
			} else if (currentKind== 21) {
				kind = TokenKind.QMARK;
			} else if (currentKind== 22) {
				kind = TokenKind.SEMICOLON;
			} else if (currentKind== 23) {
				kind = TokenKind.COMMA;
			} else if (currentKind== 24) {
				kind = TokenKind.ASSIGNMENT;
			} else if (currentKind== 25) {
				kind = TokenKind.DOT;
			} else if (currentKind== 26) {
				kind = TokenKind.LPAREN;
			} else if (currentKind== 27) {
				kind = TokenKind.RPAREN;
			} else if (currentKind== 28) {
				kind = TokenKind.LBRACKET;
			} else if (currentKind== 29) {
				kind = TokenKind.RBRACKET;
			} else if (currentKind== 30) {
				kind = TokenKind.LCURLY;
			} else if (currentKind== 31) {
				kind = TokenKind.RCURLY;
			} else if (currentKind== 32) {
				kind = TokenKind.EOT;
			} else if (currentKind== 33) {
				kind = TokenKind.ERROR;
			} 
	}
}