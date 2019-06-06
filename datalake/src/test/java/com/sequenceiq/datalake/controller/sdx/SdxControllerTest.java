package com.sequenceiq.datalake.controller.sdx;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class SdxControllerTest {

    private static final String USER_CRN = "crn:altus:iam:us-west-1:hortonworks:user:test@test.com";

    @Mock
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Spy
    private SdxClusterConverter sdxClusterConverter;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxController sdxController;

    @BeforeEach
    void init() {
        when(threadBasedUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
    }

    @Test
    void createTest() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setClusterShape("big");
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        when(sdxService.createSdx(anyString(), anyString(), any(SdxClusterRequest.class), nullable(StackV4Request.class))).thenReturn(sdxCluster);

        SdxClusterRequest createSdxClusterRequest = new SdxClusterRequest();
        createSdxClusterRequest.setAccessCidr("0.0.0.0/0");
        createSdxClusterRequest.setClusterShape("big");
        createSdxClusterRequest.setEnvironment("test-env");
        Map<String, String> tags = new HashMap<>();
        tags.put("tag1", "value1");
        createSdxClusterRequest.setTags(tags);
        SdxClusterResponse sdxClusterResponse = sdxController.create("test-sdx-cluster", createSdxClusterRequest);
        verify(sdxService).createSdx(eq(USER_CRN), eq("test-sdx-cluster"), eq(createSdxClusterRequest), nullable(StackV4Request.class));
        assertEquals("test-sdx-cluster", sdxClusterResponse.getSdxName());
        assertEquals("test-env", sdxClusterResponse.getEnvironmentName());
        assertEquals("crn:sdxcluster", sdxClusterResponse.getSdxCrn());
        assertEquals(SdxClusterStatusResponse.REQUESTED, sdxClusterResponse.getStatus());
    }

    @Test
    void getTest() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setClusterShape("big");
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        when(sdxService.getByAccountIdAndSdxName(anyString(), anyString())).thenReturn(sdxCluster);

        SdxClusterResponse sdxClusterResponse = sdxController.get("test-sdx-cluster");
        assertEquals("test-sdx-cluster", sdxClusterResponse.getSdxName());
        assertEquals("test-env", sdxClusterResponse.getEnvironmentName());
        assertEquals("crn:sdxcluster", sdxClusterResponse.getSdxCrn());
        assertEquals(SdxClusterStatusResponse.REQUESTED, sdxClusterResponse.getStatus());
    }

}