package per.pandora.test.pojo;

public class B extends  A{
    public int c;
    private int d;
    public void setD(int d){
        this.d = d;
    }
    @Override
    public String toString() {
        return super.toString() + "|c:"+c + ",d:"+d;
    }
}
