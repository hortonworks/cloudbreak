package com.sequenceiq.cloudbreak.service.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.UpdateNodeCountValidator;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.service.FlowService;

@ExtendWith(MockitoExtension.class)
public class ClusterCommonServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final long STACK_ID = 1L;

    private static final String HOST_GROUP_NAME_WORKER = "worker";

    private static final long CLUSTER_ID = 2L;

    private static final String STACK_CRN = String.format("crn:cdp:datahub:us-west-1:%s:cluster:cluster", ACCOUNT_ID);

    private static final int SCALING_ADJUSTMENT = 5;

    private static final String STACK_NAME = "stackName";

    @InjectMocks
    private ClusterCommonService underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private FlowService flowService;

    @Mock
    private StackCommonService stackCommonService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClusterService clusterService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private CmTemplateValidator cmTemplateValidator;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private UpdateNodeCountValidator updateNodeCountValidator;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private StackDto stackDto;

    @Mock
    private StackView stackView;

    @Mock
    private ClusterView clusterView;

    private Blueprint blueprint;

    private FlowIdentifier flowIdentifier;

    @BeforeEach
    public void setUp() {
        blueprint = new Blueprint();
        flowIdentifier = new FlowIdentifier(FlowType.FLOW, "1");
    }

    @Test
    public void testRotateAutoTlsCertificatesWithStoppedInstances() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("cluster");
        StackView stack = mock(StackView.class);
        when(stack.isAvailable()).thenReturn(true);
        when(stack.isDatalake()).thenReturn(true);
        when(instanceMetaDataService.anyInstanceStopped(any())).thenReturn(true);
        when(stackDtoService.getStackViewByNameOrCrn(nameOrCrn, ACCOUNT_ID)).thenReturn(stack);
        CertificatesRotationV4Request certificatesRotationV4Request = new CertificatesRotationV4Request();
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.rotateAutoTlsCertificates(nameOrCrn, ACCOUNT_ID, certificatesRotationV4Request));
        assertEquals("Please start all stopped instances in Datalake. Certificates rotation can only be made when all your nodes in running state.",
                badRequestException.getMessage());
    }

    @Test
    public void testRotateAutoTls() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("cluster");
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.isAvailable()).thenReturn(true);

        CertificatesRotationV4Request certificatesRotationV4Request = new CertificatesRotationV4Request();
        when(clusterOperationService.rotateAutoTlsCertificates(STACK_ID, null, certificatesRotationV4Request)).thenReturn(flowIdentifier);
        when(stackDtoService.getStackViewByNameOrCrn(nameOrCrn, ACCOUNT_ID)).thenReturn(stack);
        underTest.rotateAutoTlsCertificates(nameOrCrn, ACCOUNT_ID, certificatesRotationV4Request);
        verify(clusterOperationService, times(1)).rotateAutoTlsCertificates(STACK_ID, null, certificatesRotationV4Request);
    }

    @Test
    public void testRotateAutoTlsCertificatesWithNodeFailure() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("cluster");
        StackView stack = mock(StackView.class);
        when(stack.getName()).thenReturn("cluster");
        when(stack.getStatus()).thenReturn(Status.NODE_FAILURE);
        when(stackDtoService.getStackViewByNameOrCrn(nameOrCrn, ACCOUNT_ID)).thenReturn(stack);
        CertificatesRotationV4Request certificatesRotationV4Request = new CertificatesRotationV4Request();
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.rotateAutoTlsCertificates(nameOrCrn, ACCOUNT_ID, certificatesRotationV4Request));
        assertEquals("Stack 'cluster' is currently in 'NODE_FAILURE' state. Certificates rotation can only be made when the underlying stack is 'AVAILABLE'.",
                badRequestException.getMessage());
    }

    @Test
    public void testInventoryFileGeneration() {
        // GIVEN

        Stack stack = new Stack();
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        cluster.setClusterManagerIp("gatewayIP");
        stack.setCluster(cluster);

        when(instanceMetaDataService.getAllInstanceMetadataByStackId(anyLong())).thenReturn(generateInstanceMetadata());

        // WHEN
        String result = underTest.getHostNamesAsIniString(stack, "cloudbreak");
        // THEN

        assertEquals("""
                [cluster]
                name=cl1

                [server]
                gatewayIP

                [worker]
                worker-1
                pub-worker-2

                [master]
                m1

                [agent]
                m1
                worker-1
                pub-worker-2

                [all:vars]
                ansible_ssh_user=cloudbreak
                ansible_ssh_common_args='-o StrictHostKeyChecking=no'
                ansible_become=yes
                """, result);
    }

    @Test
    public void testInventoryFileGenerationWithoutAgents() {
        Stack stack = new Stack();
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        cluster.setClusterManagerIp(null);
        stack.setCluster(cluster);

        // WHEN
        when(instanceMetaDataService.getAllInstanceMetadataByStackId(anyLong())).thenReturn(Collections.emptySet());
        assertThrows(NotFoundException.class, () -> underTest.getHostNamesAsIniString(stack, "cloudbreak"));
    }

    private Set<InstanceMetaData> generateInstanceMetadata() {
        //LinkedHashSet required to keep the order, and have a reliable test run
        Set<InstanceMetaData> instanceMetaData = new LinkedHashSet<>();
        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        InstanceGroup worker = new InstanceGroup();
        worker.setGroupName(HOST_GROUP_NAME_WORKER);

        InstanceMetaData master1 = new InstanceMetaData();
        master1.setPublicIp("m1");
        master1.setInstanceGroup(master);
        master1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetaData.add(master1);

        master.setInstanceMetaData(Set.of(master1));

        InstanceMetaData worker1 = new InstanceMetaData();
        worker1.setPrivateIp("worker-1");
        worker1.setInstanceGroup(worker);
        worker1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetaData.add(worker1);

        InstanceMetaData worker2 = new InstanceMetaData();
        // If we have both then we need public ip
        worker2.setPublicIp("pub-worker-2");
        worker2.setPrivateIp("worker-2");
        worker2.setInstanceGroup(worker);
        worker2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);

        worker.setInstanceMetaData(new HashSet<>(Arrays.asList(worker1, worker2)));
        instanceMetaData.add(worker2);

        return instanceMetaData;
    }

    @Test
    public void testUpdateNodeCountWhenCheckCallEnvironmentCheck() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackDtoService.getByCrn("crn")).thenReturn(stack);
        doThrow(RuntimeException.class).when(environmentService).checkEnvironmentStatus(stackView, EnvironmentStatus.upscalable());

        UpdateClusterV4Request update = new UpdateClusterV4Request();
        update.setHostGroupAdjustment(new HostGroupAdjustmentV4Request());
        assertThrows(RuntimeException.class, () -> underTest.put("crn", update));
        verify(environmentService).checkEnvironmentStatus(stackView, EnvironmentStatus.upscalable());
    }

    @Test
    public void testSaltUpdate() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("cluster");
        StackView stack = mock(StackView.class);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackDtoService.getStackViewByNameOrCrn(nameOrCrn, ACCOUNT_ID)).thenReturn(stack);
        when(clusterOperationService.updateSalt(STACK_ID, true)).thenReturn(flowIdentifier);

        FlowIdentifier flowIdentifier = underTest.updateSalt(nameOrCrn, ACCOUNT_ID, true);

        verify(clusterOperationService, times(1)).updateSalt(STACK_ID, true);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("1", flowIdentifier.getPollableId());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"STOPPED", "STOP_IN_PROGRESS", "STOP_REQUESTED", "DELETE_COMPLETED",
            "DELETE_FAILED", "DELETE_IN_PROGRESS", "DELETED_ON_PROVIDER_SIDE", "PRE_DELETE_IN_PROGRESS"}, mode = EnumSource.Mode.INCLUDE)
    public void testSaltUpdateThrowsBadRequestWhenStackNotAvailable(Status status) {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("cluster");
        StackView stack = mock(StackView.class);
        when(stack.getStatus()).thenReturn(status);
        when(stack.getName()).thenReturn("stack-name");
        when(stackDtoService.getStackViewByNameOrCrn(nameOrCrn, ACCOUNT_ID)).thenReturn(stack);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->  underTest.updateSalt(nameOrCrn, ACCOUNT_ID, false));

        verifyNoInteractions(clusterOperationService);
        assertEquals(String.format("SaltStack update cannot be initiated as stack 'stack-name' is currently in '%s' state.", status), ex.getMessage());
    }

    @Test
    public void testPutDeleteVolumes() {
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(STACK_ID);
        stackDeleteVolumesRequest.setGroup("TEST");
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackDtoService.getByCrn(eq("CRN"))).thenReturn(stack);
        when(clusterOperationService.deleteVolumes(STACK_ID, stackDeleteVolumesRequest)).thenReturn(flowIdentifier);

        FlowIdentifier flowIdentifier = underTest.putDeleteVolumes("CRN", stackDeleteVolumesRequest);

        verify(clusterOperationService, times(1)).deleteVolumes(eq(STACK_ID), eq(stackDeleteVolumesRequest));
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("1", flowIdentifier.getPollableId());
    }

    @Test
    public void testPutDeleteVolumesBadRequest() {
        StackDto stack = mock(StackDto.class);
        when(stackDtoService.getByCrn(eq("CRN"))).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.putDeleteVolumes("CRN", null));

        assertEquals("Invalid update cluster request!", exception.getMessage());
    }

    @Test
    public void testPutAddVolumes() {
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackDtoService.getByCrn(eq("CRN"))).thenReturn(stack);
        when(clusterOperationService.addVolumes(STACK_ID, stackAddVolumesRequest)).thenReturn(flowIdentifier);

        FlowIdentifier flowIdentifier = underTest.putAddVolumes("CRN", stackAddVolumesRequest);

        verify(clusterOperationService, times(1)).addVolumes(eq(STACK_ID), eq(stackAddVolumesRequest));
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("1", flowIdentifier.getPollableId());
    }

    @Test
    public void testPutAddVolumesBadRequest() {
        StackDto stack = mock(StackDto.class);
        when(stackDtoService.getByCrn(eq("CRN"))).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.putAddVolumes("CRN", null));

        assertEquals("Invalid update cluster request!", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void putTestWhenHostGroupAdjustmentAndAvailable(boolean clouderaManagerTemplate) {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.isAvailable()).thenReturn(true);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(hostGroupService.hasHostGroupInCluster(CLUSTER_ID, HOST_GROUP_NAME_WORKER)).thenReturn(true);
        when(blueprintService.isClouderaManagerTemplate(blueprint)).thenReturn(clouderaManagerTemplate);
        Optional<ClouderaManagerProduct> cdhProduct = Optional.empty();
        Set<InstanceGroup> instanceGroups = Set.of();
        if (clouderaManagerTemplate) {
            when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
            when(clusterComponentConfigProvider.getNormalizedCdhProductWithNormalizedVersion(CLUSTER_ID)).thenReturn(cdhProduct);
            when(instanceGroupService.findNotTerminatedByStackId(STACK_ID)).thenReturn(instanceGroups);
        }

        UpdateClusterV4Request updateJson = new UpdateClusterV4Request();
        HostGroupAdjustmentV4Request hostGroupAdjustmentV4Request = new HostGroupAdjustmentV4Request();
        updateJson.setHostGroupAdjustment(hostGroupAdjustmentV4Request);
        hostGroupAdjustmentV4Request.setHostGroup(HOST_GROUP_NAME_WORKER);
        hostGroupAdjustmentV4Request.setScalingAdjustment(SCALING_ADJUSTMENT);

        when(clusterOperationService.updateHosts(STACK_ID, hostGroupAdjustmentV4Request)).thenReturn(flowIdentifier);

        FlowIdentifier flowIdentifierResult = underTest.put(stackDto, updateJson);

        assertThat(flowIdentifierResult).isEqualTo(flowIdentifier);

        verify(environmentService).checkEnvironmentStatus(stackView, EnvironmentStatus.upscalable());
        verify(updateNodeCountValidator).validateScalabilityOfInstanceGroup(stackDto, hostGroupAdjustmentV4Request);
        if (clouderaManagerTemplate) {
            verify(cmTemplateValidator).validateHostGroupScalingRequest(
                    ACCOUNT_ID,
                    blueprint,
                    cdhProduct,
                    HOST_GROUP_NAME_WORKER,
                    SCALING_ADJUSTMENT,
                    instanceGroups,
                    false);
        } else {
            verifyNoInteractions(cmTemplateValidator);
        }
    }

    @Test
    void putTestWhenHostGroupAdjustmentAndNodeFailure() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.isAvailable()).thenReturn(false);
        when(stackView.hasNodeFailure()).thenReturn(true);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(hostGroupService.hasHostGroupInCluster(CLUSTER_ID, HOST_GROUP_NAME_WORKER)).thenReturn(true);
        when(blueprintService.isClouderaManagerTemplate(blueprint)).thenReturn(true);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        Optional<ClouderaManagerProduct> cdhProduct = Optional.empty();
        when(clusterComponentConfigProvider.getNormalizedCdhProductWithNormalizedVersion(CLUSTER_ID)).thenReturn(cdhProduct);
        Set<InstanceGroup> instanceGroups = Set.of();
        when(instanceGroupService.findNotTerminatedByStackId(STACK_ID)).thenReturn(instanceGroups);

        UpdateClusterV4Request updateJson = new UpdateClusterV4Request();
        HostGroupAdjustmentV4Request hostGroupAdjustmentV4Request = new HostGroupAdjustmentV4Request();
        updateJson.setHostGroupAdjustment(hostGroupAdjustmentV4Request);
        hostGroupAdjustmentV4Request.setHostGroup(HOST_GROUP_NAME_WORKER);
        hostGroupAdjustmentV4Request.setScalingAdjustment(SCALING_ADJUSTMENT);

        when(clusterOperationService.updateHosts(STACK_ID, hostGroupAdjustmentV4Request)).thenReturn(flowIdentifier);

        FlowIdentifier flowIdentifierResult = underTest.put(stackDto, updateJson);

        assertThat(flowIdentifierResult).isEqualTo(flowIdentifier);

        verify(environmentService).checkEnvironmentStatus(stackView, EnvironmentStatus.upscalable());
        verify(updateNodeCountValidator).validateScalabilityOfInstanceGroup(stackDto, hostGroupAdjustmentV4Request);
        verify(cmTemplateValidator).validateHostGroupScalingRequest(
                ACCOUNT_ID,
                blueprint,
                cdhProduct,
                HOST_GROUP_NAME_WORKER,
                SCALING_ADJUSTMENT,
                instanceGroups,
                false);
    }

    @Test
    void putTestWhenHostGroupAdjustmentAndBadStackStatus() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.isAvailable()).thenReturn(false);
        when(stackView.hasNodeFailure()).thenReturn(false);
        when(stackDto.getStatus()).thenReturn(Status.CREATE_FAILED);

        UpdateClusterV4Request updateJson = new UpdateClusterV4Request();
        HostGroupAdjustmentV4Request hostGroupAdjustmentV4Request = new HostGroupAdjustmentV4Request();
        updateJson.setHostGroupAdjustment(hostGroupAdjustmentV4Request);
        hostGroupAdjustmentV4Request.setHostGroup(HOST_GROUP_NAME_WORKER);
        hostGroupAdjustmentV4Request.setScalingAdjustment(SCALING_ADJUSTMENT);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.put(stackDto, updateJson));

        assertThat(badRequestException.getMessage()).isEqualTo(String.format(
                "Stack '%s' is currently in '%s' status. Cluster scale can only be made " +
                        "if the underlying stack status is 'AVAILABLE' or 'NODE_FAILURE'.", STACK_ID, Status.CREATE_FAILED));

        verify(environmentService).checkEnvironmentStatus(stackView, EnvironmentStatus.upscalable());
        verifyNoInteractions(updateNodeCountValidator);
        verifyNoInteractions(cmTemplateValidator);
        verifyNoInteractions(clusterOperationService);
    }

    @Test
    void putTestWhenHostGroupAdjustmentAndHostGroupNotFound() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.isAvailable()).thenReturn(true);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(hostGroupService.hasHostGroupInCluster(CLUSTER_ID, HOST_GROUP_NAME_WORKER)).thenReturn(false);
        when(stackDto.getName()).thenReturn(STACK_NAME);

        UpdateClusterV4Request updateJson = new UpdateClusterV4Request();
        HostGroupAdjustmentV4Request hostGroupAdjustmentV4Request = new HostGroupAdjustmentV4Request();
        updateJson.setHostGroupAdjustment(hostGroupAdjustmentV4Request);
        hostGroupAdjustmentV4Request.setHostGroup(HOST_GROUP_NAME_WORKER);
        hostGroupAdjustmentV4Request.setScalingAdjustment(SCALING_ADJUSTMENT);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.put(stackDto, updateJson));

        assertThat(badRequestException.getMessage()).isEqualTo(String.format(
                "Host group '%s' not found or not member of the cluster '%s'", HOST_GROUP_NAME_WORKER, STACK_NAME));

        verify(environmentService).checkEnvironmentStatus(stackView, EnvironmentStatus.upscalable());
        verifyNoInteractions(updateNodeCountValidator);
        verifyNoInteractions(cmTemplateValidator);
        verifyNoInteractions(clusterOperationService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void putTestWhenCrnAndHostGroupAdjustmentAndAvailable(boolean clouderaManagerTemplate) {
        when(stackDtoService.getByCrn(STACK_CRN)).thenReturn(stackDto);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.isAvailable()).thenReturn(true);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(hostGroupService.hasHostGroupInCluster(CLUSTER_ID, HOST_GROUP_NAME_WORKER)).thenReturn(true);
        when(blueprintService.isClouderaManagerTemplate(blueprint)).thenReturn(clouderaManagerTemplate);
        Optional<ClouderaManagerProduct> cdhProduct = Optional.empty();
        Set<InstanceGroup> instanceGroups = Set.of();
        if (clouderaManagerTemplate) {
            when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
            when(clusterComponentConfigProvider.getNormalizedCdhProductWithNormalizedVersion(CLUSTER_ID)).thenReturn(cdhProduct);
            when(instanceGroupService.findNotTerminatedByStackId(STACK_ID)).thenReturn(instanceGroups);
        }

        UpdateClusterV4Request updateJson = new UpdateClusterV4Request();
        HostGroupAdjustmentV4Request hostGroupAdjustmentV4Request = new HostGroupAdjustmentV4Request();
        updateJson.setHostGroupAdjustment(hostGroupAdjustmentV4Request);
        hostGroupAdjustmentV4Request.setHostGroup(HOST_GROUP_NAME_WORKER);
        hostGroupAdjustmentV4Request.setScalingAdjustment(SCALING_ADJUSTMENT);

        when(clusterOperationService.updateHosts(STACK_ID, hostGroupAdjustmentV4Request)).thenReturn(flowIdentifier);

        FlowIdentifier flowIdentifierResult = underTest.put(STACK_CRN, updateJson);

        assertThat(flowIdentifierResult).isEqualTo(flowIdentifier);

        verify(environmentService).checkEnvironmentStatus(stackView, EnvironmentStatus.upscalable());
        verify(updateNodeCountValidator).validateScalabilityOfInstanceGroup(stackDto, hostGroupAdjustmentV4Request);
        if (clouderaManagerTemplate) {
            verify(cmTemplateValidator).validateHostGroupScalingRequest(
                    ACCOUNT_ID,
                    blueprint,
                    cdhProduct,
                    HOST_GROUP_NAME_WORKER,
                    SCALING_ADJUSTMENT,
                    instanceGroups,
                    false);
        } else {
            verifyNoInteractions(cmTemplateValidator);
        }
    }
}
