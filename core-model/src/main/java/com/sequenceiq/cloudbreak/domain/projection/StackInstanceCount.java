package com.sequenceiq.cloudbreak.domain.projection;

public interface StackInstanceCount {

    Long getStackId();

    Integer getInstanceCount();
}