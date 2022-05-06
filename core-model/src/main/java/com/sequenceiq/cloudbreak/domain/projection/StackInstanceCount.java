package com.sequenceiq.cloudbreak.domain.projection;

public interface StackInstanceCount {
    Long getStackId();

    Integer getInstanceCount();

    default String asString() {
        return "StackInstanceCount{stackId='" + getStackId() + "'; instanceCount='" + getInstanceCount() + "'}";
    }
}
