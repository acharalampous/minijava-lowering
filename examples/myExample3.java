class myExample2{
    public static void main(String[] args){
		C c;
        int j;
        j = c.foo(new B());
		c = args;

    }
}

class A{
    int j;
}

class B extends A{
    int q;
}

class C{
    public int foo(B a){
        int j;
        return 1;
    }
}
