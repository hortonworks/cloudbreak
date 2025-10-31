package com.sequenceiq.cloudbreak.core.flow2.service;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.CORE_MODIFY_SELINUX_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.CoreVerticalScalingTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseBackupTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseRestoreTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DeleteVolumesTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MaintenanceModeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.RollingVerticalScaleFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserOperation;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreRootVolumeUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.service.FlowCancelService;

@ExtendWith(MockitoExtension.class)
class ReactorFlowManagerTest {

    private static final Long STACK_ID = 1L;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenantName:user:userName";

    private static final String BACKUP_LOCATION = "/path/to/backup";

    @Mock
    private ReactorNotifier reactorNotifier;

    @Mock
    private AsyncTaskExecutor asyncTaskExecutor;

    @Mock
    private TerminationTriggerService terminationTriggerService;

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private StackService stackService;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private EntitlementService entitlementService;

    private Stack stack;

    @InjectMocks
    private ReactorFlowManager underTest;

    @BeforeEach
    void setUp() {
        reset(reactorNotifier);
        stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        lenient().when(asyncTaskExecutor.submit(captor.capture())).then(invocation -> {
            captor.getValue().run();
            return null;
        });
    }

    @Test
    void shouldReturnTheNextFailureTransition() {
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setScalingAdjustment(5);
        instanceGroupAdjustment.setInstanceGroup("hostgroup");
        HostGroupAdjustmentV4Request hostGroupAdjustment = new HostGroupAdjustmentV4Request();
        hostGroupAdjustment.setHostGroup("hostgroup");
        hostGroupAdjustment.setScalingAdjustment(5);
        Map<String, Set<Long>> instanceIdsByHostgroup = new HashMap<>();
        instanceIdsByHostgroup.put("hostgroup", Collections.singleton(1L));
        List<String> instanceIds = List.of("i-1", "i-2");
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, "imageid");
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        doReturn(clusterView).when(stackDto).getCluster();

        when(stackService.getPlatformVariantByStackId(STACK_ID)).thenReturn(cloudPlatformVariant);
        when(cloudPlatformVariant.getVariant()).thenReturn(Variant.variant("AWS"));
        when(stackService.getById(STACK_ID)).thenReturn(stack);

        underTest.triggerProvisioning(STACK_ID);
        underTest.triggerClusterInstall(STACK_ID);
        underTest.triggerClusterReInstall(STACK_ID);
        underTest.triggerStackStop(STACK_ID);
        underTest.triggerStackStart(STACK_ID);
        underTest.triggerClusterStop(STACK_ID);
        underTest.triggerClusterStart(STACK_ID);
        underTest.triggerTermination(STACK_ID);
        underTest.triggerStackUpscale(STACK_ID, instanceGroupAdjustment, true);
        underTest.triggerStackDownscale(STACK_ID, instanceGroupAdjustment);
        underTest.triggerStackRemoveInstance(STACK_ID, "hostgroup", 5L);
        underTest.triggerStackRemoveInstance(STACK_ID, "hostgroup", 5L, false);
        underTest.triggerStackRemoveInstances(STACK_ID, instanceIdsByHostgroup, false);
        underTest.triggerClusterUpscale(STACK_ID, hostGroupAdjustment);
        underTest.triggerClusterDownscale(STACK_ID, hostGroupAdjustment);
        underTest.triggerClusterSync(STACK_ID);
        underTest.triggerClusterSyncWithoutCheck(STACK_ID);
        underTest.triggerStackSync(STACK_ID);
        underTest.triggerFullSync(STACK_ID);
        underTest.triggerFullSyncWithoutCheck(STACK_ID);
        underTest.triggerClusterCredentialReplace(STACK_ID, "admin", "admin1");
        underTest.triggerClusterCredentialUpdate(STACK_ID, "admin1");
        underTest.triggerClusterTermination(stack, false, USER_CRN);
        underTest.triggerClusterTermination(stack, true, USER_CRN);
        underTest.triggerManualRepairFlow(STACK_ID);
        underTest.triggerStackRepairFlow(STACK_ID, new UnhealthyInstances());
        underTest.triggerClusterRepairFlow(STACK_ID, new HashMap<>(), false);
        underTest.triggerClusterRepairFlow(STACK_ID, new HashMap<>(), RepairType.ALL_AT_ONCE, false, "variant", false);
        underTest.triggerStackImageUpdate(new ImageChangeDto(STACK_ID, "asdf"));
        underTest.triggerMaintenanceModeValidationFlow(STACK_ID);
        underTest.triggerClusterCertificationRenewal(STACK_ID);
        underTest.triggerDatalakeClusterUpgrade(STACK_ID, "asdf", true);
        underTest.triggerDistroXUpgrade(STACK_ID, imageChangeDto, false, false, "variant", false, "aRuntime");
        underTest.triggerClusterUpgradePreparation(STACK_ID, imageChangeDto, "runtimeVersion");
        underTest.triggerSaltUpdate(STACK_ID);
        underTest.triggerPillarConfigurationUpdate(STACK_ID);
        underTest.triggerDatalakeDatabaseBackup(STACK_ID, null, null, true, Collections.emptyList(), 0, false);
        underTest.triggerDatalakeDatabaseRestore(STACK_ID, null, null, 0, false);
        underTest.triggerAutoTlsCertificatesRotation(STACK_ID, new CertificatesRotationV4Request());
        underTest.triggerStackLoadBalancerUpdate(STACK_ID);
        underTest.triggerSyncComponentVersionsFromCm(STACK_ID, Set.of());
        underTest.triggerDatalakeClusterRecovery(STACK_ID);
        underTest.triggerStopStartStackUpscale(STACK_ID, instanceGroupAdjustment, true);
        underTest.triggerStopStartStackDownscale(STACK_ID, instanceIdsByHostgroup, false);
        underTest.triggerRestartInstances(STACK_ID, instanceIds, false);
        underTest.triggerClusterServicesRestart(STACK_ID, true, false, false);
        underTest.triggerClusterProxyConfigReRegistration(STACK_ID, false, "");
        underTest.triggerRdsUpgrade(STACK_ID, TargetMajorVersion.VERSION_11, null, null);
        underTest.triggerModifyProxyConfig(STACK_ID, null);
        underTest.triggerVerticalScale(STACK_ID, new StackVerticalScaleV4Request());
        underTest.triggerOsUpgradeByUpgradeSetsFlow(STACK_ID, "AWS", new ImageChangeDto(STACK_ID, null), List.of());
        underTest.triggerDetermineDatalakeDataSizes(STACK_ID, "asdf");
        underTest.triggerDeleteVolumes(STACK_ID, new StackDeleteVolumesRequest());
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("test");
        diskUpdateRequest.setDiskType(DiskType.ADDITIONAL_DISK);
        underTest.triggerStackUpdateDisks(stackDto, diskUpdateRequest);
        underTest.triggerSecretRotation(STACK_ID, "CRN", Lists.newArrayList(), RotationFlowExecutionType.ROTATE, null);
        underTest.triggerInstanceMetadataUpdate(stackDto, InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED);
        underTest.triggerRefreshEntitlementParams(STACK_ID, "CRN", Collections.emptyMap(), Boolean.FALSE);
        StackAddVolumesRequest stackAddVolumesRequest = mock(StackAddVolumesRequest.class);
        doReturn(CloudVolumeUsageType.GENERAL.toString()).when(stackAddVolumesRequest).getCloudVolumeUsageType();
        underTest.triggerAddVolumes(STACK_ID, stackAddVolumesRequest);
        underTest.triggerRotateRdsCertificate(STACK_ID);
        underTest.triggerSkuMigration(STACK_ID, true);
        underTest.triggerExternalDatabaseUserOperation(STACK_ID, "name", "crn", ExternalDatabaseUserOperation.CREATION, DatabaseType.HIVE, "user");
        underTest.triggerZookeeperToKraftMigration(STACK_ID);
        underTest.triggerZookeeperToKraftMigrationFinalization(STACK_ID);
        underTest.triggerZookeeperToKraftMigrationRollback(STACK_ID);
        underTest.triggerUpdatePublicDnsEntriesInPem(STACK_ID);

        int count = 0;
        for (Method method : underTest.getClass().getDeclaredMethods()) {
            if (method.getName().startsWith("trigger")) {
                count++;
            }
        }
        // -6: 2 notifyWithoutCheck, 1 terminationTriggerService, 1 triggerStackRemoveInstance internal, 1 triggerMigrateRdsToTls, 1 rootVolumeUpdateFlow
        verify(reactorNotifier, times(count - 7)).notify(anyLong(), anyString(), any(Acceptable.class));
        verify(reactorNotifier, times(2)).notifyWithoutCheck(anyLong(), anyString(), any(Acceptable.class));
        verify(terminationTriggerService, times(1)).triggerTermination(stack, true);
        verify(terminationTriggerService, times(1)).triggerTermination(stack, false);
    }

    @Test
    void testClusterTerminationNotForced() {
        underTest.triggerClusterTermination(stack, false, USER_CRN);

        verify(terminationTriggerService, times(1)).triggerTermination(stack, false);
    }

    @Test
    void testClusterTerminationForced() {
        underTest.triggerClusterTermination(stack, true, USER_CRN);

        verify(terminationTriggerService, times(1)).triggerTermination(stack, true);
    }

    @Test
    void testTriggerUpscaleWithoutClusterEvent() {
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setInstanceGroup("ig");
        instanceGroupAdjustment.setScalingAdjustment(3);

        when(stackService.getPlatformVariantByStackId(STACK_ID)).thenReturn(cloudPlatformVariant);
        when(cloudPlatformVariant.getVariant()).thenReturn(Variant.variant("AWS"));

        underTest.triggerStackUpscale(stack.getId(), instanceGroupAdjustment, false);

        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier, times(1)).notify(eq(stack.getId()), eq(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT), captor.capture());
        StackAndClusterUpscaleTriggerEvent event = (StackAndClusterUpscaleTriggerEvent) captor.getValue();
        assertEquals(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.selector());
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(instanceGroupAdjustment.getInstanceGroup(), event.getHostGroupsWithAdjustment().keySet().stream().findFirst().get());
        assertEquals(instanceGroupAdjustment.getScalingAdjustment(), event.getHostGroupsWithAdjustment().values().stream().findFirst().get());
        assertEquals(ScalingType.UPSCALE_ONLY_STACK, event.getScalingType());
    }

    @Test
    void testTriggerUpscaleWithClusterEvent() {
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setInstanceGroup("ig");
        instanceGroupAdjustment.setScalingAdjustment(3);

        when(stackService.getPlatformVariantByStackId(STACK_ID)).thenReturn(cloudPlatformVariant);
        when(cloudPlatformVariant.getVariant()).thenReturn(Variant.variant("AWS"));

        underTest.triggerStackUpscale(stack.getId(), instanceGroupAdjustment, true);

        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier, times(1)).notify(eq(stack.getId()), eq(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT), captor.capture());
        StackAndClusterUpscaleTriggerEvent event = (StackAndClusterUpscaleTriggerEvent) captor.getValue();
        assertEquals(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.selector());
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(instanceGroupAdjustment.getInstanceGroup(), event.getHostGroupsWithAdjustment().keySet().stream().findFirst().get());
        assertEquals(instanceGroupAdjustment.getScalingAdjustment(), event.getHostGroupsWithAdjustment().values().stream().findFirst().get());
        assertEquals(ScalingType.UPSCALE_TOGETHER, event.getScalingType());
    }

    @Test
    void testTriggerStackImageUpdate() {
        long stackId = 1L;
        String imageID = "imageID";
        String imageCatalogName = "imageCatalogName";
        String imageCatalogUrl = "imageCatalogUrl";
        underTest.triggerStackImageUpdate(new ImageChangeDto(stackId, imageID, imageCatalogName, imageCatalogUrl));
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(stackId), eq(FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT), captor.capture());
        StackImageUpdateTriggerEvent event = (StackImageUpdateTriggerEvent) captor.getValue();
        assertEquals(FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT, event.selector());
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(imageCatalogName, event.getImageCatalogName());
        assertEquals(imageID, event.getNewImageId());
        assertEquals(imageCatalogUrl, event.getImageCatalogUrl());
    }

    @Test
    void testTriggerMaintenanceModeValidationFlow() {
        long stackId = 1L;
        underTest.triggerMaintenanceModeValidationFlow(stackId);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(stackId), eq(FlowChainTriggers.CLUSTER_MAINTENANCE_MODE_VALIDATION_TRIGGER_EVENT), captor.capture());
        MaintenanceModeValidationTriggerEvent event = (MaintenanceModeValidationTriggerEvent) captor.getValue();
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(FlowChainTriggers.CLUSTER_MAINTENANCE_MODE_VALIDATION_TRIGGER_EVENT, event.selector());
    }

    @Test
    void testTriggerDatabaseBackupFlowchain() {
        long stackId = 1L;
        String backupId = UUID.randomUUID().toString();
        underTest.triggerDatalakeDatabaseBackup(stackId, BACKUP_LOCATION, backupId, true, Collections.emptyList(), 0, false);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(stackId), eq(FlowChainTriggers.DATALAKE_DATABASE_BACKUP_CHAIN_TRIGGER_EVENT), captor.capture());
        DatabaseBackupTriggerEvent event = (DatabaseBackupTriggerEvent) captor.getValue();
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(backupId, event.getBackupId());
        assertEquals(BACKUP_LOCATION, event.getBackupLocation());
        assertEquals(FlowChainTriggers.DATALAKE_DATABASE_BACKUP_CHAIN_TRIGGER_EVENT, event.selector());
    }

    @Test
    void testTriggerDatabaseBackupFlowchainWithCustomizedMaxDuration() {
        long stackId = 1L;
        int databaseMaxDurationInMin = 20;
        String backupId = UUID.randomUUID().toString();
        underTest.triggerDatalakeDatabaseBackup(stackId, BACKUP_LOCATION, backupId, true, Collections.emptyList(), databaseMaxDurationInMin, false);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(stackId), eq(FlowChainTriggers.DATALAKE_DATABASE_BACKUP_CHAIN_TRIGGER_EVENT), captor.capture());
        DatabaseBackupTriggerEvent event = (DatabaseBackupTriggerEvent) captor.getValue();
        assertEquals(databaseMaxDurationInMin, event.getDatabaseMaxDurationInMin());
    }

    @Test
    void testTriggerDatabaseBackupRestoreWithCustomizedMaxDuration() {
        long stackId = 1L;
        int databaseMaxDurationInMin = 20;
        String backupId = UUID.randomUUID().toString();
        underTest.triggerDatalakeDatabaseRestore(stackId, BACKUP_LOCATION, backupId, databaseMaxDurationInMin, false);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(stackId), eq("DATABASE_RESTORE_EVENT"), captor.capture());
        DatabaseRestoreTriggerEvent event = (DatabaseRestoreTriggerEvent) captor.getValue();
        assertEquals(databaseMaxDurationInMin, event.getDatabaseMaxDurationInMin());
    }

    @Test
    void testTriggerDeleteVolumes() {
        long stackId = 1L;
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(stackId);
        stackDeleteVolumesRequest.setGroup("TEST");
        underTest.triggerDeleteVolumes(stackId, stackDeleteVolumesRequest);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(stackId), eq(DELETE_VOLUMES_VALIDATION_EVENT.event()), captor.capture());
        DeleteVolumesTriggerEvent event = (DeleteVolumesTriggerEvent) captor.getValue();
        assertEquals(DELETE_VOLUMES_VALIDATION_EVENT.event(), event.selector());
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(stackDeleteVolumesRequest, event.getStackDeleteVolumesRequest());
        assertEquals(stackId, event.getResourceId().longValue());
    }

    @Test
    void testTriggerStackUpdateDisks() {
        StackDto stackDto = mock(StackDto.class);
        doReturn(1L).when(stackDto).getId();
        DiskUpdateRequest diskUpdateRequest = mock(DiskUpdateRequest.class);
        doReturn("TEST").when(diskUpdateRequest).getGroup();
        doReturn(DiskType.ADDITIONAL_DISK).when(diskUpdateRequest).getDiskType();
        ClusterView clusterView = mock(ClusterView.class);
        doReturn(clusterView).when(stackDto).getCluster();
        underTest.triggerStackUpdateDisks(stackDto, diskUpdateRequest);
        ArgumentCaptor<DistroXDiskUpdateEvent> eventCaptor = ArgumentCaptor.forClass(DistroXDiskUpdateEvent.class);
        verify(reactorNotifier).notify(eq(1L), eq(DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_VALIDATION_EVENT.event()), eventCaptor.capture());
        assertEquals(stackDto.getId(), eventCaptor.getValue().getStackId());
    }

    @Test
    void testTriggerAddVolumes() {
        long stackId = 1L;
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);
        underTest.triggerAddVolumes(stackId, stackAddVolumesRequest);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(stackId), eq(ADD_VOLUMES_TRIGGER_EVENT.event()), captor.capture());
        AddVolumesRequest event = (AddVolumesRequest) captor.getValue();
        assertEquals(ADD_VOLUMES_TRIGGER_EVENT.event(), event.selector());
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(stackId, event.getResourceId().longValue());
    }

    @Test
    void testTriggerRotateRdsCertificate() {
        underTest.triggerRotateRdsCertificate(STACK_ID);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(STACK_ID), eq(ROTATE_RDS_CERTIFICATE_EVENT.event()), captor.capture());
        RotateRdsCertificateTriggerRequest event = (RotateRdsCertificateTriggerRequest) captor.getValue();
        assertEquals(ROTATE_RDS_CERTIFICATE_EVENT.event(), event.selector());
        assertEquals(stack.getId(), event.getResourceId());
    }

    @Test
    void testTriggerRootVolumeUpdateFlow() {
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setDiskType(DiskType.ADDITIONAL_DISK);
        diskUpdateRequest.setGroup("executor");
        diskUpdateRequest.setVolumeType("gp2");
        diskUpdateRequest.setSize(100);
        Map<String, List<String>> updatedNodesMap = Map.of();
        underTest.triggerRootVolumeUpdateFlow(1L, updatedNodesMap, diskUpdateRequest);
        ArgumentCaptor<CoreRootVolumeUpdateTriggerEvent> eventCaptor = ArgumentCaptor.forClass(CoreRootVolumeUpdateTriggerEvent.class);
        verify(reactorNotifier).notify(eq(1L), eq(FlowChainTriggers.CORE_ROOT_VOLUME_UPDATE_TRIGGER_EVENT), eventCaptor.capture());
        assertEquals(1L, eventCaptor.getValue().getResourceId());
    }

    @Test
    void testTriggerEnableSelinux() {
        underTest.triggerModifySelinux(1L, SeLinux.ENFORCING);
        ArgumentCaptor<CoreModifySeLinuxEvent> eventCaptor = ArgumentCaptor.forClass(CoreModifySeLinuxEvent.class);
        verify(reactorNotifier).notify(eq(1L), eq(CORE_MODIFY_SELINUX_EVENT.event()), eventCaptor.capture());
        assertEquals(1L, eventCaptor.getValue().getResourceId());
        assertEquals(CORE_MODIFY_SELINUX_EVENT.event(), eventCaptor.getValue().getSelector());
    }

    @Test
    void testTriggerZookeeperToKraftMigration() {
        underTest.triggerZookeeperToKraftMigration(STACK_ID);
        ArgumentCaptor<MigrateZookeeperToKraftFlowChainTriggerEvent> captor = ArgumentCaptor.forClass(MigrateZookeeperToKraftFlowChainTriggerEvent.class);
        verify(reactorNotifier, times(1)).notify(eq(stack.getId()), eq(MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT), captor.capture());
        MigrateZookeeperToKraftFlowChainTriggerEvent event = captor.getValue();
        assertEquals(1L, captor.getValue().getResourceId());
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT, event.selector());
    }

    @Test
    void testTriggerZookeeperToKraftMigrationFinalization() {
        underTest.triggerZookeeperToKraftMigrationFinalization(STACK_ID);
        ArgumentCaptor<MigrateZookeeperToKraftFinalizationTriggerEvent> captor = ArgumentCaptor.forClass(MigrateZookeeperToKraftFinalizationTriggerEvent.class);
        verify(reactorNotifier, times(1)).notify(eq(stack.getId()), eq(START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event()), captor.capture());
        MigrateZookeeperToKraftFinalizationTriggerEvent event = captor.getValue();
        assertEquals(1L, captor.getValue().getResourceId());
        assertEquals(START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event(), event.selector());
    }

    @Test
    void testTriggerZookeeperToKraftMigrationRollback() {
        underTest.triggerZookeeperToKraftMigrationRollback(STACK_ID);
        ArgumentCaptor<MigrateZookeeperToKraftRollbackTriggerEvent> captor = ArgumentCaptor.forClass(MigrateZookeeperToKraftRollbackTriggerEvent.class);
        verify(reactorNotifier, times(1)).notify(eq(stack.getId()), eq(START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event()), captor.capture());
        MigrateZookeeperToKraftRollbackTriggerEvent event = captor.getValue();
        assertEquals(1L, captor.getValue().getResourceId());
        assertEquals(START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event(), event.selector());
    }

    @Test
    void testTriggerVerticalScaleWithEntitlementEnabled() {
        StackVerticalScaleV4Request request = new StackVerticalScaleV4Request();
        String resourceCrn = "crn:cdp:datahub:us-west-1:account123:cluster:cluster-id";
        String accountId = "account123";
        stack.setResourceCrn(resourceCrn);
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(entitlementService.isVerticalScaleHaEnabled(accountId)).thenReturn(true);

        underTest.triggerVerticalScale(STACK_ID, request);

        ArgumentCaptor<RollingVerticalScaleFlowChainTriggerEvent> captor = ArgumentCaptor.forClass(RollingVerticalScaleFlowChainTriggerEvent.class);
        verify(reactorNotifier, times(1)).notify(eq(STACK_ID), eq(FlowChainTriggers.ROLLING_VERTICALSCALE_CHAIN_TRIGGER_EVENT), captor.capture());
        RollingVerticalScaleFlowChainTriggerEvent event = captor.getValue();
        assertEquals(STACK_ID, event.getResourceId());
        assertEquals(FlowChainTriggers.ROLLING_VERTICALSCALE_CHAIN_TRIGGER_EVENT, event.selector());
        assertEquals(request, event.getRequest());
    }

    @Test
    void testTriggerVerticalScaleWithEntitlementDisabled() {
        StackVerticalScaleV4Request request = new StackVerticalScaleV4Request();
        String resourceCrn = "crn:cdp:datahub:us-west-1:account123:cluster:cluster-id";
        String accountId = "account123";
        stack.setResourceCrn(resourceCrn);
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(entitlementService.isVerticalScaleHaEnabled(accountId)).thenReturn(false);

        underTest.triggerVerticalScale(STACK_ID, request);

        ArgumentCaptor<CoreVerticalScalingTriggerEvent> captor = ArgumentCaptor.forClass(CoreVerticalScalingTriggerEvent.class);
        verify(reactorNotifier, times(1)).notify(eq(STACK_ID), eq(STACK_VERTICALSCALE_EVENT.event()), captor.capture());
        CoreVerticalScalingTriggerEvent event = captor.getValue();
        assertEquals(STACK_ID, event.getResourceId());
        assertEquals(STACK_VERTICALSCALE_EVENT.event(), event.selector());
        assertEquals(request, event.getRequest());
    }

    private static class TestAcceptable implements Acceptable {
        @Override
        public Promise<AcceptResult> accepted() {
            Promise<AcceptResult> a = new Promise<>();
            a.accept(FlowAcceptResult.runningInFlow("FLOW_ID"));
            return a;
        }

        @Override
        public Long getResourceId() {
            return STACK_ID;
        }
    }
}
