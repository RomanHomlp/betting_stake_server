package test.java;

import main.java.com.betting.manager.SessionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;
import java.util.Map;


public class SessionManagerTest {

    @BeforeEach
    @AfterEach
    void cleanup() throws NoSuchFieldException, IllegalAccessException {
        Field mapField = SessionManager.class.getDeclaredField("map");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, SessionManager.Session> map = (Map<Integer, SessionManager.Session>) mapField.get(null);
        if (map != null) {
            map.clear();
        }
    }

    @Test
    void testGetUserSession_CreatesNewSession() {
        int customerId = 1001;

        String sessionKey = SessionManager.getUserSession(customerId);

        assertNotNull(sessionKey, "Session key should not be null");
        assertEquals(32, sessionKey.length());

        String sessionKeyAgain = SessionManager.getUserSession(customerId);
        assertEquals(sessionKey, sessionKeyAgain, "Same customer should get the same session key within TTL");
    }

    @Test
    void testFindCustomerByKey_ValidKey() {
        int customerId = 2002;
        String expectedKey = SessionManager.getUserSession(customerId);

        Integer foundId = SessionManager.findCustomerByKey(expectedKey);

        assertEquals(customerId, foundId);
    }

    @Test
    void testFindCustomerByKey_InvalidKey() {
        String invalidKey = "invalid";
        Integer foundId = SessionManager.findCustomerByKey(invalidKey);
        assertNull(foundId, "Should return null for invalid session key");
    }

    @Test
    void testSessionExpiration() throws NoSuchFieldException, IllegalAccessException {
        int customerId = 3003;
        String sessionKey = SessionManager.getUserSession(customerId);

        assertNotNull(SessionManager.findCustomerByKey(sessionKey));

        forceExpireSession(customerId);

        SessionManager.getUserSession(9999);

        Integer foundId = SessionManager.findCustomerByKey(sessionKey);
        assertNull(foundId, "Session should be null after expiration and cleanup");

        String newKey = SessionManager.getUserSession(customerId);
        assertNotNull(newKey);
        assertNotEquals(sessionKey, newKey, "Should generate a new key after old one expired");
    }

    @Test
    void testConcurrentAccess_SanityCheck() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        final boolean[] errorOccurred = {false};

        for (int i = 0; i < threadCount; i++) {
            final int id = 5000 + i;
            threads[i] = new Thread(() -> {
                try {
                    String key = SessionManager.getUserSession(id);
                    Integer storedId = SessionManager.findCustomerByKey(key);
                    if (storedId != id) {
                        errorOccurred[0] = true;
                    }
                } catch (Exception e) {
                    errorOccurred[0] = true;
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertFalse(errorOccurred[0], "No errors should occur during concurrent access");
    }

    private void forceExpireSession(int customerId) throws NoSuchFieldException, IllegalAccessException {
        Field mapField = SessionManager.class.getDeclaredField("map");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, SessionManager.Session> map = (Map<Integer, SessionManager.Session>) mapField.get(null);

        SessionManager.Session session = map.get(customerId);
        if (session != null) {
            Field expireTimeField = SessionManager.Session.class.getDeclaredField("expireTime");
            expireTimeField.setAccessible(true);
            expireTimeField.setLong(session, System.currentTimeMillis() - 1);
        }
    }
}