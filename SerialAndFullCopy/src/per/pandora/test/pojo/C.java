package per.pandora.test.pojo;

public class C {
    private int e;
    public int f;
    private D d;
    public void setE(int e){
        this.e = e;
    }
    public void setD(D d){this.d = d;}

    public D getD() {
        return d;
    }

    @Override
    public String toString() {
        return "{"+d+"}"+"e:"+e+",f:"+f;
    }
}
