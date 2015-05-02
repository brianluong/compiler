package miniJava.CodeGenerator;

public class MethodRuntimeEntity extends RuntimeEntity {

	int address;
	int numOfParams;
	boolean isStatic = false;
	public MethodRuntimeEntity(int offset) {
		address = offset;
	}
	
	public void addParam(int num) {
		numOfParams = num;
	}
	
	public void setStatic(boolean x) {
		isStatic = x;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
	
}
