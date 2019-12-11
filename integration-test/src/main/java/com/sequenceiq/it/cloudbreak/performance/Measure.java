package com.sequenceiq.it.cloudbreak.performance;

import java.util.List;
import java.util.stream.Stream;

public interface Measure {
    void add(PerformanceIndicator pi);

    void addAll(Measure measure);

    void addAll(List<PerformanceIndicator> measures);

    List<PerformanceIndicator> getAll();

    Stream<PerformanceIndicator> stream();
}
