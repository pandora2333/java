package pers.pandora.test;

public class Demo {
    public static void main(String[] args) {
        new Sub();
    }
}
class Sub extends  Super{
    static  int a=12;
    static  int b = 11;
    public  Sub(){
//        System.out.println(a);
    }

    public  void test() {
        System.out.println(a);
    }
}
class  Super{
    static int a = 45;
    static  boolean b =true;
    public  Super(){
        this.test();
    }
   public  void test(){
//        this.a = 11;
       System.out.println(a);
   }
}
