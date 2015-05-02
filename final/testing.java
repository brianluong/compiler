/* miniJava test program
 *Pass3
 *Constructor
 */
class A{
	static int a = 20;
	public static void main(String[] args){
		B b = new B(A.a);
		System.out.println(b.c.x);
    }
}

class B {
	int x;
	C c;
	public B(int x) {
		this.x = x;
	}
	
	public B(int x) {
		
	}
	
}

class C{	
	int x;
}