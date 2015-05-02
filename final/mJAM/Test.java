/**
 * Example illustrating components of mJAM package
 * @author prins
 * @version COMP 520 
 */
package mJAM;
import mJAM.Machine.Op;
import mJAM.Machine.Reg;
import mJAM.Machine.Prim;

// test class to construct mJAM program for miniJava example program  Counter.java
public class Test
{
	public static void main(String[] args){
		
		Machine.initCodeGen();
		System.out.println("Generating code for Counter.java example");
		
	/*
	 * Preamble:
	 *   generate call to main
	 *   
	 */
		Machine.emit(Op.LOADL,0);            // null pointer (should be empty array ptr)
		int patchAddr_Call_main = Machine.nextInstrAddr();  // record instr addr where
		                                                    // "main" is called
		Machine.emit(Op.CALL,Reg.CB,-1);     // call main (invalid addr)
		Machine.emit(Op.HALT,0,0,0);         // end execution
		
		
	/*
	 * Generate methods of class Counter
	 * 
	 */
		
		/*
		 *	  public void increase(int k) {
		 * 		  count = count + k;
		 *    }
		 */
		int codeAddr_increase = Machine.nextInstrAddr();  // record code addr of "increase"
/* increase: */
		Machine.emit(Op.LOAD,Reg.OB,0);      // current value of count
		Machine.emit(Op.LOAD,Reg.LB,-1);     // current value of k
		Machine.emit(Prim.add);              // count + k
		Machine.emit(Op.STORE,Reg.OB,0);     // store to count
		
		Machine.emit(Op.RETURN,0,0,1);       // return popping one caller arg (d = 1) 
		                                     // and returning no value (n = 0)
	
		
		/*
		 *    public static void main(String [] args){
		 *        Counter counter = new Counter();
		 *        counter.increase(3);
		 *        System.out.println(counter.count);
		 *    }
		 */		
		int codeAddr_main = Machine.nextInstrAddr();
/* main: */
		Machine.emit(Op.LOADL,-1);		     // -1 on stack (= no class descriptor)
		Machine.emit(Op.LOADL, 1);			 //  1 on stack (# of fields in class "Counter")
		Machine.emit(Prim.newobj);	         // create new instance of "Counter" on heap
			                                 // heap addr returned becomes initial value 
											 // of "counter"
		
		Machine.emit(Op.LOADL,3);            // 3 on stack (argument for "increase")  
		Machine.emit(Op.LOAD,Reg.LB,3);      // current value of "counter" (heap address)
		                                     // on stack is instance on which "increase" acts
		Machine.emit(Op.CALLI,Reg.CB,codeAddr_increase);   // we saved the code address
		                                     // address of the "increase" method previously
		
		Machine.emit(Op.LOAD,Reg.LB,3);      // "counter" object address on stack
		Machine.emit(Op.LOADL, 0);           // field 0 on stack ("count" field)
		Machine.emit(Prim.fieldref);         // returns value of "counter.count" on stack
		Machine.emit(Prim.putintnl);         // print value
		
		Machine.emit(Op.RETURN,0,0,1);       // return popping one caller arg 
                                             // and returning no value
		
	/*
	 *  Postamble
	 *    patch jumps and calls to unknown code addresses
	 *  
	 */
		Machine.patch(patchAddr_Call_main, codeAddr_main);
		                                     // supply correct address of "main" to
		                                     // generated call in preamble
		
	
	/*
	 * save object code and corresponding assembly code
	 */

		/* write code to object code file */
		String objectCodeFileName = "Counter.mJAM";
		ObjectFile objF = new ObjectFile(objectCodeFileName);
		System.out.print("Writing object code file " + objectCodeFileName + " ... ");
		if (objF.write()) {
			System.out.println("FAILED!");
			return;
		}
		else
			System.out.println("SUCCEEDED");	
		
		/* create asm file using disassembler */
		String asmCodeFileName = "Counter.asm";
		System.out.print("Writing assembly file ... ");
		Disassembler d = new Disassembler(objectCodeFileName);
		if (d.disassemble()) {
			System.out.println("FAILED!");
			return;
		}
		else
			System.out.println("SUCCEEDED");
		
	/* 
	 * run code using debugger
	 * 
	 */
		System.out.println("Running code ... ");
		Interpreter.debug(objectCodeFileName, asmCodeFileName);

		System.out.println("*** mJAM execution completed");
	}
}
