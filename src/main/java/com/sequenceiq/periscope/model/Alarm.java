package com.sequenceiq.periscope.model;

public class Alarm {

    private String id;
    private String alarmName;
    private String description;
    private Metric metric;
    private double threshold;
    private ComparisonOperator comparisonOperator;
    private int period;
    private long alarmHitsSince;
    private ScalingPolicy scalingPolicy;

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

    public long getAlarmHitsSince() {
        return alarmHitsSince;
    }

    public void setAlarmHitsSince(long alarmHitsSince) {
        this.alarmHitsSince = alarmHitsSince;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ScalingPolicy getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicy scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }
}