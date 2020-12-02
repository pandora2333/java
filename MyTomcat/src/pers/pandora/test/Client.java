package pers.pandora.test;

import pers.pandora.core.SerialSessionSupport;
import pers.pandora.core.SerialSessionSupportSimpler;
import pers.pandora.core.Session;
import pers.pandora.utils.IdWorker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Client {
    public static void main(String[] args) {
        //It tests session serialization
        try {
//            Map<String, Session> map = new HashMap<>();
//            IdWorker idWorker = new IdWorker();
//            for (int i = 0; i < 10; i++) {
//                Session session = new Session();
//                session.setSessionID(idWorker.nextSessionID());
//                session.getAttrbuites().put(idWorker.nextShort(), idWorker.nextId());
//                map.put(session.getSessionID(), session);
//            }
//            SerialSessionSupport.getSessionPool().put("pandora_test_1", map);
            SerialSessionSupportSimpler simpler = new SerialSessionSupportSimpler();
//            simpler.serialSession("pandora_test_1");
            simpler.deserialSession("pandora_test_1").forEach((k, v) -> {
                System.out.println(k + ":{" + v.getSessionID() + "," + v.getAttrbuites() + "}");
            });
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}