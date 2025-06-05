package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.rds.DatabaseUpgradeBackupRestoreChecker;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class UpgradeRdsServiceTest {

    private static final Long CLUSTER_ID = 123L;

    private static final Long STACK_ID = 234L;

    private static final String TARGET_VERSION = "11";

    private static final String BACKUP_LOCATION = "location";

    private static final String BACKUP_STATE = "Creating data backup, it might take a while.";

    private static final String RESTORE_STATE = "Restoring data from the backup, it might take a while.";

    private static final String MIGRATE_STATE = "Migrating database settings.";

    private static final String MIGRATE_SERVICES_STATE = "Migrating services' database settings.";

    private static final String STOP_STATE = "Stopping Runtime Services and Cloudera Manager.";

    private static final String START_STATE = "Starting back Runtime Services.";

    private static final String START_CM_STATE = "Starting back Cloudera Manager.";

    private static final String UPGRADE_STATE = "Upgrading database server.";

    private static final String INSTALL_PG_STATE = "Installing Postgres packages if necessary.";

    private static final String BACKUP_INSTANCE_PROFILE = "BACKUP_INSTANCE_PROFILE";

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private DatabaseUpgradeBackupRestoreChecker backupRestoreChecker;

    @Mock
    private StackView stackView;

    @Mock
    private ClusterView clusterView;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private EnvironmentService environmentClientService;

    @InjectMocks
    private UpgradeRdsService underTest;

    @Test
    public void testRdsUpgradeFinished() {
        underTest.rdsUpgradeFinished(STACK_ID, CLUSTER_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.AVAILABLE), eq("RDS upgrade finished"));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(AVAILABLE.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_FINISHED));
    }

    @Test
    public void testRdsUpgradeFailedWithException() {
        Exception exception = new RuntimeException("Backup for RDS upgrade has failed");

        underTest.rdsUpgradeFailed(STACK_ID, CLUSTER_ID, exception);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_FAILED),
                eq("RDS upgrade failed with exception: " + exception.getMessage()));
        verify(flowMessageService).fireEventAndLog(
                eq(STACK_ID),
                eq(UPDATE_FAILED.name()),
                eq(ResourceEvent.CLUSTER_RDS_UPGRADE_FAILED),
                eq("Backup for RDS upgrade has failed"));
    }

    @Test
    public void testBackupRds() throws CloudbreakOrchestratorException {
        underTest.backupRds(STACK_ID, BACKUP_LOCATION, BACKUP_INSTANCE_PROFILE);

        verify(rdsUpgradeOrchestratorService).backupRdsData(eq(STACK_ID), eq(BACKUP_LOCATION), eq(BACKUP_INSTANCE_PROFILE));
    }

    @Test
    public void testRestoreRds() throws CloudbreakOrchestratorException {
        StackDto stack = mock(StackDto.class);
        when(stackDtoService.getById(anyLong())).thenReturn(stack);

        underTest.restoreRds(STACK_ID, TARGET_VERSION);

        verify(rdsUpgradeOrchestratorService).checkRdsConnection(stack);
        verify(rdsUpgradeOrchestratorService).restoreRdsData(eq(stack), eq(TARGET_VERSION));
    }

    @Test
    public void testInstallPostgresPackages() throws CloudbreakOrchestratorException {
        underTest.installPostgresPackages(STACK_ID, MajorVersion.VERSION_14);

        verify(rdsUpgradeOrchestratorService).installPostgresPackages(eq(STACK_ID), eq(MajorVersion.VERSION_14));
        verify(rdsUpgradeOrchestratorService).updatePostgresAlternatives(eq(STACK_ID), eq(MajorVersion.VERSION_14));
    }

    @Test
    public void testShouldRunDataBackupRestore() {
        Database database = mock(Database.class);
        underTest.shouldRunDataBackupRestore(stackView, clusterView, database);

        verify(backupRestoreChecker).shouldRunDataBackupRestore(eq(stackView), eq(clusterView), eq(database));
    }

    @Test
    public void testBackupRdsState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_BACKUP_DATA.getMessage())).thenReturn(BACKUP_STATE);
        underTest.backupRdsState(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS), eq(BACKUP_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_BACKUP_DATA));
    }

    @Test
    public void testMigrateDatabaseSettingsState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_MIGRATE_DB_SETTINGS.getMessage())).thenReturn(MIGRATE_STATE);
        underTest.migrateDatabaseSettingsState(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS), eq(MIGRATE_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_MIGRATE_DB_SETTINGS));
    }

    @Test
    public void testMigrateServicesDatabaseSettingsState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_MIGRATE_SERVICES_DB_SETTINGS.getMessage())).thenReturn(MIGRATE_SERVICES_STATE);
        underTest.migrateServicesDatabaseSettingsState(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS), eq(MIGRATE_SERVICES_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(ResourceEvent.CLUSTER_RDS_UPGRADE_MIGRATE_SERVICES_DB_SETTINGS));
    }

    @Test
    public void testRestoreRdsState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_RESTORE_DATA.getMessage())).thenReturn(RESTORE_STATE);
        underTest.restoreRdsState(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS), eq(RESTORE_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_RESTORE_DATA));
    }

    @Test
    public void testUpgradeRdsState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_DBSERVER_UPGRADE.getMessage())).thenReturn(UPGRADE_STATE);
        underTest.upgradeRdsState(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS), eq(UPGRADE_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_DBSERVER_UPGRADE));
    }

    @Test
    public void testStopServicesState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_STOP_SERVICES.getMessage())).thenReturn(STOP_STATE);
        underTest.stopServicesState(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS), eq(STOP_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_STOP_SERVICES));
    }

    @Test
    public void testStartServicesState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_START_CMSERVICES.getMessage())).thenReturn(START_STATE);
        underTest.startCMServicesState(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS), eq(START_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_START_CMSERVICES));
    }

    @Test
    public void testStartClusterManagerState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_START_CLUSTERMANAGER.getMessage())).thenReturn(START_CM_STATE);
        underTest.startClusterManagerState(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS), eq(START_CM_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_START_CLUSTERMANAGER));
    }

    @Test
    public void testInstallPostgresPackagesState() {
        when(messagesService.getMessageWithArgs(ResourceEvent.CLUSTER_RDS_UPGRADE_INSTALL_PG.getMessage(), TARGET_VERSION)).thenReturn(INSTALL_PG_STATE);
        underTest.installPostgresPackagesState(STACK_ID, TARGET_VERSION);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS), eq(INSTALL_PG_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_INSTALL_PG),
                eq(TARGET_VERSION));
    }

}