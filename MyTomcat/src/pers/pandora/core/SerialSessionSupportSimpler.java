package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.LOG;
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

    private static final Logger logger = LogManager.getLogger(SerialSessionSupportSimpler.class);

    @Override
    public void serialSession(final String serverName) throws IOException {
        final Map<String, Session> tmp = new HashMap<>(16);
        final Set<String> excludesessions = getExcludeSessions();
        Optional.ofNullable(getSessionPool().get(serverName)).get().forEach((k, v) -> {
            if (!excludesessions.contains(serverName + HTTPStatus.TRANSVERSE + k)) {
                tmp.put(k, v);
            }
        });
        if (CollectionUtil.isNotEmptry(tmp)) {
            final ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(
                    new File(SESSIONPATH + serverName + HTTPStatus.TRANSVERSE + SESSIONFILE_POS)));
            oo.writeObject(tmp);
            oo.close();
        }
    }

    @Override
    public Map<String, Session> deserialSession(final String serverName) throws IOException, ClassNotFoundException {
        final File file = new File(SESSIONPATH + serverName + HTTPStatus.TRANSVERSE + SESSIONFILE_POS);
        if (!file.exists()) {
            logger.warn(LOG.LOG_PRE + "Not Found Session File", serverName);
            return new HashMap<>();
        }
        final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        final Map<String, Session> map = (Map<String, Session>) ois.readObject();
        ois.close();
        //init session and delete source file
        file.delete();
        return map;
    }

    @Override
    public void excliudeSession(final String serverName, final String sessionID) {
        getExcludeSessions().add(serverName + HTTPStatus.TRANSVERSE + sessionID);
    }

    @Override
    public void invalidSession(final String serverName, final String sessionID) {
        getExcludeSessions().remove(serverName + HTTPStatus.TRANSVERSE + sessionID);
    }
}
