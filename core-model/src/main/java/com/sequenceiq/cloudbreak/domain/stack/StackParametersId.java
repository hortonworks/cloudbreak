package com.sequenceiq.cloudbreak.domain.stack;

import java.io.Serializable;

import jakarta.persistence.Column;

public class StackParametersId implements Serializable {
    @Column(name = "stack_id")
    private Long stackId;

    @Column(name = "\"key\"")
    private String key;
}
