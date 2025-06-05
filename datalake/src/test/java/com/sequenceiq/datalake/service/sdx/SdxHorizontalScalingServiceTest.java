package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeInstanceGroupScalingDetails;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.DatalakeHorizontalScaleRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sdx horizontal scaling service tests")
public class SdxHorizontalScalingServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final Long SDX_ID = 2L;

    private static final String SDX_CRN = "crn";

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @InjectMocks
    private SdxHorizontalScalingService underTest;

    @Test
    void testDatalakeHorizontalScaleInvoke() {
        when(stackV4Endpoint.putScaling(any(), anyString(), any(), anyString())).thenReturn(new FlowIdentifier(FlowType.FLOW, "flowId"));
        SdxCluster sdxCluster = getSdxCluster();
        StackScaleV4Request scaleRequest = new StackScaleV4Request();
        scaleRequest.setGroup(DatalakeInstanceGroupScalingDetails.SOLR_SCALE_OUT.getName());
        scaleRequest.setDesiredCount(1);
        String flowId = underTest.triggerScalingFlow(sdxCluster, scaleRequest);
        assertEquals("flowId", flowId);
        verify(stackV4Endpoint, times(1)).putScaling(eq(0L), eq(sdxCluster.getName()), any(), eq(sdxCluster.getAccountId()));
    }

    @Test
    void testDatalakeHorizontalScaleValidation() {
        SdxCluster sdxCluster = getSdxCluster();

        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setGroup(DatalakeInstanceGroupScalingDetails.AUXILIARY.getName());
        scaleRequest.setDesiredCount(0);
        assertThrows(BadRequestException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));
        scaleRequest.setGroup(DatalakeInstanceGroupScalingDetails.CORE.getName());
        scaleRequest.setDesiredCount(3);
        assertThrows(BadRequestException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));
    }

    @Test
    void testDatalakeHorizontalScaleValidationNotAllowedGroups() {
        SdxCluster sdxCluster = getSdxCluster();
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setGroup(DatalakeInstanceGroupScalingDetails.ATLAS_SCALE_OUT.getName());
        scaleRequest.setDesiredCount(1);
        assertThrows(BadRequestException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));
        scaleRequest.setGroup(DatalakeInstanceGroupScalingDetails.MASTER.getName());
        scaleRequest.setDesiredCount(4);
        assertThrows(BadRequestException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));
        scaleRequest.setDesiredCount(-1);
        assertThrows(BadRequestException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));
    }

    @Test
    void testDatalakeHorizontalScaleDownScaleNotAllowedOnGroup() {
        DetailedEnvironmentResponse environmentDetailedResponse = getEnvironmentDetailedResponse();
        when(environmentClientService.getByName(anyString())).thenReturn(environmentDetailedResponse);
        StackV4Response stackV4Response = new StackV4Response();
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        ClouderaManagerV4Response cm = new ClouderaManagerV4Response();
        ClouderaManagerProductV4Response cdpResponse = new ClouderaManagerProductV4Response();
        cdpResponse.setName("CDH");
        cdpResponse.setVersion("7.2.17");
        cm.setProducts(Collections.singletonList(cdpResponse));
        clusterV4Response.setCm(cm);
        stackV4Response.setCluster(clusterV4Response);
        InstanceGroupV4Response gateway = new InstanceGroupV4Response();
        gateway.setName(DatalakeInstanceGroupScalingDetails.GATEWAY.getName());
        gateway.setMinimumNodeCount(2);
        InstanceGroupV4Response storagehg = new InstanceGroupV4Response();
        storagehg.setName(DatalakeInstanceGroupScalingDetails.STORAGE_SCALE_OUT.getName());
        storagehg.setMinimumNodeCount(0);
        storagehg.setNodeCount(3);
        stackV4Response.setInstanceGroups(List.of(gateway, storagehg));
        stackV4Response.setStatus(Status.AVAILABLE);
        when(stackV4Endpoint.getByCrn(any(), any(), any())).thenReturn(stackV4Response);
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setGroup(DatalakeInstanceGroupScalingDetails.STORAGE_SCALE_OUT.getName());
        scaleRequest.setDesiredCount(1);
        SdxCluster sdxCluster = getSdxCluster();
        assertThrows(BadRequestException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));

    }

    @Test
    void testDatalakeHorizontalScaleValidationPrimaryGatewayDown() {
        SdxCluster sdxCluster = getSdxCluster();
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();

        scaleRequest.setDesiredCount(1);
        scaleRequest.setGroup(DatalakeInstanceGroupScalingDetails.SOLR_SCALE_OUT.getName());

        StackV4Response stackV4Response = new StackV4Response();
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        ClouderaManagerV4Response cm = new ClouderaManagerV4Response();
        ClouderaManagerProductV4Response cdpResponse = new ClouderaManagerProductV4Response();
        cdpResponse.setName("CDH");
        cdpResponse.setVersion("7.2.17");
        cm.setProducts(Collections.singletonList(cdpResponse));
        clusterV4Response.setCm(cm);
        stackV4Response.setCluster(clusterV4Response);
        InstanceGroupV4Response core = new InstanceGroupV4Response();
        core.setName(DatalakeInstanceGroupScalingDetails.CORE.getName());
        core.setMinimumNodeCount(3);
        InstanceGroupV4Response auxiliary = new InstanceGroupV4Response();
        auxiliary.setName(DatalakeInstanceGroupScalingDetails.AUXILIARY.getName());
        auxiliary.setMinimumNodeCount(1);
        InstanceGroupV4Response gateway = new InstanceGroupV4Response();
        gateway.setName(DatalakeInstanceGroupScalingDetails.GATEWAY.getName());
        gateway.setMinimumNodeCount(2);
        InstanceMetaDataV4Response instanceMetaDataV4Response = new InstanceMetaDataV4Response();
        instanceMetaDataV4Response.setInstanceStatus(InstanceStatus.FAILED);
        instanceMetaDataV4Response.setInstanceType(InstanceMetadataType.GATEWAY_PRIMARY);
        gateway.setMetadata(Set.of(instanceMetaDataV4Response));
        InstanceGroupV4Response solrhg = new InstanceGroupV4Response();
        solrhg.setName(DatalakeInstanceGroupScalingDetails.SOLR_SCALE_OUT.getName());
        solrhg.setMinimumNodeCount(0);
        stackV4Response.setInstanceGroups(List.of(core, auxiliary, gateway, solrhg));
        stackV4Response.setStatus(Status.AVAILABLE);

        when(stackV4Endpoint.getByCrn(any(), any(), any())).thenReturn(stackV4Response);

        DetailedEnvironmentResponse detailedEnvironmentResponse = getEnvironmentDetailedResponse();
        when(environmentClientService.getByName(eq(sdxCluster.getEnvName()))).thenReturn(detailedEnvironmentResponse);
        assertThrows(BadRequestException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));
    }

    @Test
    void testDatalakeHorizontalScaleInvalidTargetGroup() {
        SdxCluster sdxCluster = getSdxCluster();
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setDesiredCount(1);
        scaleRequest.setGroup("noNameGroup");
        assertThrows(IllegalArgumentException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));
    }

    @Test
    void testDatalakeHorizontalScaleEnvironmentIsNotHealthy() {
        SdxCluster sdxCluster = getSdxCluster();
        DetailedEnvironmentResponse detailedEnvironmentResponse = getEnvironmentDetailedResponse();
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS);
        when(environmentClientService.getByName(eq(sdxCluster.getEnvName()))).thenReturn(detailedEnvironmentResponse);
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setGroup(DatalakeInstanceGroupScalingDetails.SOLR_SCALE_OUT.getName());
        scaleRequest.setDesiredCount(1);
        assertThrows(BadRequestException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));
    }

    private StackV4Response getStackV4Response() {
        StackV4Response stackV4Response = new StackV4Response();
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        ClouderaManagerV4Response cm = new ClouderaManagerV4Response();
        ClouderaManagerProductV4Response cdpResponse = new ClouderaManagerProductV4Response();
        cdpResponse.setName("CDH");
        cdpResponse.setVersion("7.2.17");
        cm.setProducts(Collections.singletonList(cdpResponse));
        clusterV4Response.setCm(cm);
        stackV4Response.setCluster(clusterV4Response);
        InstanceGroupV4Response core = new InstanceGroupV4Response();
        core.setName(DatalakeInstanceGroupScalingDetails.CORE.getName());
        core.setMinimumNodeCount(3);
        InstanceGroupV4Response auxiliary = new InstanceGroupV4Response();
        auxiliary.setName(DatalakeInstanceGroupScalingDetails.AUXILIARY.getName());
        auxiliary.setMinimumNodeCount(1);
        InstanceGroupV4Response gateway = new InstanceGroupV4Response();
        gateway.setName(DatalakeInstanceGroupScalingDetails.GATEWAY.getName());
        gateway.setMinimumNodeCount(2);
        InstanceGroupV4Response solrhg = new InstanceGroupV4Response();
        solrhg.setName(DatalakeInstanceGroupScalingDetails.SOLR_SCALE_OUT.getName());
        solrhg.setMinimumNodeCount(0);
        stackV4Response.setInstanceGroups(List.of(core, auxiliary, gateway, solrhg));
        stackV4Response.setStatus(Status.AVAILABLE);
        return stackV4Response;
    }

    @Test
    void testDatalakeHorizontalScaleValidationRAZ() {
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setRangerRazEnabled(false);
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setGroup(DatalakeInstanceGroupScalingDetails.RAZ_SCALE_OUT.getName());
        scaleRequest.setDesiredCount(1);
        DetailedEnvironmentResponse environmentDetailedResponse = getEnvironmentDetailedResponse();
        when(environmentClientService.getByName(anyString())).thenReturn(environmentDetailedResponse);
        StackV4Response stackV4Response = getStackV4Response();
        when(stackV4Endpoint.getByCrn(any(), any(), any())).thenReturn(stackV4Response);
        assertThrows(BadRequestException.class, () -> underTest.validateHorizontalScaleRequest(sdxCluster, scaleRequest));
    }

    private DetailedEnvironmentResponse getEnvironmentDetailedResponse() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName("clusterName");
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn("crn");
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        return detailedEnvironmentResponse;
    }

    private SdxCluster getSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setAccountId("accountId");
        sdxCluster.setId(SDX_ID);
        sdxCluster.setCrn(SDX_CRN);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setEnvName("envir");
        sdxCluster.setClusterName("sdx-cluster-name");
        sdxCluster.setClusterShape(ENTERPRISE);
        sdxCluster.setStackId(1L);
        return sdxCluster;
    }
}
