package per.pandora.test;

import per.pandora.fullCopy.core.ObjectDeepCopyStrategyFactory;
import per.pandora.serial.core.ObjectSerialFactory;
import per.pandora.test.pojo.*;

public class Main {
    public static void main(String[] args) {
        B b = new B();
        b.setA(-101);
        E e = (E)b;
        e.setEe("B str");
        e.t = 12.0f;
        b.b = 10000;
        b.c = 1000000000;
        b.s = "B str";
        b.setD(-1);
        b.cc = new C();
        b.cc.setE(1010);
        b.cc.f = 3232;
        D d = new D();
        d.setG(-101);
        d.h = -111;
        b.cc.setD(null);
//        System.out.println(b.getClass().getSuperclass());
//        ParameterizedType superclass = (ParameterizedType) (b.getClass().getGenericSuperclass());
//        for(Type type : superclass.getActualTypeArguments()){
//            System.out.println(type.getTypeName());
//        }

//        System.out.println(b);
//        B bb = ObjectDeepCopyStrategyFactory.getInstance().handle(b);
//        System.out.println(bb);
//        System.out.println(b.cc == bb.cc);
//        System.out.println(b.cc.getD() == bb.cc.getD());

        String res = ObjectSerialFactory.getInstance().serialObject(b);
        System.out.println(res);
        System.out.println(ObjectSerialFactory.getInstance().serialObject(ObjectSerialFactory.getInstance().unSerialObject(res,B.class)));

    }
}
