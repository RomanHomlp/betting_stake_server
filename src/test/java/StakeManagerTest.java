package test.java;

import main.java.com.betting.manager.StakeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class StakeManagerTest {

    @BeforeEach
    @AfterEach
    void cleanup() throws NoSuchFieldException, IllegalAccessException {
        Field mapField = StakeManager.class.getDeclaredField("customerStakeMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, Map<Integer, Integer>> map = (Map<Integer, Map<Integer, Integer>>) mapField.get(null);
        if (map != null) {
            map.clear();
        }
    }

    @Test
    void testAddStake_singleUserAccumulation() {
        int betOfferId = 101;
        int customerId = 201;

        StakeManager.addStake(betOfferId, customerId, 100);
        StakeManager.addStake(betOfferId, customerId, 50);

        String result = StakeManager.getHighStakes(betOfferId);

        assertEquals("201=150", result);
    }

    @Test
    void testGetHighStakes_emptyResult() {
        String result = StakeManager.getHighStakes(999);
        assertEquals("", result);
    }

    @Test
    void testGetHighStakes_sortingAndLimit() {
        int betOfferId = 102;

        for (int i = 1; i <= 25; i++) {
            StakeManager.addStake(betOfferId, i, i * 100);
        }

        String result = StakeManager.getHighStakes(betOfferId);

        String[] parts = result.split(",");

        assertEquals(20, parts.length, "Should only return top 20");

        assertTrue(parts[0].startsWith("25="), "First element should be the highest stake");
        assertEquals("25=2500", parts[0]);

        assertTrue(parts[19].startsWith("6="), "Last element should be the 20th highest stake");
        assertEquals("6=600", parts[19]);
    }

    @Test
    void testAddStake_ConcurrentSafety() throws InterruptedException {
        int betOfferId = 200;
        int threadCount = 10;
        int stakesPerThread = 1000;
        int stakeAmount = 1;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        int customerId = 1;

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < stakesPerThread; j++) {
                        StakeManager.addStake(betOfferId, customerId, stakeAmount);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(1, TimeUnit.SECONDS);
        executor.shutdown();

        int expectedTotal = threadCount * stakesPerThread * stakeAmount;

        String result = StakeManager.getHighStakes(betOfferId);
        assertEquals(customerId + "=" + expectedTotal, result);
    }

    @Test
    void testMultipleBetOffersIsolation() {
        int betOfferA = 301;
        int betOfferB = 302;
        int customerId = 505;

        StakeManager.addStake(betOfferA, customerId, 100);
        StakeManager.addStake(betOfferB, customerId, 200);

        String resultA = StakeManager.getHighStakes(betOfferA);
        String resultB = StakeManager.getHighStakes(betOfferB);

        assertEquals("505=100", resultA);
        assertEquals("505=200", resultB);
    }
}