package com.sequenceiq.periscope.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@DiscriminatorValue("METRIC")
public class MetricAlarm extends BaseAlarm {

    @Enumerated(EnumType.STRING)
    private Metric metric;
    @Enumerated(EnumType.STRING)
    private ComparisonOperator comparisonOperator;
    private double threshold;
    private int period;
    @Transient
    private long alarmHitsSince;

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

    public long getAlarmHitsSince() {
        return alarmHitsSince;
    }

    public void setAlarmHitsSince(long alarmHitsSince) {
        this.alarmHitsSince = alarmHitsSince;
    }

    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(ComparisonOperator comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    @Override
    public void reset() {
        setAlarmHitsSince(0);
        setNotificationSent(false);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o, "notificationSent", "notifications", "scalingPolicy", "alarmHitsSince");
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "notificationSent", "notifications", "scalingPolicy", "alarmHitsSince");
    }
}