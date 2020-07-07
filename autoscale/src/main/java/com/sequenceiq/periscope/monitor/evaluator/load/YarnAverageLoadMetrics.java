package com.sequenceiq.periscope.monitor.evaluator.load;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.stereotype.Component;

@Component
public class YarnAverageLoadMetrics {

    private final Integer WINDOW_SIZE = 10;

    private final ConcurrentHashMap<String, CircularFifoQueue<Integer>> clusterMap = new ConcurrentHashMap<>();

    public void addScalingRecommendation(String clusterCrn, Integer nodeCount) {
        CircularFifoQueue<Integer> buf = Optional.ofNullable(clusterMap.get(clusterCrn))
                .orElseGet(() -> {
                    clusterMap.putIfAbsent(clusterCrn, new CircularFifoQueue(WINDOW_SIZE));
                    return clusterMap.get(clusterCrn);
                });
        buf.add(nodeCount);
    }

    public Integer getScalingAverage(String clusterCrn) {
        return Optional.ofNullable(clusterMap.get(clusterCrn))
                .map(clusterBuffer -> clusterBuffer.stream().mapToInt(Integer::intValue).average().getAsDouble())
                .map(avg -> avg.intValue())
                .orElse(0);
    }
}