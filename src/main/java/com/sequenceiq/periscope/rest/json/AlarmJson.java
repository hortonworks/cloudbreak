package com.sequenceiq.periscope.rest.json;

import com.sequenceiq.periscope.domain.ComparisonOperator;
import com.sequenceiq.periscope.domain.Metric;

public class AlarmJson implements Json {

    private Long id;
    private String alarmName;
    private String description;
    private Metric metric;
    private double threshold;
    private ComparisonOperator comparisonOperator;
    private int period;
    private Long scalingPolicyId;

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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

    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(ComparisonOperator comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScalingPolicyId() {
        return scalingPolicyId;
    }

    public void setScalingPolicyId(Long scalingPolicyId) {
        this.scalingPolicyId = scalingPolicyId;
    }
}
