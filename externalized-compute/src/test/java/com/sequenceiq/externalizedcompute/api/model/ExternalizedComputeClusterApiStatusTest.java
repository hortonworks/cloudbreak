package com.sequenceiq.externalizedcompute.api.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;

class ExternalizedComputeClusterApiStatusTest {

    @ParameterizedTest
    @EnumSource(ExternalizedComputeClusterStatusEnum.class)
    public void testFromEntityToApi(ExternalizedComputeClusterStatusEnum clusterStatusEnum) {
        ExternalizedComputeClusterApiStatus.valueOf(clusterStatusEnum.name());
    }

    @ParameterizedTest
    @EnumSource(ExternalizedComputeClusterApiStatus.class)
    public void testFromApiToEntity(ExternalizedComputeClusterApiStatus clusterClusterStatusResponse) {
        ExternalizedComputeClusterStatusEnum.valueOf(clusterClusterStatusResponse.name());
    }
}