package pers.pandora.web.test;

import pers.pandora.web.core.SerializeSessionSupportSimpler;
import pers.pandora.web.core.StartUper;

import java.io.IOException;

@Deprecated
public class Client {

    private String testOM;

    public void setTestOM(String testOM) {
        this.testOM = testOM;
    }

    public String getTestOM() {
        return testOM;
    }

    public static void testSerialSession(){
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
//            SerializeSessionSupport.getSessionPool().put("pandora_test_1", map);
            SerializeSessionSupportSimpler simpler = new SerializeSessionSupportSimpler();
//            simpler.serializeSession("pandora_test_1");
            simpler.deserializeSession("pandora_test_1").forEach((k, v) -> {
                System.out.println(k + ":{" + v.getSessionID() + "," + v.getAttrbuites() + "}");
            });
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void testStartUper(){
        //It gives two ways to init-servers:
        //1.Absolute path
        //2.Relative path:It is relative to the current project directory. If it is under SRC, it should be src/ + your path "src/Pandora_test_ws_1.properties"
        StartUper startUper = new StartUper("src/Pandora_test_1.properties");
        startUper.start(true);
    }

    public static void main(String[] args) {
//        testSerialSession();
        testStartUper();
    }
}
