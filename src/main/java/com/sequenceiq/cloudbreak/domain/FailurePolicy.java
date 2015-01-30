package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class FailurePolicy implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "failure_generator")
    @SequenceGenerator(name = "failure_generator", sequenceName = "sequence_table")
    private Long id;
    private Long threshold;
    @Enumerated(EnumType.STRING)
    private AdjustmentType adjustmentType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getThreshold() {
        return threshold;
    }

    public void setThreshold(Long threshold) {
        this.threshold = threshold;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }
}
