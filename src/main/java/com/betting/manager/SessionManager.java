package main.java.com.betting.manager;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SessionManager {

    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());

    private static final long SESSION_TTL = 10L * 60 * 1000;
    //leverage ConcurrentHashMap to guarantee thread safe
    private static final Map<Integer, Session> map = new ConcurrentHashMap<>();

    /**
     * Get or create customer session from memory
     * @param customerId user unique identifier
     * @return user session with uuid format
     */
    public static String getUserSession(int customerId) {
        cleanExpired(); //clean expired session to make sure there is enough memory
        Session s = map.get(customerId);
        if (s != null && !s.expired()) {
            logger.info("Get session from cache for customer " + customerId);
            return s.sessionKey;
        }
        String key = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        map.put(customerId, new Session(key, System.currentTimeMillis() + SESSION_TTL));
        logger.info("Generated session for customer " + customerId);
        return key;
    }


    /**
     * Get customerId by session key
     * @param sessionKey the session key with uuid format
     * @return customer id
     */
    public static Integer findCustomerByKey(String sessionKey) {
        cleanExpired();
        for (Map.Entry<Integer, Session> e : map.entrySet()) {
            if (!e.getValue().expired() && e.getValue().sessionKey.equals(sessionKey)) {
                return e.getKey();
            }
        }
        return null;
    }

    private static void cleanExpired() {
        map.entrySet().removeIf(x -> x.getValue().expired());
    }

    public static class Session {
        String sessionKey;
        long expireTime;

        Session(String sessionKey, long expireTime) {
            this.sessionKey = sessionKey;
            this.expireTime = expireTime;
        }

        boolean expired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}