package com.sequenceiq.sdx.api.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @ParameterizedTest
    @EnumSource(value = SdxClusterStatusResponse.class, names = {"RUNNING", "DATALAKE_BACKUP_INPROGRESS"})
    void testAvailableStatus(SdxClusterStatusResponse status) {
        assertTrue(status.isAvailable());
    }

    @ParameterizedTest
    @EnumSource(value = SdxClusterStatusResponse.class, names = {"SALT_UPDATE_IN_PROGRESS", "PROVISIONING_FAILED", "DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS"})
    void testNotAvailableStatus(SdxClusterStatusResponse status) {
        assertFalse(status.isAvailable());
    }
}