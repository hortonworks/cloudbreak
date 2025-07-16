package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANUALRECOVERY_COULD_NOT_START;
import static com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName.hostGroupName;
import static com.sequenceiq.redbeams.api.model.common.Status.AVAILABLE;
import static com.sequenceiq.redbeams.api.model.common.Status.STOPPED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.salt.SaltVersionUpgradeService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.AwsDiskType;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
class ClusterRepairServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final long STACK_ID = 1;

    private static final String STACK_CRN = "STACK_CRN";

    private static final String STACK_NAME = "STACK_NAME";

    private static final String ENV_CRN = "ENV_CRN";

    private static final long CLUSTER_ID = 1;

    private static final Long WORKSPACE_ID = 2L;

    private static final String PLATFORM_VARIANT = "platformVariant";

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackDtoService stackDtoService;

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

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ClusterRepairService underTest;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private FreeipaService freeipaService;

    @Mock
    private StackStopRestrictionService stackStopRestrictionService;

    @Mock
    private StackUpgradeService stackUpgradeService;

    @Mock
    private SaltVersionUpgradeService saltVersionUpgradeService;

    private Stack stack;

    private StackDto stackDto;

    private Cluster cluster;

    @BeforeEach
    void setUp() throws TransactionExecutionException {
        cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setRdsConfigs(Set.of());
        stackDto = spy(new StackDto());
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setResourceCrn(STACK_CRN);
        stack.setCluster(cluster);
        stack.setPlatformVariant("AWS");
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setTunnel(Tunnel.CLUSTER_PROXY);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
        stack.setInstanceGroups(Set.of());
        stack.setName(STACK_NAME);
        cluster.setStack(stack);

        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);

        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
        lenient().when(stackDto.getStack()).thenReturn(stack);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(stackDto.getPlatformVariant()).thenReturn(PLATFORM_VARIANT);
    }

    @Test
    void testRepairByHostGroups() {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackUpdater.updateStackStatus(1L, DetailedStackStatus.REPAIR_IN_PROGRESS)).thenReturn(stack);
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stackDto.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        when(stackStopRestrictionService.isInfrastructureStoppable(stackDto)).thenReturn(StopRestrictionReason.NONE);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairHostGroups(1L, Set.of("hostGroup1"), false));

        verify(flowManager).triggerClusterRepairFlow(eq(1L), eq(Map.of("hostGroup1", List.of("host1"))), eq(RepairType.ALL_AT_ONCE), eq(false), any(),
                eq(false));
    }

    @Test
    void testCMNodeRepairSelectedAndAllStoppedNodesNotSelected() {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.CORE);
        host1.setClusterManagerServer(true);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        HostGroup hostGroup2 = new HostGroup();
        hostGroup2.setName("hostGroup2");
        hostGroup2.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host2 = getHost("host2", hostGroup2.getName(), InstanceStatus.STOPPED, InstanceGroupType.CORE);
        hostGroup2.setInstanceGroup(host2.getInstanceGroup());

        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(stackDto.getNotTerminatedInstanceMetaData()).thenReturn(List.of(host1, host2));
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairHostGroups(1L, Set.of("host1"), false));
        });

        String expectedErrorMessage = "Need to select all stopped nodes as CM node is selected for repair.";
        assertEquals(expectedErrorMessage,
                exception.getMessage());

    }

    @Test
    void repairValidationShouldFailWhenStackUsesCCMAndHasMultipleGatewayInstances() {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.GATEWAY);
        InstanceMetaData host2 = getHost("host2", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.GATEWAY);
        host2.setInstanceGroup(host1.getInstanceGroup());
        host1.getInstanceGroup().getAllInstanceMetaData().add(host2);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(stackDto.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(host1.getInstanceGroup(), List.of(host1, host2))));
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairHostGroups(1L, Set.of("hostGroup1"), false)));

        assertEquals("Repair is not supported when the cluster uses cluster proxy and has multiple gateway nodes. This will be fixed in future releases.",
                exception.getMessage());
    }

    @Test
    void testCanRepairCoreTypeNode() {
        cluster.setDatabaseServerCrn("dbCrn");
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_RUNNING, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setStatus(AVAILABLE);
        when(redbeamsClientService.getByCrn(eq("dbCrn"))).thenReturn(databaseServerV4Response);
        when(stackStopRestrictionService.isInfrastructureStoppable(stackDto)).thenReturn(StopRestrictionReason.NONE);

        Result result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairWithDryRun(stack.getId()));

        assertTrue(result.isSuccess());
        verifyNoInteractions(stackUpdater, flowManager, resourceService);
    }

    @Test
    void testCanRepairPrewarmedGatewayWithRepairPossibleBasedOnDBSetup() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(mock(Image.class));
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.isPrewarmed()).thenReturn(true);
        when(imageCatalogService.getImage(any(), any(), any(), any())).thenReturn(StatedImage.statedImage(image, "catalogUrl", "catalogName"));
        when(clusterDBValidationService.isGatewayRepairEnabled(cluster)).thenReturn(true);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        when(stackStopRestrictionService.isInfrastructureStoppable(stackDto)).thenReturn(StopRestrictionReason.NONE);

        Result result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairWithDryRun(stack.getId()));

        assertTrue(result.isSuccess());
        verifyNoInteractions(stackUpdater, flowManager, resourceService);
    }

    @Test
    void testCannotRepairBaseImageGateway() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(mock(Image.class));
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.isPrewarmed()).thenReturn(false);
        when(imageCatalogService.getImage(any(), any(), any(), any())).thenReturn(StatedImage.statedImage(image, "catalogUrl", "catalogName"));
        when(clusterDBValidationService.isGatewayRepairEnabled(cluster)).thenReturn(true);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);

        Result result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairWithDryRun(stack.getId()));

        assertFalse(result.isSuccess());
        verifyNoInteractions(stackUpdater, flowManager, resourceService);
    }

    @Test
    void testCannotRepairGatewayWithoutExternalDatabase() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(mock(Image.class));
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.isPrewarmed()).thenReturn(true);
        when(imageCatalogService.getImage(any(), any(), any(), any())).thenReturn(StatedImage.statedImage(image, "catalogUrl", "catalogName"));
        when(clusterDBValidationService.isGatewayRepairEnabled(cluster)).thenReturn(false);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);

        Result result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairWithDryRun(stack.getId()));

        assertFalse(result.isSuccess());
        verifyNoInteractions(stackUpdater, flowManager, resourceService);
    }

    @Test
    void testRepairByNodeIds() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        hostGroup1.setInstanceGroup(instanceGroup);

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);

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
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(resourceService.findByStackIdAndType(stack.getId(), volumeSet.getResourceType())).thenReturn(List.of(volumeSet));
        when(stackStopRestrictionService.isInfrastructureStoppable(stackDto)).thenReturn(StopRestrictionReason.NONE);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairNodes(1L, Set.of("instanceId1"), false, false));
        verify(resourceService).findByStackIdAndType(stack.getId(), volumeSet.getResourceType());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Resource>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceService).saveAll(saveCaptor.capture());
        assertFalse(resourceAttributeUtil.getTypedAttributes(saveCaptor.getValue().get(0), VolumeSetAttributes.class).get().getDeleteOnTermination());
        verify(flowManager).triggerClusterRepairFlow(eq(1L), eq(Map.of("hostGroup1", List.of("host1Name.healthy"))), eq(RepairType.ALL_AT_ONCE),
                eq(false), any(), eq(false));
    }

    @Test
    void shouldNotUpdateStackStateWhenThereAreNoNodesToRepair() {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairHostGroups(1L, Set.of("hostGroup1"), false));
        });

        assertEquals("Repairable node list is empty. Please check node statuses and try again.", exception.getMessage());
        verifyEventArguments(CLUSTER_MANUALRECOVERY_COULD_NOT_START, "Repairable node list is empty. Please check node statuses and try again.");
        verifyNoInteractions(stackUpdater);
    }

    @Test
    void repairShouldFailIfNotAvailableDatabaseExistsForCluster() {
        cluster.setDatabaseServerCrn("dbCrn");

        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setStatus(STOPPED);
        when(redbeamsClientService.getByCrn(eq("dbCrn"))).thenReturn(databaseServerV4Response);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairHostGroups(1L, Set.of("hostGroup1"), false));
        });

        assertEquals("Database dbCrn is not in AVAILABLE status, could not start node replacement.", exception.getMessage());
        verifyEventArguments(CLUSTER_MANUALRECOVERY_COULD_NOT_START, "Database dbCrn is not in AVAILABLE status, could not start node replacement.");
        verifyNoInteractions(stackUpdater);
    }

    @Test
    void testValidateRepairWhenFreeIpaNotAvailable() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(false);

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                underTest.validateRepair(ManualClusterRepairMode.ALL, STACK_ID, Collections.emptySet(), false);

        assertEquals(1, actual.getError().getValidationErrors().size());
        assertEquals("Action cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state.",
                actual.getError().getValidationErrors().get(0));
    }

    @Test
    void testValidateRepairWhenEnvNotAvailable() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME))
                .thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(false);

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                underTest.validateRepair(ManualClusterRepairMode.ALL, STACK_ID, Collections.emptySet(), false);

        assertEquals(1, actual.getError().getValidationErrors().size());
        assertEquals("Action cannot be performed because the Environment isn't available. Please check the Environment state.",
                actual.getError().getValidationErrors().get(0));
    }

    @Test
    void testValidateRepairWhenOneGWUnhealthyAndNotSelected() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);

        InstanceMetaData primaryGW = new InstanceMetaData();
        primaryGW.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("gateway");
        primaryGW.setInstanceGroup(instanceGroup);
        primaryGW.setInstanceId("i-ffffaaaa");

        InstanceMetaData secondaryGW = new InstanceMetaData();
        secondaryGW.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        secondaryGW.setInstanceGroup(instanceGroup);
        secondaryGW.setInstanceId("i-acbdef1");

        ArrayList<InstanceMetadataView> gatewayInstances = new ArrayList<>();
        gatewayInstances.add(primaryGW);
        gatewayInstances.add(secondaryGW);


        when(stackDto.getNotTerminatedGatewayInstanceMetadata()).thenReturn(gatewayInstances);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                    underTest.validateRepair(ManualClusterRepairMode.HOST_GROUP, STACK_ID, Set.of("idbroker"), false);
            assertEquals(1, actual.getError().getValidationErrors().size());
            assertEquals("List of unhealthy gateway nodes [i-ffffaaaa]. Gateway nodes must be repaired first.",
                    actual.getError().getValidationErrors().getFirst());
        });
    }

    @Test
    void testValidateRepairWhenTwoGWUnhealthyAndNotSelected() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);

        InstanceMetaData primaryGW = new InstanceMetaData();
        primaryGW.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("gateway");
        primaryGW.setInstanceGroup(instanceGroup);
        primaryGW.setInstanceId("i-ffffaaaa");

        InstanceMetaData secondaryGW = new InstanceMetaData();
        secondaryGW.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        secondaryGW.setInstanceGroup(instanceGroup);
        secondaryGW.setInstanceId("i-ffffbbbb");

        ArrayList<InstanceMetadataView> gatewayInstances = new ArrayList<>();
        gatewayInstances.add(primaryGW);
        gatewayInstances.add(secondaryGW);

        when(stackDto.getNotTerminatedGatewayInstanceMetadata()).thenReturn(gatewayInstances);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                    underTest.validateRepair(ManualClusterRepairMode.HOST_GROUP, STACK_ID, Set.of("idbroker"), false);
            assertEquals(1, actual.getError().getValidationErrors().size());
            assertEquals("List of unhealthy gateway nodes [i-ffffbbbb, i-ffffaaaa]. Gateway nodes must be repaired first.",
                    actual.getError().getValidationErrors().getFirst());
        });
    }

    @Test
    void testValidateRepairWhenNoUnhealthyGWAndNotSelected() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        when(stackStopRestrictionService.isInfrastructureStoppable(stackDto)).thenReturn(StopRestrictionReason.NONE);
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("idbroker");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("idbroker1", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());
        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));

        InstanceMetaData primaryGW = new InstanceMetaData();
        primaryGW.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("gateway");
        primaryGW.setInstanceGroup(instanceGroup);

        InstanceMetaData secondaryGW = new InstanceMetaData();
        secondaryGW.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        secondaryGW.setInstanceGroup(instanceGroup);

        ArrayList<InstanceMetadataView> gatewayInstances = new ArrayList<>();
        gatewayInstances.add(primaryGW);
        gatewayInstances.add(secondaryGW);

        when(stackDto.getNotTerminatedGatewayInstanceMetadata()).thenReturn(gatewayInstances);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                    underTest.validateRepair(ManualClusterRepairMode.HOST_GROUP, STACK_ID, Set.of("idbroker"), false);
            assertTrue(actual.isSuccess());
        });
    }

    @Test
    void testValidateRepairWhenReattachSupported() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        when(stackStopRestrictionService.isInfrastructureStoppable(stackDto)).thenReturn(StopRestrictionReason.NONE);

        String idbrokerGroupName = "idbroker";
        HostGroup idbrokerHg = new HostGroup();
        idbrokerHg.setName(idbrokerGroupName);
        idbrokerHg.setRecoveryMode(RecoveryMode.MANUAL);
        Set<VolumeTemplate> volumeTemplateSet = Set.of(createVolumeTemplate(AwsDiskType.Standard), createVolumeTemplate(AwsDiskType.Ephemeral));
        InstanceGroup idbrokerIg = createUnhealthyInstanceGroup(idbrokerGroupName, volumeTemplateSet);
        idbrokerHg.setInstanceGroup(idbrokerIg);
        stack.setInstanceGroups(Set.of(idbrokerIg));
        when(hostGroupService.getByCluster(CLUSTER_ID)).thenReturn(Set.of(idbrokerHg));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                    underTest.validateRepair(ManualClusterRepairMode.HOST_GROUP, STACK_ID, Set.of(idbrokerGroupName), false);
            assertEquals(1, actual.getSuccess().size());
            assertNotNull(actual.getSuccess().get(hostGroupName(idbrokerGroupName)));
        });
    }

    @Test
    void testValidateRepairWhenReattachNotSupported() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        when(stackStopRestrictionService.isInfrastructureStoppable(stackDto)).thenReturn(StopRestrictionReason.EPHEMERAL_VOLUMES);

        String idbrokerGroupName = "idbroker";
        HostGroup idbrokerHg = new HostGroup();
        idbrokerHg.setName(idbrokerGroupName);
        idbrokerHg.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceGroup idbrokerIg = createUnhealthyInstanceGroup(idbrokerGroupName, Set.of(createVolumeTemplate(AwsDiskType.Ephemeral)));
        idbrokerHg.setInstanceGroup(idbrokerIg);
        stack.setInstanceGroups(Set.of(idbrokerIg));
        when(hostGroupService.getByCluster(CLUSTER_ID)).thenReturn(Set.of(idbrokerHg));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                    underTest.validateRepair(ManualClusterRepairMode.HOST_GROUP, STACK_ID, Set.of(idbrokerGroupName), false);
            assertEquals("Reattach not supported for this disk type.", actual.getError().getValidationErrors().get(0));
        });
    }

    @Test
    void testValidateRepairWhenSaltVersionOutdatedRepairModeAll() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        when(stackStopRestrictionService.isInfrastructureStoppable(stackDto)).thenReturn(StopRestrictionReason.NONE);
        when(saltVersionUpgradeService.getGatewayInstancesWithOutdatedSaltVersion(eq(stackDto))).thenReturn(Set.of("instance1"));

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());
        when(hostGroupService.getByCluster(CLUSTER_ID)).thenReturn(Set.of(hostGroup1));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                    underTest.validateRepair(ManualClusterRepairMode.ALL, STACK_ID, Set.of(), false);
            assertEquals(1, actual.getSuccess().size());
            assertNotNull(actual.getSuccess().get(hostGroupName(hostGroup1.getName())));
        });
    }

    @Test
    void testValidateRepairWhenSaltVersionOutdatedRepairModeNodeId() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        when(saltVersionUpgradeService.getGatewayInstancesWithOutdatedSaltVersion(eq(stackDto))).thenReturn(Set.of("instance1"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                    underTest.validateRepair(ManualClusterRepairMode.NODE_ID, STACK_ID, Set.of("hostGroup1"), false);
            assertEquals("Gateway node(s) has outdated Salt version. Please include gateway node(s) in the repair selection!",
                    actual.getError().getValidationErrors().get(0));
        });
    }

    @Test
    void testValidateRepairWhenSaltVersionOutdatedRepairModeHostGroup() {
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(freeipaService.checkFreeipaRunning(stack.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        when(saltVersionUpgradeService.getGatewayInstancesWithOutdatedSaltVersion(eq(stackDto))).thenReturn(Set.of("instance1"));
        InstanceGroupView gatewayGroup = mock(InstanceGroupView.class);
        when(gatewayGroup.getGroupName()).thenReturn("gateway");
        doReturn(gatewayGroup).when(stackDto).getPrimaryGatewayGroup();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> actual =
                    underTest.validateRepair(ManualClusterRepairMode.HOST_GROUP, STACK_ID, Set.of("core"), false);
            assertEquals("Gateway node(s) has outdated Salt version. Please include gateway node(s) in the repair selection!",
                    actual.getError().getValidationErrors().get(0));
        });
    }

    @Test
    void markVolumesToNonDeletableBasedOnInstanceIdTest() {
        InstanceMetaData instanceMetaData1 = createInstanceMetaData("i-1", InstanceStatus.CREATED, "worker-1");
        InstanceMetaData instanceMetaData2 = createInstanceMetaData("i-2", InstanceStatus.CREATED, "worker-2");

        ArrayList<InstanceMetadataView> instances = new ArrayList<>();
        instances.add(instanceMetaData1);
        instances.add(instanceMetaData2);

        Resource volumeSet = new Resource();
        VolumeSetAttributes attributes = new VolumeSetAttributes("eu-west-1", Boolean.TRUE, "", List.of(), 100, "standard");
        volumeSet.setAttributes(new Json(attributes));
        volumeSet.setInstanceId("i-1");
        volumeSet.setResourceType(ResourceType.AWS_VOLUMESET);

        when(resourceService.findByStackIdAndType(stack.getId(), volumeSet.getResourceType())).thenReturn(List.of(volumeSet));
        underTest.markVolumesToNonDeletable(stack, instances);

        ArgumentCaptor<List<Resource>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceService).saveAll(saveCaptor.capture());
        assertFalse(resourceAttributeUtil.getTypedAttributes(saveCaptor.getValue().get(0), VolumeSetAttributes.class).get().getDeleteOnTermination());
    }

    @Test
    void markVolumesToNonDeletableBasedOnFqdnTest() {
        InstanceMetaData instanceMetaData1 = createInstanceMetaData("i-1", InstanceStatus.CREATED, "worker-1");
        InstanceMetaData instanceMetaData2 = createInstanceMetaData("i-2", InstanceStatus.CREATED, "worker-2");

        ArrayList<InstanceMetadataView> instances = new ArrayList<>();
        instances.add(instanceMetaData1);
        instances.add(instanceMetaData2);

        Resource volumeSet = new Resource();
        VolumeSetAttributes attributes = new VolumeSetAttributes("eu-west-1", Boolean.TRUE, "", List.of(), 100, "standard");
        attributes.setDiscoveryFQDN("worker-2");
        volumeSet.setAttributes(new Json(attributes));
        volumeSet.setInstanceId("i-4");
        volumeSet.setResourceType(ResourceType.AWS_VOLUMESET);

        when(resourceService.findByStackIdAndType(stack.getId(), volumeSet.getResourceType())).thenReturn(List.of(volumeSet));
        underTest.markVolumesToNonDeletable(stack, instances);

        ArgumentCaptor<List<Resource>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceService).saveAll(saveCaptor.capture());
        assertFalse(resourceAttributeUtil.getTypedAttributes(saveCaptor.getValue().get(0), VolumeSetAttributes.class).get().getDeleteOnTermination());
    }

    @Test
    void doNotNarkVolumesToNonDeletableTest() {
        InstanceMetaData instanceMetaData1 = createInstanceMetaData("i-1", InstanceStatus.CREATED, "worker-1");
        InstanceMetaData instanceMetaData2 = createInstanceMetaData("i-2", InstanceStatus.CREATED, "worker-2");

        ArrayList<InstanceMetadataView> instances = new ArrayList<>();
        instances.add(instanceMetaData1);
        instances.add(instanceMetaData2);

        Resource volumeSet = new Resource();
        VolumeSetAttributes attributes = new VolumeSetAttributes("eu-west-1", Boolean.TRUE, "", List.of(), 100, "standard");
        attributes.setDiscoveryFQDN("worker-4");
        volumeSet.setAttributes(new Json(attributes));
        volumeSet.setInstanceId("i-4");
        volumeSet.setResourceType(ResourceType.AWS_VOLUMESET);

        when(resourceService.findByStackIdAndType(stack.getId(), volumeSet.getResourceType())).thenReturn(List.of(volumeSet));
        underTest.markVolumesToNonDeletable(stack, instances);

        ArgumentCaptor<List<Resource>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceService).saveAll(saveCaptor.capture());
        assertTrue(saveCaptor.getValue().isEmpty());
    }

    @Test
    void testRepairAllDuringOsUpgrade() {
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        HostGroup hostGroup2 = new HostGroup();
        hostGroup2.setName("hostGroup2");
        hostGroup2.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host2 = getHost("host2", hostGroup2.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.CORE);
        hostGroup2.setInstanceGroup(host2.getInstanceGroup());

        when(freeipaService.checkFreeipaRunning(stackDto.getEnvironmentCrn(), STACK_NAME)).thenReturn(true);
        when(environmentService.environmentStatusInDesiredState(stack, Set.of(EnvironmentStatus.AVAILABLE))).thenReturn(true);
        when(stackStopRestrictionService.isInfrastructureStoppable(stackDto)).thenReturn(StopRestrictionReason.NONE);
        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1, hostGroup2));
        when(stackUpgradeService.calculateUpgradeVariant(eq(stackView), eq(USER_CRN), eq(true))).thenReturn(PLATFORM_VARIANT);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.repairAll(stackView, true, true));
        verify(flowManager, times(1)).triggerClusterRepairFlow(eq(STACK_ID), eq(Map.of("hostGroup1", List.of("host1"), "hostGroup2", List.of("host2"))),
                eq(RepairType.ALL_AT_ONCE), eq(false), eq(PLATFORM_VARIANT), eq(true));
    }

    private InstanceGroup createUnhealthyInstanceGroup(String groupName, Set<VolumeTemplate> volumeTemplates) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(groupName);
        instanceGroup.setTemplate(createTemplate(volumeTemplates));
        InstanceMetaData instanceMetaData = createInstanceMetaData("instanceId", InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetaData.setDiscoveryFQDN("fqdn");
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        return instanceGroup;
    }

    private Template createTemplate(Set<VolumeTemplate> volumeTemplates) {
        Template idbrokerTemplate = new Template();
        idbrokerTemplate.setVolumeTemplates(volumeTemplates);
        return idbrokerTemplate;
    }

    private InstanceMetaData createInstanceMetaData(String instanceId, InstanceStatus instanceStatus) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(instanceId);
        instanceMetaData.setInstanceStatus(instanceStatus);
        return instanceMetaData;
    }

    private InstanceMetaData createInstanceMetaData(String instanceId, InstanceStatus instanceStatus, String fqdn) {
        InstanceMetaData instanceMetaData = createInstanceMetaData(instanceId, instanceStatus);
        instanceMetaData.setDiscoveryFQDN(fqdn);
        return instanceMetaData;
    }

    private VolumeTemplate createVolumeTemplate(AwsDiskType diskType) {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(diskType.value());
        return volumeTemplate;
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
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));

        return instanceMetaData;
    }

}