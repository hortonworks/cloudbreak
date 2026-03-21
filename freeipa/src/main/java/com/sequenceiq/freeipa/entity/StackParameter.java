package com.sequenceiq.freeipa.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "stack_parameter")
@IdClass(StackParameterId.class)
public class StackParameter implements Serializable {
    @Id
    @Column(name = "stack_id")
    private Long stackId;

    @Id
    @Column(name = "paramkey")
    private String paramKey;

    @Column(name = "paramvalue", columnDefinition = "TEXT")
    private String paramValue;

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }
}
