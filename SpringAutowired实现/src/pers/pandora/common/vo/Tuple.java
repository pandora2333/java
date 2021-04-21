package pers.pandora.common.vo;

public class Tuple<K1, K2, V> {
    private K1 k1;
    private K2 k2;
    private V v;

    public Tuple(K1 k1, K2 k2, V v) {
        this.k1 = k1;
        this.k2 = k2;
        this.v = v;
    }

    public K1 getK1() {
        return k1;
    }

    public K2 getK2() {
        return k2;
    }

    public V getV() {
        return v;
    }

    public void setK1(K1 k1) {
        this.k1 = k1;
    }

    public void setK2(K2 k2) {
        this.k2 = k2;
    }

    public void setV(V v) {
        this.v = v;
    }
}
