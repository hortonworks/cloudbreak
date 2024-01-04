package com.sequenceiq.cloudbreak.domain.stack;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "stack_parameters")
@IdClass(StackParametersId.class)
public class StackParameters implements Serializable {
    @Id
    @Column(name = "stack_id")
    private Long stackId;

    @Id
    @Column(name = "\"key\"")
    private String key;

    @Column(name = "\"value\"", columnDefinition = "TEXT", length = 100000)
    private String value;

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
