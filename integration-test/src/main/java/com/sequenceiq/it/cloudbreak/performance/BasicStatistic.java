package com.sequenceiq.it.cloudbreak.performance;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BasicStatistic extends KeyMeasurement {
    private String action;

    private Double average;

    private Double deviation;

    private BasicStatistic(String action, double average, double deviation) {
        this.action = action;
        this.average = average;
        this.deviation = deviation;
    }

    public String getAction() {
        return action;
    }

    public Double getAverage() {
        return average;
    }

    public Double getDeviation() {
        return deviation;
    }

    public static KeyPerformanceIndicator<BasicStatistic> build(Measure measure) {
        if (measure == null || measure.getAll().size() == 0) {
            return null;
        }
        Map<String, Double> avarages = measure.stream().collect(
                Collectors.groupingBy(PerformanceIndicator::getAction, Collectors.averagingLong(PerformanceIndicator::getDuration))
        );
        Map<String, List<PerformanceIndicator>> groupedPIs = measure.stream().collect(
                Collectors.groupingBy(PerformanceIndicator::getAction)
        );
        List<BasicStatistic> result = groupedPIs.entrySet().stream().map(entry -> {
            Double deviation = Double.NaN;
            if (entry.getValue().size() > 1) {
                deviation = Math.sqrt(
                        entry.getValue().stream().collect(
                                Collectors.summingDouble(pi -> Math.pow(pi.getDuration() - avarages.get(entry.getKey()), 2))
                        ) / (entry.getValue().size() - 1));
            }
            return new BasicStatistic(entry.getKey(), avarages.get(entry.getKey()), deviation);
        }).collect(Collectors.toList());
        result.sort((x, y) -> y.getAverage().compareTo(x.getAverage()));
        KeyPerformanceIndicator<BasicStatistic> keyPI = new KeyPerformanceIndicator<>(result);
        keyPI.setFormatter(new BasicStatisticFormatter());
        return keyPI;
    }
}
