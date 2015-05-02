package miniJava.CodeGenerator;

public class ClassRuntimeEntity extends RuntimeEntity {

	int staticFieldDisplacement; // displacement from SB
	int staticFieldSize; // number of static fields
	int instanceFieldSize;
	boolean hasStaticFields = false;
	
	public ClassRuntimeEntity(int staticFieldLocation, int staticFieldOffset, int instanceFieldDisplacement) {
		staticFieldDisplacement = staticFieldLocation;
		staticFieldSize = staticFieldOffset;
		instanceFieldSize = instanceFieldDisplacement;
		hasStaticFields = true;
	}
	
	public ClassRuntimeEntity(int instanceFieldOffset) {
		staticFieldDisplacement = -1;
		staticFieldSize = -1;
		instanceFieldSize = instanceFieldOffset;
	}
	
	public void setInstanceFieldSize(int instanceFieldOffset) {
		instanceFieldSize = instanceFieldOffset;
	}
	
	public boolean hasStaticFields() {
		return hasStaticFields;
	}
}
