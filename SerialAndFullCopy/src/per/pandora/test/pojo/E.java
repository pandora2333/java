package per.pandora.test.pojo;

public class E {
    private String ee = "E str";
    public float t;

    public void setEe(String ee) {
        this.ee = ee;
    }

    @Override
    public String toString() {
        return "ee:"+ee+",t:"+t;
    }
}
