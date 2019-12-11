package com.sequenceiq.it.cloudbreak.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MeasureAll implements Measure {
    private final List<PerformanceIndicator> performanceIndicators = new ArrayList<>();

    public void add(PerformanceIndicator pi) {
        if (!pi.isStopped()) {
            pi.stop();
        }
        performanceIndicators.add(pi);
    }

    public void addAll(Measure measure) {
        addAll(measure.getAll());
    }

    public void addAll(List<PerformanceIndicator> measures) {
        performanceIndicators.addAll(measures);
    }

    @Override
    public List<PerformanceIndicator> getAll() {
        return performanceIndicators;
    }

    public Stream<PerformanceIndicator> stream() {
        return getAll().stream();
    }
}
