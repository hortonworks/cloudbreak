package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.converter.AdjustmentTypeConverter;
import com.sequenceiq.common.api.type.AdjustmentType;

@Entity
public class FailurePolicy implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "failurepolicy_generator")
    @SequenceGenerator(name = "failurepolicy_generator", sequenceName = "failurepolicy_id_seq", allocationSize = 1)
    private Long id;

    private Long threshold;

    @Convert(converter = AdjustmentTypeConverter.class)
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

    @Override
    public String toString() {
        return "FailurePolicy{" +
                "id=" + id +
                ", threshold=" + threshold +
                ", adjustmentType=" + adjustmentType +
                '}';
    }
}
