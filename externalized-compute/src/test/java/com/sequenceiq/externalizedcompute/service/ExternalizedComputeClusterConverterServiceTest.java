package com.sequenceiq.externalizedcompute.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatus;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;

@ExtendWith(MockitoExtension.class)
public class ExternalizedComputeClusterConverterServiceTest {

    @Mock
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Mock
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @InjectMocks
    private ExternalizedComputeClusterConverterService externalizedComputeClusterConverterService;

    @Test
    void convertToResponse() {
        ExternalizedComputeCluster externalizedComputeCluster = new ExternalizedComputeCluster();
        externalizedComputeCluster.setName("ext-cluster");
        externalizedComputeCluster.setEnvironmentCrn("envCrn");
        externalizedComputeCluster.setLiftieName("liftiename");
        externalizedComputeCluster.setResourceCrn("ext-cluster-crn");
        externalizedComputeCluster.setCreated(123L);
        externalizedComputeCluster.setId(1L);
        externalizedComputeCluster.setAccountId("accid");
        ExternalizedComputeClusterStatus status = new ExternalizedComputeClusterStatus();
        status.setExternalizedComputeCluster(externalizedComputeCluster);
        status.setStatus(ExternalizedComputeClusterStatusEnum.AVAILABLE);
        status.setStatusReason("available");
        String liftieCrn = "crn:cdp:compute:us-west-1:accid:cluster:liftiename";
        when(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster))
                .thenReturn(liftieCrn);
        when(externalizedComputeClusterStatusService.getActualStatus(externalizedComputeCluster)).thenReturn(status);
        ExternalizedComputeClusterResponse externalizedComputeClusterResponse = externalizedComputeClusterConverterService.convertToResponse(
                externalizedComputeCluster);
        assertEquals(liftieCrn, externalizedComputeClusterResponse.getLiftieClusterCrn());
        assertEquals(ExternalizedComputeClusterApiStatus.AVAILABLE, externalizedComputeClusterResponse.getStatus());
        assertEquals("available", externalizedComputeClusterResponse.getStatusReason());
    }

    @Test
    void convertToResponseNoLiftieName() {
        ExternalizedComputeCluster externalizedComputeCluster = new ExternalizedComputeCluster();
        externalizedComputeCluster.setName("ext-cluster");
        externalizedComputeCluster.setEnvironmentCrn("envCrn");
        externalizedComputeCluster.setResourceCrn("ext-cluster-crn");
        externalizedComputeCluster.setCreated(123L);
        externalizedComputeCluster.setId(1L);
        externalizedComputeCluster.setAccountId("accid");
        ExternalizedComputeClusterStatus status = new ExternalizedComputeClusterStatus();
        status.setExternalizedComputeCluster(externalizedComputeCluster);
        status.setStatus(ExternalizedComputeClusterStatusEnum.AVAILABLE);
        status.setStatusReason("available");
        when(externalizedComputeClusterStatusService.getActualStatus(externalizedComputeCluster)).thenReturn(status);
        ExternalizedComputeClusterResponse externalizedComputeClusterResponse = externalizedComputeClusterConverterService.convertToResponse(
                externalizedComputeCluster);
        assertNull(externalizedComputeClusterResponse.getLiftieClusterCrn());
        assertEquals(ExternalizedComputeClusterApiStatus.AVAILABLE, externalizedComputeClusterResponse.getStatus());
        assertEquals("available", externalizedComputeClusterResponse.getStatusReason());
    }
}