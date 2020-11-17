package per.pandora.test.pojo;

public class A extends E{
    public static String s = "A str";
    private int a;
    public int b;
    public C cc;
    public void setA(int a){
        this.a = a;
    }

    @Override
    public String toString() {
        return super.toString()+"|s:"+s+",a:"+a+",b:"+b+"{"+cc+"}";
    }
}
