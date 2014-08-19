package com.sequenceiq.periscope.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

@Entity
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_generator")
    @SequenceGenerator(name = "template_generator", sequenceName = "sequence_table")
    private long id;
    private String alarmName;
    private String description;
    private Metric metric;
    private double threshold;
    private ComparisonOperator comparisonOperator;
    private int period;
    @Transient
    private long alarmHitsSince;
    @OneToOne(cascade = CascadeType.ALL)
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

    public ScalingPolicy getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicy scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}