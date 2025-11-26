package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_AUTORECOVERY_REQUESTED_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_AUTORECOVERY_REQUESTED_HOST_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FAILED_NODES_REPORTED_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FAILED_NODES_REPORTED_HOST_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RECOVERED_NODES_REPORTED_CLUSTER_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.telemetry.DynamicEntitlementRefreshService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class ClusterOperationServiceTest {

    private static final long STACK_ID = 1;

    private static final String STACK_CRN = "STACK_CRN";

    private static final long CLUSTER_ID = 1;

    private static final FlowIdentifier FLOW_IDENTIFIER = new FlowIdentifier(FlowType.FLOW, "flowId");

    @Mock
    private StackService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterModificationService clusterModificationService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private UpdateHostsValidator updateHostsValidator;

    @Mock
    private StackUpdater stackUpdater;

    @Spy
    private ResourceAttributeUtil resourceAttributeUtil;

    @InjectMocks
    private ClusterOperationService underTest;

    private Cluster cluster;

    private Stack stack;

    @BeforeEach
    void setUp() throws TransactionExecutionException {
        cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setRdsConfigs(Set.of());
        stack = spy(new Stack());
        stack.setId(STACK_ID);
        stack.setResourceCrn(STACK_CRN);
        stack.setCluster(cluster);
        stack.setPlatformVariant("AWS");
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
        cluster.setStack(stack);

        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
    }

    @Test
    void shouldThrowExceptionWhenNewHealthyAndFailedNodeAreTheSame() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            underTest.reportHealthChange(STACK_CRN, Map.of("host", Optional.empty()), Set.of("host"));
        });

        assertEquals("Failed nodes [host] and healthy nodes [host] should not have common items.", exception.getMessage());
    }

    @Test
    void shouldTriggerRecoveryInAutoRecoveryHostsAndUpdateHostMetaData() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{ \"Blueprints\": { \"blueprint_name\": \"MyBlueprint\" } }");
        cluster.setBlueprint(blueprint);

        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);

        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn("auto recovery").thenReturn("failed node");

        InstanceMetaData host1 = getHost("host1", "master", InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        InstanceMetaData host2 = getHost("host2", "group2", InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);

        HostGroup hostGroup = getHostGroup(host1, RecoveryMode.AUTO);
        when(instanceMetaDataService.getAllInstanceMetadataByStackId(eq(stack.getId()))).thenReturn(Set.of(host1, host2));
        when(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(eq(stack.getId()))).thenReturn(Set.of(host1, host2));
        when(hostGroupService.findHostGroupsInCluster(stack.getCluster().getId())).thenReturn(Set.of(hostGroup, getHostGroup(host2, RecoveryMode.MANUAL)));

        underTest.reportHealthChange(STACK_CRN, Map.of("host1", Optional.empty(), "host2", Optional.empty()), Set.of());

        Map<String, List<String>> autoRecoveredNodes = Map.of("master", List.of("host1"));
        verify(flowManager).triggerClusterRepairFlow(STACK_ID, autoRecoveredNodes, false);

        verify(cloudbreakMessagesService, times(1)).getMessage(eq(CLUSTER_AUTORECOVERY_REQUESTED_HOST_EVENT.getMessage()),
                any());
        verify(cloudbreakMessagesService, times(1)).getMessage(eq(CLUSTER_AUTORECOVERY_REQUESTED_CLUSTER_EVENT.getMessage()),
                any());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(STACK_ID, "RECOVERY",
                CLUSTER_AUTORECOVERY_REQUESTED_CLUSTER_EVENT, List.of("host1"));

        verify(cloudbreakMessagesService, times(1)).getMessage(eq(CLUSTER_FAILED_NODES_REPORTED_HOST_EVENT.getMessage()),
                any());
        verify(cloudbreakMessagesService, times(1)).getMessage(eq(CLUSTER_FAILED_NODES_REPORTED_CLUSTER_EVENT.getMessage()),
                any());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(STACK_ID, "RECOVERY",
                CLUSTER_FAILED_NODES_REPORTED_CLUSTER_EVENT, List.of("host2"));
        verify(updateHostsValidator, times(1)).validateComponentsCategory(stack, hostGroup.getName());
    }

    @Test
    void shouldNotTriggerSync() {
        String hostFQDN = "host2Name.stopped";
        InstanceMetaData instanceMd = getHost(hostFQDN, "master", InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        when(instanceMetaDataService.getAllInstanceMetadataByStackId(eq(stack.getId()))).thenReturn(Set.of(instanceMd));
        when(hostGroupService.findHostGroupsInCluster(stack.getCluster().getId())).thenReturn(Set.of(getHostGroup(instanceMd, RecoveryMode.MANUAL)));

        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);
        lenient().when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn("failed node");

        underTest.reportHealthChange(STACK_CRN, Map.of(hostFQDN, Optional.empty()), Set.of());

        verify(flowManager, times(0)).triggerStackSync(eq(stack.getId()));
    }

    @Test
    void shouldNotUpdateHostMetaDataWhenRecoveryTriggerFails() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{ \"Blueprints\": { \"blueprint_name\": \"MyBlueprint\" } }");
        cluster.setBlueprint(blueprint);

        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);
        InstanceMetaData host1 = getHost("host1", "master", InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        when(instanceMetaDataService.getAllInstanceMetadataByStackId(eq(stack.getId()))).thenReturn(Set.of(host1));
        when(hostGroupService.findHostGroupsInCluster(stack.getCluster().getId()))
                .thenReturn(Set.of(getHostGroup(host1, RecoveryMode.AUTO)));

        doThrow(new FlowsAlreadyRunningException("Flow in action")).when(flowManager).triggerClusterRepairFlow(anyLong(), anyMap(), anyBoolean());

        assertThrows(FlowsAlreadyRunningException.class, () -> {
            underTest.reportHealthChange(STACK_CRN, Map.of("host1", Optional.empty()), Set.of());
        });

        verifyNoMoreInteractions(cloudbreakMessagesService);
        verifyNoMoreInteractions(cloudbreakEventService);
    }

    @Test
    void shouldRegisterNewHealthyHosts() {
        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);

        InstanceMetaData host1 = getHost("host1", "master", InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.GATEWAY);

        when(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(eq(stack.getId()))).thenReturn(new HashSet<>(Arrays.asList(host1)));

        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn("recovery detected");

        underTest.reportHealthChange(STACK_CRN, Map.of(), Set.of("host1"));

        verify(cloudbreakMessagesService, times(1)).getMessage(eq(CLUSTER_RECOVERED_NODES_REPORTED_CLUSTER_EVENT.getMessage()), any());
        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, "AVAILABLE", CLUSTER_RECOVERED_NODES_REPORTED_CLUSTER_EVENT, List.of("host1"));
        assertEquals(InstanceStatus.SERVICES_HEALTHY, host1.getInstanceStatus());
        assertEquals(StringUtils.EMPTY, host1.getStatusReason());
    }

    @Test
    void updateHostsTestAndCheckDownscaleAndUpscaleStatusChange() {
        HostGroupAdjustmentV4Request downscaleHostGroupAdjustment = new HostGroupAdjustmentV4Request();
        downscaleHostGroupAdjustment.setHostGroup("worker");
        downscaleHostGroupAdjustment.setScalingAdjustment(-5);
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(updateHostsValidator.validateRequest(stack, downscaleHostGroupAdjustment)).thenReturn(true);
        underTest.updateHosts(STACK_ID, downscaleHostGroupAdjustment);
        verify(flowManager, times(1)).triggerClusterDownscale(STACK_ID, downscaleHostGroupAdjustment);

        HostGroupAdjustmentV4Request upscaleHostGroupAdjustment = new HostGroupAdjustmentV4Request();
        upscaleHostGroupAdjustment.setHostGroup("worker");
        upscaleHostGroupAdjustment.setScalingAdjustment(5);
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(updateHostsValidator.validateRequest(stack, upscaleHostGroupAdjustment)).thenReturn(false);
        underTest.updateHosts(STACK_ID, upscaleHostGroupAdjustment);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, DetailedStackStatus.UPSCALE_REQUESTED,
                "Requested node count for upscaling: " + 5 + ", instance group: worker");
        verify(flowManager, times(1)).triggerClusterUpscale(STACK_ID, upscaleHostGroupAdjustment);
    }

    @Test
    void shouldUpdateHostStatusAndNotFailWhenMissingDiscoveryFqdn() {
        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);

        InstanceMetaData instanceWithoutFqdn = getHost(null, "master", InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.GATEWAY);

        when(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(eq(stack.getId()))).thenReturn(new HashSet<>(Arrays.asList(instanceWithoutFqdn)));

        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn("recovery detected");

        underTest.reportHealthChange(STACK_CRN, Map.of(), Collections.singleton(null));

        verify(cloudbreakMessagesService, times(1)).getMessage(eq(CLUSTER_RECOVERED_NODES_REPORTED_CLUSTER_EVENT.getMessage()), any());
        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, "AVAILABLE", CLUSTER_RECOVERED_NODES_REPORTED_CLUSTER_EVENT, List.of("null"));
        assertEquals(InstanceStatus.SERVICES_HEALTHY, instanceWithoutFqdn.getInstanceStatus());
    }

    @Test
    void shouldTriggerDeleteVolumes() {
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");

        underTest.deleteVolumes(STACK_ID, stackDeleteVolumesRequest);

        ArgumentCaptor<StackDeleteVolumesRequest> captor = ArgumentCaptor.forClass(StackDeleteVolumesRequest.class);
        verify(flowManager).triggerDeleteVolumes(eq(STACK_ID), captor.capture());
        StackDeleteVolumesRequest request = captor.getValue();
        assertEquals("COMPUTE", request.getGroup());
        assertEquals(STACK_ID, request.getStackId());
    }

    @Test
    void shouldTriggerAddVolumes() {
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);

        underTest.addVolumes(STACK_ID, stackAddVolumesRequest);

        ArgumentCaptor<StackAddVolumesRequest> captor = ArgumentCaptor.forClass(StackAddVolumesRequest.class);
        verify(flowManager).triggerAddVolumes(eq(STACK_ID), captor.capture());
        StackAddVolumesRequest request = captor.getValue();
        assertEquals("COMPUTE", request.getInstanceGroup());
    }

    @Test
    void testRefreshEntitlementParams() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        when(dynamicEntitlementRefreshService.isClusterManagerServerReachable(stackDto)).thenReturn(true);
        Map<String, Boolean> changedEntitlements = Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE);
        when(dynamicEntitlementRefreshService.getChangedWatchedEntitlementsAndStoreNewFromUms(stackDto)).thenReturn(changedEntitlements);
        when(dynamicEntitlementRefreshService.saltRefreshNeeded(any())).thenReturn(true);

        underTest.refreshEntitlementParams(stackDto);

        assertTrue(changedEntitlements.get(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        verify(flowManager).triggerRefreshEntitlementParams(eq(STACK_ID), eq(STACK_CRN), eq(changedEntitlements), eq(true));
    }

    @Test
    void testRefreshEntitlementParamsNotChanged() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getName()).thenReturn(STACK_CRN);
        when(dynamicEntitlementRefreshService.isClusterManagerServerReachable(stackDto)).thenReturn(true);
        Map<String, Boolean> changedEntitlements = Map.of();
        when(dynamicEntitlementRefreshService.getChangedWatchedEntitlementsAndStoreNewFromUms(stackDto)).thenReturn(changedEntitlements);

        FlowIdentifier flowIdentifier = underTest.refreshEntitlementParams(stackDto);

        assertEquals(FlowIdentifier.notTriggered(), flowIdentifier);
        verify(flowManager, never()).triggerRefreshEntitlementParams(any(), any(), any(), any());
    }

    @Test
    void testRotateRdsCertificate() {
        when(flowManager.triggerRotateRdsCertificate(STACK_ID)).thenReturn(FLOW_IDENTIFIER);
        FlowIdentifier result = underTest.rotateRdsCertificate(stack);
        verify(flowManager).triggerRotateRdsCertificate(STACK_ID);
        assertThat(result).isEqualTo(FLOW_IDENTIFIER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AWS", "AWS_NATIVE", "AWS_NATIVE_GOV"})
    void testDeleteShouldMarkDiskResourcesDeleteOnTerminationOnAws(String platformVariant) throws TransactionExecutionException {
        long stackId = 1L;
        List<ResourceType> expectedVolumeResourceTypes = List.of(ResourceType.AWS_VOLUMESET, ResourceType.AWS_ENCRYPTED_VOLUME);
        Credential credential = TestUtil.awsCredential();
        Stack stack = TestUtil.stack(Status.AVAILABLE, credential);
        stack.setPlatformVariant(platformVariant);
        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        when(transactionService.required(any(Supplier.class))).then(invocation -> {
            Supplier supplier = invocation.getArgument(0);
            return supplier.get();
        });
        List<Resource> resources = List.of(
                getVolumeResource(ResourceType.AWS_VOLUMESET, "volumeSet", stack, "availabilityZone"),
                getVolumeResource(ResourceType.AWS_ENCRYPTED_VOLUME, "encryptedVolume", stack, "availabilityZone"),
                new Resource(ResourceType.AWS_SECURITY_GROUP, "secGroup", stack, "availabilityZone")
        );
        when(resourceService.getAllByStackId(stackId)).thenReturn(resources);

        underTest.delete(stackId, Boolean.FALSE);

        ArgumentCaptor<Iterable<Resource>> savedResourcesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(resourceService).saveAll(savedResourcesCaptor.capture());
        Iterable<Resource> savedResources = savedResourcesCaptor.getValue();
        Assertions.assertThat(savedResources)
                .hasSize(expectedVolumeResourceTypes.size())
                .allMatch(resource -> expectedVolumeResourceTypes.contains(resource.getResourceType())
                        && Boolean.TRUE.equals(resource.getAttributes().getMap().get("deleteOnTermination")));
    }

    @Test
    void testDeleteShouldMarkDiskResourcesDeleteOnTerminationOnAzure() throws TransactionExecutionException {
        List<ResourceType> expectedVolumeResourceTypes = List.of(ResourceType.AZURE_VOLUMESET);
        long stackId = 1L;
        Credential credential = TestUtil.azureCredential();
        Stack stack = TestUtil.stack(Status.AVAILABLE, credential);
        stack.setPlatformVariant("AZURE");
        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        when(transactionService.required(any(Supplier.class))).then(invocation -> {
            Supplier supplier = invocation.getArgument(0);
            return supplier.get();
        });
        List<Resource> resources = List.of(
                getVolumeResource(ResourceType.AZURE_VOLUMESET, "volumeSet", stack, "availabilityZone"),
                new Resource(ResourceType.AZURE_INSTANCE, "secGroup", stack, "availabilityZone")
        );
        when(resourceService.getAllByStackId(stackId)).thenReturn(resources);

        underTest.delete(stackId, Boolean.FALSE);

        ArgumentCaptor<Iterable<Resource>> savedResourcesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(resourceService).saveAll(savedResourcesCaptor.capture());
        Iterable<Resource> savedResources = savedResourcesCaptor.getValue();
        Assertions.assertThat(savedResources)
                .hasSize(expectedVolumeResourceTypes.size())
                .allMatch(resource -> expectedVolumeResourceTypes.contains(resource.getResourceType())
                        && Boolean.TRUE.equals(resource.getAttributes().getMap().get("deleteOnTermination")));
    }

    @Test
    void testDeleteShouldMarkDiskResourcesDeleteOnTerminationOnGcp() throws TransactionExecutionException {
        long stackId = 1L;
        List<ResourceType> expectedVolumeResourceTypes = List.of(ResourceType.GCP_ATTACHED_DISKSET);
        Credential credential = TestUtil.gcpCredential();
        Stack stack = TestUtil.stack(Status.AVAILABLE, credential);
        stack.setPlatformVariant("GCP");
        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        when(transactionService.required(any(Supplier.class))).then(invocation -> {
            Supplier supplier = invocation.getArgument(0);
            return supplier.get();
        });
        List<Resource> resources = List.of(
                getVolumeResource(ResourceType.GCP_ATTACHED_DISKSET, "volumeSet", stack, "availabilityZone"),
                new Resource(ResourceType.GCP_DISK, "secGroup", stack, "availabilityZone"),
                new Resource(ResourceType.GCP_INSTANCE, "secGroup", stack, "availabilityZone")
        );
        when(resourceService.getAllByStackId(stackId)).thenReturn(resources);

        underTest.delete(stackId, Boolean.FALSE);

        ArgumentCaptor<Iterable<Resource>> savedResourcesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(resourceService).saveAll(savedResourcesCaptor.capture());
        Iterable<Resource> savedResources = savedResourcesCaptor.getValue();
        Assertions.assertThat(savedResources)
                .hasSize(expectedVolumeResourceTypes.size())
                .allMatch(resource -> expectedVolumeResourceTypes.contains(resource.getResourceType())
                        && Boolean.TRUE.equals(resource.getAttributes().getMap().get("deleteOnTermination")));
    }

    private Resource getVolumeResource(ResourceType volumeResourceType, String volumeSet, Stack stack, String availabilityZone) {
        Resource resource = new Resource(volumeResourceType, volumeSet, stack, availabilityZone);
        resource.setAttributes(new Json(new VolumeSetAttributes(availabilityZone, Boolean.FALSE, "", List.of(), 0, "")));
        resource.setInstanceId("instanceId");
        return resource;
    }

    private InstanceMetaData getHost(String hostName, String groupName, InstanceStatus instanceStatus, InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(instanceGroupType);
        instanceGroup.setGroupName(groupName);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(hostName);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));

        return instanceMetaData;
    }

    private HostGroup getHostGroup(InstanceMetaData instanceMetaData, RecoveryMode recoveryMode) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(instanceMetaData.getInstanceGroupName());
        hostGroup.setRecoveryMode(recoveryMode);
        return hostGroup;
    }

}
