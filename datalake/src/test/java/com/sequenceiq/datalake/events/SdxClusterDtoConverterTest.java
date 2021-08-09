package com.sequenceiq.datalake.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.entity.SdxCluster;

class SdxClusterDtoConverterTest {

    public static final String ACCOUNT_ID = "account-id";

    public static final String CLUSTER_CRN = "crn:crn:cdp:datalake:us-west-1:bderriso:datalake:6af62c9f-275d-4a4b-9d0c-882e92737c27";

    public static final String CLUSTER_NAME = "cluster-name";

    public static final long ID = 1234L;

    private SdxClusterDtoConverter sdxClusterDtoConverter;

    @Test
    void sdxClusterToDto() {
        sdxClusterDtoConverter = new SdxClusterDtoConverter();

        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setAccountId(ACCOUNT_ID);
        sdxCluster.setCrn(CLUSTER_CRN);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setId(ID);

        SdxClusterDto result = sdxClusterDtoConverter.sdxClusterToDto(sdxCluster);

        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertEquals(CLUSTER_NAME, result.getName());
        assertEquals(CLUSTER_CRN, result.getResourceCrn());
        assertEquals(CLUSTER_NAME, result.getResourceName());
        assertEquals(ID, result.getId());
    }
}