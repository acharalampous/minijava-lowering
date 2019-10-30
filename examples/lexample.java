class lexample{
    public static void main(String[] args){
        A a;
        int j;
        boolean b;
        a = new A();
        j = a.test(100, a, true, new int[3], 2); 
        System.out.println(j);
    }
}

class A{
    int q;
    int t;
    boolean w;
    public int test(int i, A a, boolean k, int[] g, int j){ 
        System.out.println(i);
        w = true;
        System.out.println(w);
        q = 5;
        t = q;
        return 5555; 
    }

    public boolean test2(){
        return true;
    }
}