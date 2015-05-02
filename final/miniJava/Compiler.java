package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.CodeGenerator.Encoder;
import miniJava.ContextualAnalyzer.*;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;

public class Compiler {
	public static void main(String[] args) throws FileNotFoundException {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(args[0]);
		} catch (FileNotFoundException e) {
			System.out.println("File Not Found!");
			System.exit(4);
		}
		AST theAST = null;

		Scanner scanner = new Scanner(inputStream);
		Parser parser = new Parser(scanner);
		ASTDisplay astDisplay = new ASTDisplay();
		Identification identifier = new Identification();
		TypeChecker typeChecker = new TypeChecker();
		//Encoder encoder = new Encoder(args[0]);
		Encoder encoder = new Encoder();
		
		//scanAndPrintTokens(parser);
		//parser = resetScannerParser(args, scanner);
		
		try {
			theAST = parser.parse();
			//astDisplay.showTree(theAST);
			ClassTableManager tables = identifier.identify(theAST);
			typeChecker.typeCheck(theAST, tables);
			//astDisplay.showTree(theAST);
			encoder.encode(theAST);
			System.exit(0);
		} catch (SyntaxError e) {
			System.out.println("Compilation Failed");
			System.exit(4);
		}
	}

	private static Parser resetScannerParser(String[] args, Scanner scanner)
			throws FileNotFoundException {
		InputStream inputStream;
		Parser parser;
		inputStream = new FileInputStream(args[0]);
		scanner.resetInputStream(inputStream);
		parser = new Parser(scanner);
		return parser;
	}

	private static void scanAndPrintTokens(Parser parser) {
		ArrayList<Token> tokens = parser.scanAll();
		for (Token token : tokens) {
			System.out.println(Token.getTokenName(token));
		}
	}
}