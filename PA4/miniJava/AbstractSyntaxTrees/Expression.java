/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import  miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Expression extends AST {

  public Expression(SourcePosition posn) {
    super (posn);
  }
  
  Type type;
  public int intValue; // for code generation. like if (x + 2 > 3) or some something. need to know if it's 0 or 1
  
}
