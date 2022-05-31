package com.sequenceiq.datalake.controller.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.SdxImageCatalogService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxChangeImageCatalogRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxGenerateImageCatalogResponse;

@ExtendWith(MockitoExtension.class)
class SdxControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:test@test.com";

    private static final String SDX_CLUSTER_NAME = "test-sdx-cluster";

    @Mock
    private SdxStatusService sdxStatusService;

    @Spy
    private SdxClusterConverter sdxClusterConverter;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxMetricService metricService;

    @Mock
    private SdxImageCatalogService sdxImageCatalogService;

    @InjectMocks
    private SdxController sdxController;

    @Test
    void createTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.createSdx(anyString(), anyString(), any(SdxClusterRequest.class), nullable(StackV4Request.class)))
                .thenReturn(Pair.of(sdxCluster, new FlowIdentifier(FlowType.FLOW, "FLOW_ID")));

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
        ReflectionTestUtils.setField(sdxClusterConverter, "sdxStatusService", sdxStatusService);
        SdxClusterResponse sdxClusterResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.create(SDX_CLUSTER_NAME, createSdxClusterRequest));
        verify(sdxService).createSdx(eq(USER_CRN), eq(SDX_CLUSTER_NAME), eq(createSdxClusterRequest), nullable(StackV4Request.class));
        verify(sdxStatusService, times(1)).getActualStatusForSdx(sdxCluster);
        assertEquals(SDX_CLUSTER_NAME, sdxClusterResponse.getName());
        assertEquals("test-env", sdxClusterResponse.getEnvironmentName());
        assertEquals("crn:sdxcluster", sdxClusterResponse.getCrn());
        assertEquals(SdxClusterStatusResponse.REQUESTED, sdxClusterResponse.getStatus());
        assertEquals("statusreason", sdxClusterResponse.getStatusReason());
    }

    @Test
    void getTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(anyString(), anyString())).thenReturn(sdxCluster);

        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.REQUESTED);
        sdxStatusEntity.setStatusReason("statusreason");
        sdxStatusEntity.setCreated(1L);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);
        ReflectionTestUtils.setField(sdxClusterConverter, "sdxStatusService", sdxStatusService);

        SdxClusterResponse sdxClusterResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.get(SDX_CLUSTER_NAME));
        assertEquals(SDX_CLUSTER_NAME, sdxClusterResponse.getName());
        assertEquals("test-env", sdxClusterResponse.getEnvironmentName());
        assertEquals("crn:sdxcluster", sdxClusterResponse.getCrn());
        assertEquals(SdxClusterStatusResponse.REQUESTED, sdxClusterResponse.getStatus());
        assertEquals("statusreason", sdxClusterResponse.getStatusReason());
    }

    @Test
    void changeImageCatalogTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(any(), anyString())).thenReturn(sdxCluster);

        SdxChangeImageCatalogRequest request = new SdxChangeImageCatalogRequest();
        request.setImageCatalog("image-catalog");

        sdxController.changeImageCatalog(sdxCluster.getName(), request);

        verify(sdxImageCatalogService).changeImageCatalog(sdxCluster, request.getImageCatalog());
    }

    @Test
    void generateImageCatalogTest() {
        CloudbreakImageCatalogV3 imageCatalog = mock(CloudbreakImageCatalogV3.class);
        when(sdxImageCatalogService.generateImageCatalog(SDX_CLUSTER_NAME)).thenReturn(imageCatalog);

        SdxGenerateImageCatalogResponse actual = sdxController.generateImageCatalog(SDX_CLUSTER_NAME);

        assertEquals(imageCatalog, actual.getImageCatalog());
    }

    @Test
    void enableRangerRazByCrnTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.enableRangerRazByCrn(sdxCluster.getCrn()));

        verify(sdxService).updateRangerRazEnabled(sdxCluster);
    }

    @Test
    void enableRangerRazByNameTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(anyString(), anyString())).thenReturn(sdxCluster);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.enableRangerRazByName(sdxCluster.getName()));

        verify(sdxService).updateRangerRazEnabled(sdxCluster);
    }

    @Test
    void rotateSaltPasswordByName() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByCrn(USER_CRN, sdxCluster.getCrn())).thenReturn(sdxCluster);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.rotateSaltPasswordByCrn(sdxCluster.getCrn()));

        verify(sdxService).rotateSaltPassword(sdxCluster);
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(SDX_CLUSTER_NAME);
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        return sdxCluster;
    }

}
