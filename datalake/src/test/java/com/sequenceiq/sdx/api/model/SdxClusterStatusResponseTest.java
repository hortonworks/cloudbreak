package com.sequenceiq.sdx.api.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;

public class SdxClusterStatusResponseTest {

    @ParameterizedTest
    @EnumSource(DatalakeStatusEnum.class)
    public void testFromEntityToApi(DatalakeStatusEnum datalakeStatusEnum) {
        SdxClusterStatusResponse.valueOf(datalakeStatusEnum.name());
    }

    @ParameterizedTest
    @EnumSource(SdxClusterStatusResponse.class)
    public void testFromApiToEntity(SdxClusterStatusResponse sdxClusterStatusResponse) {
        DatalakeStatusEnum.valueOf(sdxClusterStatusResponse.name());
    }
}