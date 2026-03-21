package com.sequenceiq.freeipa.entity;

import java.io.Serializable;

import jakarta.persistence.Column;

public class StackParameterId implements Serializable {
    @Column(name = "stack_id")
    private Long stackId;

    @Column(name = "paramkey")
    private String paramKey;
}
