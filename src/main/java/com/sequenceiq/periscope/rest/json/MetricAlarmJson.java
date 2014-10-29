package com.sequenceiq.periscope.rest.json;

import com.sequenceiq.periscope.domain.ComparisonOperator;
import com.sequenceiq.periscope.domain.Metric;

public class MetricAlarmJson extends AbstractAlarmJson {

    private Metric metric;
    private ComparisonOperator comparisonOperator;
    private double threshold;
    private int period;

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(ComparisonOperator comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

}
