package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

public class Scanner {

	private char currentChar;
	private StringBuilder currentSpelling;
	private InputStream inputStream;
	
	public int currentLine;
	boolean minusChecker;
	
	public Scanner(InputStream inputStream) {
		this.inputStream = inputStream;
		currentLine = 1;
		readChar(); // initilize scanner
	}
	
	public Token scan() {
		skipWhitespace();
		Token tok;
		
		// for skipping comments
		currentSpelling  = new StringBuilder("");
		SourcePosition pos = new SourcePosition();
		pos.start = currentLine;
		
		while (currentChar == '/') {
			takeIt();
			if (currentChar == '/') {					
				takeIt();
				while (currentChar != '\n') {
					readChar();
				}
				takeIt();
				skipWhitespace();								// need to skip whitespace again
				} else if (currentChar == '*') {
					takeIt();
					while (true) {
						while (currentChar != '*' && currentChar != (char) -1) {
							if (currentChar == (char) -1) {
								break;
							}
							readChar();
						}
						if (currentChar == (char) -1) {
							System.out.println("CommentError");
							pos.finish = currentLine;
							return new Token(Token.ERROR,"", pos);
						}
						readChar();
						if (currentChar == '/') {
							readChar();
							break;
						} 
					}
					skipWhitespace();
				} else {
					pos.finish = currentLine;
					return new Token(Token.BINOP,"/", pos);
			}
		}
		currentSpelling  = new StringBuilder("");
		pos.start = currentLine;
		int tokenNumber = scanToken();
		pos.finish = currentLine;
		tok = new Token(tokenNumber, currentSpelling.toString(), pos);
		return tok;
	}

	private void skipWhitespace() {
		while (currentChar == ' ' 
				|| currentChar ==  '\t'
				|| currentChar ==  '\n'
				|| currentChar ==  '\r') {
			readChar();
		}
	}
	
	private int scanToken() {
	    switch (currentChar) {

	    case 'a':  case 'b':  case 'c':  case 'd':  case 'e':
	    case 'f':  case 'g':  case 'h':  case 'i':  case 'j':
	    case 'k':  case 'l':  case 'm':  case 'n':  case 'o':
	    case 'p':  case 'q':  case 'r':  case 's':  case 't':
	    case 'u':  case 'v':  case 'w':  case 'x':  case 'y':
	    case 'z':
	    case 'A':  case 'B':  case 'C':  case 'D':  case 'E':
	    case 'F':  case 'G':  case 'H':  case 'I':  case 'J':
	    case 'K':  case 'L':  case 'M':  case 'N':  case 'O':
	    case 'P':  case 'Q':  case 'R':  case 'S':  case 'T':
	    case 'U':  case 'V':  case 'W':  case 'X':  case 'Y':
	    case 'Z':
	    	takeIt();
	    	while (isLetter(currentChar) 
	    			|| isDigit(currentChar) 
	    			|| isUnderscore(currentChar)) {
	    		takeIt();
	    	}
	        return Token.ID; // return an id token
	   
	    case '0':  case '1':  case '2':  case '3':  case '4':
	    case '5':  case '6':  case '7':  case '8':  case '9':
	    	takeIt();
	    	while (isDigit(currentChar))
	    		takeIt();
	   		return Token.NUM;
	      
	    case '-':
	    	takeIt();
	    	if (currentChar == '-') { // check for the '--'
	    		return Token.ERROR;
	    	}
	    	return Token.BINUNOP;
	    
	    case '!':
	    	takeIt();
	    	if (currentChar == '=') {
	    		takeIt();
	    		return Token.BINOP;
	    	} else {
	    		return Token.UNOP;
	    	}
	   
	    case '+':  case '*':
	    	takeIt();
	    	return Token.BINOP;
	    
	    case '/':
	    	takeIt();
	    	if (currentChar == '/') {
	    		takeIt();
	    		while (currentChar != '\n') {
	    			readChar();
	    		}
	    	} else {
	    		return Token.BINOP;
	    	}
	    	
	    case '&':  
	    	takeIt();
	    	if (currentChar == '&') {
	    		takeIt();
	    		return Token.BINOP;
	    	} else {
	    		return Token.ERROR;
	    	}
	    	
	    case '|':
	      	takeIt();
	    	if (currentChar == '|') {
	    		takeIt();
	    		return Token.BINOP;
	    	} else {
	    		return Token.ERROR;
	    	}	
	    
	    case '>':
	    	takeIt();
	    	if (currentChar == '=') {
	    		takeIt();
	    	}
	    	return Token.BINOP;
	    	
	    case '<':  
	    	takeIt();
	    	if (currentChar == '=') {
	    		takeIt();
	    	}
	    	return Token.BINOP;
	    	
	    case '=': 
	    	takeIt();
	    	if (currentChar == '=') {
	    		takeIt();
	    		return Token.BINOP;
	    	} else {
	    		return Token.ASSIGNMENT;	
	    	}
	    	
	    case '.':
	    	takeIt();
	    	return Token.DOT;
	    	
	    case ';':
	    	takeIt();
	        return Token.SEMICOLON;

	    case ',':
	    	takeIt();
	    	return Token.COMMA;

	    case '(':
	    	takeIt();
	    	return Token.LPAREN;

	    case ')':
	    	takeIt();
	    	return Token.RPAREN;

	    case '[':
	    	takeIt();
	    	return Token.LBRACKET;

	    case ']':
	    	takeIt();
	    	return Token.RBRACKET;

	    case '{':
	    	takeIt();
	    	return Token.LCURLY;

	    case '}':
	    	takeIt();
	    	return Token.RCURLY;
	    
	    case (char) -1: 
	    	return Token.EOT;

	    default:
	    	return Token.ERROR;
	    }
	}

	private boolean isUnderscore(char c) {
		return c == '_';
	}

	private boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}
	
	private boolean isDigit(char c) {
		return (c >= '0' && c <= '9');
	}
	
	private void takeIt() {
		currentSpelling.append(currentChar);
		readChar();
	}
	
	private void readChar() {
		try {
			int c = inputStream.read();
			currentChar = (char) c;
			if (c == -1) {
				currentChar = (char) -1;
			} else if (c == '\n') {
				currentLine++;
			}
		} catch (IOException e) {
			currentChar = (char) -1;
		}
	}
	
	public void resetInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
		currentLine = 1;
		readChar();
	}
}