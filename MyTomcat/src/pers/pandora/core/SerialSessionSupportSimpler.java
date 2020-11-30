package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.utils.CollectionUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * by java in a simple  way
 */
public class SerialSessionSupportSimpler extends SerialSessionSupport {

    private static Logger logger = LogManager.getLogger(SerialSessionSupportSimpler.class);

    @Override
    public void serialSession(String serverName) throws IOException {
        final Map<String, Session> tmp = new HashMap<>();
        Set<String> excludesessions = getExcludeSessions();
        Optional.ofNullable(getSessionPool().get(serverName)).get().forEach((k, v) -> {
            if (!excludesessions.contains(serverName + HTTPStatus.TRANSVERSE + k)) {
                tmp.put(k, v);
            }
        });
        if (CollectionUtil.isNotEmptry(tmp)) {
            ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(
                    new File(SESSIONPATH + serverName + HTTPStatus.TRANSVERSE + SESSIONFILE_POS)));
            oo.writeObject(tmp);
            oo.close();
        }
    }

    @Override
    public Map<String, Session> deserialSession(String serverName) throws IOException, ClassNotFoundException {
        File file = new File(SESSIONPATH + serverName + HTTPStatus.TRANSVERSE + SESSIONFILE_POS);
        if (!file.exists()) {
            logger.warn("Not Found Session File");
            return new HashMap<>();
        }
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        Map<String, Session> map = (Map<String, Session>) ois.readObject();
        ois.close();
        return map;
    }

    @Override
    public void excliudeSession(String serverName, String sessionID) {
        getExcludeSessions().add(serverName + HTTPStatus.TRANSVERSE + sessionID);
    }

    @Override
    public void invalidSession(String serverName, String sessionID) {
        getExcludeSessions().remove(serverName + HTTPStatus.TRANSVERSE + sessionID);
    }
}
