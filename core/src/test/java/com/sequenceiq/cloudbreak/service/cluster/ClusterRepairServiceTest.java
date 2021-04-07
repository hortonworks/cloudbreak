package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANUALRECOVERY_COULD_NOT_START;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANUALRECOVERY_NO_NODES_TO_RECOVER;
import static com.sequenceiq.redbeams.api.model.common.Status.AVAILABLE;
import static com.sequenceiq.redbeams.api.model.common.Status.STOPPED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
public class ClusterRepairServiceTest {
    private static final long STACK_ID = 1;

    private static final String STACK_CRN = "STACK_CRN";

    private static final long CLUSTER_ID = 1;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackService stackService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private ResourceService resourceService;

    @Spy
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterDBValidationService clusterDBValidationService;

    @Mock
    private RedbeamsClientService redbeamsClientService;

    @InjectMocks
    private ClusterRepairService underTest;

    private Stack stack;

    private Cluster cluster;

    @BeforeEach
    public void setUp() throws TransactionExecutionException {
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
    public void testRepairByHostGroups() {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackUpdater.updateStackStatus(1L, DetailedStackStatus.REPAIR_IN_PROGRESS)).thenReturn(stack);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getInstanceMetaDataAsList()).thenReturn(List.of(host1));

        underTest.repairHostGroups(1L, Set.of("hostGroup1"), false, false);

        verify(flowManager).triggerClusterRepairFlow(eq(1L), eq(Map.of("hostGroup1", List.of("host1"))), eq(false), eq(false));
    }

    @Test
    public void testCanRepairCoreTypeNode() {
        cluster.setDatabaseServerCrn("dbCrn");
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_RUNNING, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setStatus(AVAILABLE);
        when(redbeamsClientService.getByCrn(eq("dbCrn"))).thenReturn(databaseServerV4Response);

        Result result = underTest.repairWithDryRun(stack.getId());

        assertTrue(result.isSuccess());
        verifyNoInteractions(stackUpdater, flowManager, resourceService);
    }

    @Test
    public void testCanRepairPrewarmedGatewayWithRepairPossibleBasedOnDBSetup() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(mock(Image.class));
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.isPrewarmed()).thenReturn(true);
        when(imageCatalogService.getImage(any(), any(), any())).thenReturn(StatedImage.statedImage(image, "catalogUrl", "catalogName"));
        when(clusterDBValidationService.isGatewayRepairEnabled(cluster)).thenReturn(true);

        Result result = underTest.repairWithDryRun(stack.getId());

        assertTrue(result.isSuccess());
        verifyNoInteractions(stackUpdater, flowManager, resourceService);
    }

    @Test
    public void testCannotRepairBaseImageGateway() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(mock(Image.class));
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.isPrewarmed()).thenReturn(false);
        when(imageCatalogService.getImage(any(), any(), any())).thenReturn(StatedImage.statedImage(image, "catalogUrl", "catalogName"));
        when(clusterDBValidationService.isGatewayRepairEnabled(cluster)).thenReturn(true);

        Result result = underTest.repairWithDryRun(stack.getId());

        assertFalse(result.isSuccess());
        verifyNoInteractions(stackUpdater, flowManager, resourceService);
    }

    @Test
    public void testCannotRepairGatewayWithoutExternalDatabase() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(mock(Image.class));
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.isPrewarmed()).thenReturn(true);
        when(imageCatalogService.getImage(any(), any(), any())).thenReturn(StatedImage.statedImage(image, "catalogUrl", "catalogName"));
        when(clusterDBValidationService.isGatewayRepairEnabled(cluster)).thenReturn(false);

        Result result = underTest.repairWithDryRun(stack.getId());

        assertFalse(result.isSuccess());
        verifyNoInteractions(stackUpdater, flowManager, resourceService);
    }

    @Test
    public void testRepairByNodeIds() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        hostGroup1.setInstanceGroup(instanceGroup);

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));

        InstanceMetaData instance1md = new InstanceMetaData();
        instance1md.setInstanceId("instanceId1");
        instance1md.setDiscoveryFQDN("host1Name.healthy");
        instance1md.setInstanceGroup(instanceGroup);
        instanceGroup.setInstanceMetaData(Collections.singleton(instance1md));

        Resource volumeSet = new Resource();
        VolumeSetAttributes attributes = new VolumeSetAttributes("eu-west-1", Boolean.TRUE, "", List.of(), 100, "standard");
        attributes.setDeleteOnTermination(null);
        volumeSet.setAttributes(new Json(attributes));
        volumeSet.setInstanceId("instanceId1");
        volumeSet.setResourceType(ResourceType.AWS_VOLUMESET);
        FlowLog flowLog = new FlowLog();
        flowLog.setStateStatus(StateStatus.SUCCESSFUL);
        when(stackUpdater.updateStackStatus(1L, DetailedStackStatus.REPAIR_IN_PROGRESS)).thenReturn(stack);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getInstanceMetaDataAsList()).thenReturn(List.of(instance1md));
        when(resourceService.findByStackIdAndType(stack.getId(), volumeSet.getResourceType())).thenReturn(List.of(volumeSet));

        underTest.repairNodes(1L, Set.of("instanceId1"), false, false, false);
        verify(resourceService).findByStackIdAndType(stack.getId(), volumeSet.getResourceType());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Resource>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceService).saveAll(saveCaptor.capture());
        assertFalse(resourceAttributeUtil.getTypedAttributes(saveCaptor.getValue().get(0), VolumeSetAttributes.class).get().getDeleteOnTermination());
        verify(flowManager).triggerClusterRepairFlow(eq(1L), eq(Map.of("hostGroup1", List.of("host1Name.healthy"))), eq(false), eq(false));
    }

    @Test
    public void shouldNotUpdateStackStateWhenThereAreNoNodesToRepair() {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getInstanceMetaDataAsList()).thenReturn(List.of(host1));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            underTest.repairHostGroups(1L, Set.of("hostGroup1"), false, false);
        });

        assertEquals("Could not trigger cluster repair for stack 1 because node list is incorrect", exception.getMessage());
        verifyEventArguments(CLUSTER_MANUALRECOVERY_NO_NODES_TO_RECOVER, "hostGroup1");
        verifyNoInteractions(stackUpdater);
    }

    @Test
    public void repairShouldFailIfNotAvailableDatabaseExistsForCluster() {
        cluster.setDatabaseServerCrn("dbCrn");

        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setStatus(STOPPED);
        when(redbeamsClientService.getByCrn(eq("dbCrn"))).thenReturn(databaseServerV4Response);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            underTest.repairHostGroups(1L, Set.of("hostGroup1"), false, false);
        });

        assertEquals("Database dbCrn is not in AVAILABLE status, could not start repair.", exception.getMessage());
        verifyEventArguments(CLUSTER_MANUALRECOVERY_COULD_NOT_START, "Database dbCrn is not in AVAILABLE status, could not start repair.");
        verifyNoInteractions(stackUpdater);
    }

    @Test
    public void shouldNotAllowRepairWhenNodeIsStoppedInNotSelectedInstanceGroup() {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        HostGroup hostGroup2 = new HostGroup();
        hostGroup2.setName("hostGroup2");
        hostGroup2.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host2 = getHost("host2", hostGroup2.getName(), InstanceStatus.STOPPED, InstanceGroupType.CORE);
        hostGroup2.setInstanceGroup(host2.getInstanceGroup());

        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getInstanceMetaDataAsList()).thenReturn(List.of(host1, host2));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            underTest.repairHostGroups(1L, Set.of("hostGroup1"), false, false);
        });

        String expectedErrorMessage =
                "Action cannot be performed because there are stopped nodes in the cluster. Please select them for repair or start the stopped nodes.";
        assertEquals(expectedErrorMessage,
                exception.getMessage());
        verifyEventArguments(CLUSTER_MANUALRECOVERY_COULD_NOT_START,
                expectedErrorMessage);
        verifyNoInteractions(stackUpdater);
    }

    private void verifyEventArguments(ResourceEvent resourceEvent, String messageAssert) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> argument = ArgumentCaptor.forClass(Collection.class);
        verify(eventService).fireCloudbreakEvent(any(), eq("RECOVERY_FAILED"), eq(resourceEvent), argument.capture());
        assertEquals(messageAssert, argument.getValue().iterator().next());
    }

    private InstanceMetaData getHost(String hostName, String groupName, InstanceStatus instanceStatus, InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(instanceGroupType);
        instanceGroup.setGroupName(groupName);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(hostName);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceMetaData.setInstanceId(hostName);
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));

        return instanceMetaData;
    }

}