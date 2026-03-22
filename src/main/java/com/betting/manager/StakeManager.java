package main.java.com.betting.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StakeManager {

    // betOfferId -> (customerId -> total stake)
    private static final Map<Integer, Map<Integer, Integer>> customerStakeMap = new ConcurrentHashMap<>();

    /**
     * accumulate the stake amount for a given customer under a bet offer in a thread-safe manner
     * @param betOfferId unique identifier of a bet offer
     * @param customerId customer id
     * @param stake single stake
     */
    public static void addStake(int betOfferId, int customerId, int stake) {
        customerStakeMap.computeIfAbsent(betOfferId, k -> new ConcurrentHashMap<>())
                .merge(customerId, stake, Integer::sum);
    }

    /**
     * Get the top 20 customers with the highest stakes for a bet offer
     * @param betOfferId unique identifier of a bet offer
     * @return the top 20 stake list
     */
    public static String getHighStakes(int betOfferId) {
        Map<Integer, Integer> singleBetOffer = customerStakeMap.get(betOfferId);

        if (singleBetOffer == null || singleBetOffer.isEmpty())
            return "";

        return singleBetOffer.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed()) //comparing by total stake desc
                .limit(20)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }
}