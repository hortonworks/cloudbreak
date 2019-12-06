package com.sequenceiq.datalake.controller.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
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
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.notification.NotificationService;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class SdxControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:test@test.com";

    @Mock
    private SdxStatusService sdxStatusService;

    @Spy
    private SdxClusterConverter sdxClusterConverter;

    @Mock
    private SdxService sdxService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SdxController sdxController;

    @BeforeEach
    void init() {
    }

    @Test
    void createTest() throws NoSuchFieldException {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.createSdx(anyString(), anyString(), any(SdxClusterRequest.class), nullable(StackV4Request.class))).thenReturn(sdxCluster);

        SdxClusterRequest createSdxClusterRequest = new SdxClusterRequest();
        createSdxClusterRequest.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        createSdxClusterRequest.setEnvironment("test-env");
        Map<String, String> tags = new HashMap<>();
        tags.put("tag1", "value1");
        createSdxClusterRequest.addTags(tags);
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.REQUESTED);
        sdxStatusEntity.setStatusReason("statusreason");
        sdxStatusEntity.setCreated(1L);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);
        FieldSetter.setField(sdxClusterConverter, SdxClusterConverter.class.getDeclaredField("sdxStatusService"), sdxStatusService);
        SdxClusterResponse sdxClusterResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.create("test-sdx-cluster", createSdxClusterRequest));
        verify(sdxService).createSdx(eq(USER_CRN), eq("test-sdx-cluster"), eq(createSdxClusterRequest), nullable(StackV4Request.class));
        verify(sdxStatusService, times(1)).getActualStatusForSdx(sdxCluster);
        assertEquals("test-sdx-cluster", sdxClusterResponse.getName());
        assertEquals("test-env", sdxClusterResponse.getEnvironmentName());
        assertEquals("crn:sdxcluster", sdxClusterResponse.getCrn());
        assertEquals(SdxClusterStatusResponse.REQUESTED, sdxClusterResponse.getStatus());
        assertEquals("statusreason", sdxClusterResponse.getStatusReason());
    }

    @Test
    void getTest() throws NoSuchFieldException {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getSdxByNameInAccount(anyString(), anyString())).thenReturn(sdxCluster);

        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.REQUESTED);
        sdxStatusEntity.setStatusReason("statusreason");
        sdxStatusEntity.setCreated(1L);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);
        FieldSetter.setField(sdxClusterConverter, SdxClusterConverter.class.getDeclaredField("sdxStatusService"), sdxStatusService);

        SdxClusterResponse sdxClusterResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.get("test-sdx-cluster"));
        assertEquals("test-sdx-cluster", sdxClusterResponse.getName());
        assertEquals("test-env", sdxClusterResponse.getEnvironmentName());
        assertEquals("crn:sdxcluster", sdxClusterResponse.getCrn());
        assertEquals(SdxClusterStatusResponse.REQUESTED, sdxClusterResponse.getStatus());
        assertEquals("statusreason", sdxClusterResponse.getStatusReason());
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        return sdxCluster;
    }

}
