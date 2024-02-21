package com.sequenceiq.externalizedcompute.entity;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class ExternalizedComputeClusterStatusEnumConverter extends DefaultEnumConverter<ExternalizedComputeClusterStatusEnum> {

    @Override
    public ExternalizedComputeClusterStatusEnum getDefault() {
        return ExternalizedComputeClusterStatusEnum.AVAILABLE;
    }

}
