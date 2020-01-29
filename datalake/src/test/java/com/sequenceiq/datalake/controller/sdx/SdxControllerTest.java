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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.notification.NotificationService;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
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

    @Test
    void filteredClusterDetailsTest() throws Exception {

        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getSdxByNameInAccount(anyString(), anyString())).thenReturn(sdxCluster);

        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.REQUESTED);
        sdxStatusEntity.setStatusReason("statusreason");
        sdxStatusEntity.setCreated(1L);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);
        FieldSetter.setField(sdxClusterConverter, SdxClusterConverter.class.getDeclaredField("sdxStatusService"), sdxStatusService);

        StackV4Response stackV4Response = getValidStackV4Response();
        when(sdxService.getDetail(anyString(), any(Set.class))).thenReturn(stackV4Response);

        SdxClusterDetailResponse sdxClusterDetailResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.getDetail("test-sdx-cluster", Stream.of("hardware_info").collect(Collectors.toSet())));

        assertEquals(1, sdxClusterDetailResponse.getStackV4Response().getCluster().getExposedServices().get("cdp-proxy").size());
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        return sdxCluster;
    }

    private StackV4Response getValidStackV4Response() {

        ClusterExposedServiceV4Response nameNode = new ClusterExposedServiceV4Response();
        nameNode.setServiceName("NAMENODE");
        ClusterExposedServiceV4Response solrServer = new ClusterExposedServiceV4Response();
        solrServer.setServiceName("SOLR_SERVER");
        ClusterExposedServiceV4Response webHdfsUi = new ClusterExposedServiceV4Response();
        webHdfsUi.setServiceName("MASTER");
        ClusterExposedServiceV4Response ranger = new ClusterExposedServiceV4Response();
        ranger.setServiceName("RANGER_ADMIN");

        StackV4Response stackV4Response = new StackV4Response();
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setExposedServices(new HashMap<>());
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.getCluster().getExposedServices().put("cdp-proxy",
                Stream.of(nameNode, solrServer, webHdfsUi, ranger).collect(Collectors.toList())
        );
        stackV4Response.getCluster().getExposedServices().put("cdp-proxy-api",
                Stream.of(nameNode, solrServer, webHdfsUi, ranger).collect(Collectors.toList())
        );
        return stackV4Response;
    }

}
