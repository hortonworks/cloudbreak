package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RDS_UPGRADE_ALREADY_UPGRADED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RDS_UPGRADE_NOT_AVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class RdsUpgradeServiceTest {

    private static final Long CLUSTER_ID = 123L;

    private static final Long STACK_ID = 234L;

    private static final String ENV_CRN = "envCrn";

    private static final String STACK_CRN = "crn";

    private static final NameOrCrn STACK_NAME_OR_CRN = NameOrCrn.ofCrn(STACK_CRN);

    private static final String FLOW_ID = "Mocked flowId";

    private static final String ERROR_REASON = "reason";

    private static final TargetMajorVersion TARGET_VERSION = TargetMajorVersion.VERSION_11;

    private static final String WORKSPACE_NAME = "workspaceName";

    private static final String TENANT_NAME = "tenant";

    private static final String STACK_VERSION = "7.2.10";

    private static final String ACCOUNT_ID = "accountId";

    private static final String BACKUP_LOCATION = "location";

    @Mock
    private EnvironmentClientService environmentService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ReactorFlowManager reactorFlowManager;

    @Mock
    private DatabaseService databaseService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private DatabaseUpgradeRuntimeValidator databaseUpgradeRuntimeValidator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private RdsUpgradeService underTest;

    @BeforeEach
    void setup() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        BackupResponse backupResponse = new BackupResponse();
        backupResponse.setStorageLocation(BACKUP_LOCATION);
        environmentResponse.setBackup(backupResponse);
        lenient().when(environmentService.getByCrn(anyString())).thenReturn(environmentResponse);
    }

    @Test
    void testUpgradeRdsWithValidSetupThenSuccess() {
        Stack stack = createStack(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_ID);
        when(databaseUpgradeRuntimeValidator.validateRuntimeVersionForUpgrade(STACK_VERSION, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(reactorFlowManager.triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(BACKUP_LOCATION))).thenReturn(flowId);
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);

        RdsUpgradeV4Response response = underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verify(reactorFlowManager).triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(BACKUP_LOCATION));
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.FLOW_CHAIN);
        assertThat(response.getFlowIdentifier().getPollableId()).isEqualTo(FLOW_ID);
    }

    @Test
    void testUpgradeRdsWithValidSetupWithoutBackupLocationThenSuccess() {

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        when(environmentService.getByCrn(anyString())).thenReturn(environmentResponse);
        Stack stack = createStack(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_ID);
        when(databaseUpgradeRuntimeValidator.validateRuntimeVersionForUpgrade(STACK_VERSION, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(reactorFlowManager.triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(null))).thenReturn(flowId);
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);

        RdsUpgradeV4Response response = underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verify(reactorFlowManager).triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(null));
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.FLOW_CHAIN);
        assertThat(response.getFlowIdentifier().getPollableId()).isEqualTo(FLOW_ID);
    }

    @Test
    void testUpgradeRdsWithMissingTargetVersionThenSuccess() {
        Field defaultTargetMajorVersion = ReflectionUtils.findField(RdsUpgradeService.class, "defaultTargetMajorVersion");
        ReflectionUtils.makeAccessible(defaultTargetMajorVersion);
        ReflectionUtils.setField(defaultTargetMajorVersion, underTest, TARGET_VERSION);

        Stack stack = createStack(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(databaseUpgradeRuntimeValidator.validateRuntimeVersionForUpgrade(STACK_VERSION, ACCOUNT_ID)).thenReturn(Optional.empty());
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_ID);
        when(reactorFlowManager.triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(BACKUP_LOCATION))).thenReturn(flowId);
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);

        RdsUpgradeV4Response response = underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), null);

        verify(reactorFlowManager).triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(BACKUP_LOCATION));
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.FLOW_CHAIN);
        assertThat(response.getFlowIdentifier().getPollableId()).isEqualTo(FLOW_ID);
    }

    @Test
    void testUpgradeRdsWithValidSetupAndMissingDatabaseVersionThenSuccess() {
        Stack stack = createStack(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(null));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_ID);
        when(databaseUpgradeRuntimeValidator.validateRuntimeVersionForUpgrade(STACK_VERSION, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(reactorFlowManager.triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(BACKUP_LOCATION))).thenReturn(flowId);
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);

        RdsUpgradeV4Response response = underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verify(reactorFlowManager).triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(BACKUP_LOCATION));
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.FLOW_CHAIN);
        assertThat(response.getFlowIdentifier().getPollableId()).isEqualTo(FLOW_ID);
    }

    @Test
    void testWhenRdsAlreadyUpgradedThenError() {
        Stack stack = createStack(Status.AVAILABLE);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_11));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(messagesService.getMessage(CLUSTER_RDS_UPGRADE_ALREADY_UPGRADED.getMessage(),
                List.of(MajorVersion.VERSION_11.getMajorVersion()))).thenReturn(ERROR_REASON);

        Assertions.assertThatCode(() -> underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ERROR_REASON);

        verifyNoInteractions(reactorFlowManager);
    }

    @Test
    void testUpgradeRdsWithInvalidRuntimeVersionThenError() {
        Stack stack = createStack(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        String errorMessage = "Runtime version is not valid.";
        when(databaseUpgradeRuntimeValidator.validateRuntimeVersionForUpgrade(STACK_VERSION, ACCOUNT_ID)).thenReturn(Optional.of(errorMessage));

        Assertions.assertThatCode(() -> underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Runtime version is not valid.");

        verifyNoInteractions(reactorFlowManager);
    }

    @Test
    void testCheckUpgradeRdsOnDatahubWithValidSetup() {
        Stack stack = createStack(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);

        underTest.checkUpgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verify(stackService, never()).getByWorkspaceId(anyLong(), anyString(), anyList());
    }

    @Test
    void testCheckUpgradeRdsOnDatalakeWithRunningAttachedDatahubs() {
        Stack stack = createStack(Status.AVAILABLE);
        stack.setType(StackType.DATALAKE);
        StackListItem datahub = mock(StackListItem.class);
        when(datahub.getStackStatus()).thenReturn(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(1L);
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);
        when(stackService.getByWorkspaceId(1L, stack.getEnvironmentCrn(), List.of(StackType.WORKLOAD))).thenReturn(Set.of(datahub));

        Assertions.assertThatCode(() -> underTest.checkUpgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("There are attached Data Hub clusters in incorrect state");
    }

    @Test
    void testCheckUpgradeRdsOnDatalakeWithStoppedDatahubs() {
        Stack stack = createStack(Status.AVAILABLE);
        stack.setType(StackType.DATALAKE);
        StackListItem datahub = mock(StackListItem.class);
        when(datahub.getStackStatus()).thenReturn(Status.STOPPED);
        when(datahub.getClusterStatus()).thenReturn(Status.STOPPED);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(1L);
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);
        when(stackService.getByWorkspaceId(1L, stack.getEnvironmentCrn(), List.of(StackType.WORKLOAD))).thenReturn(Set.of(datahub));

        underTest.checkUpgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verify(stackService).getByWorkspaceId(1L, stack.getEnvironmentCrn(), List.of(StackType.WORKLOAD));
    }

    @Test
    void testCheckUpgradeRdsOnDatalakeWithStoppedDatahubsAndEntitlementEnabled() {
        Stack stack = createStack(Status.AVAILABLE);
        stack.setType(StackType.DATALAKE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(true);

        underTest.checkUpgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verify(stackService, never()).getByWorkspaceId(1L, stack.getEnvironmentCrn(), List.of(StackType.WORKLOAD));
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"AVAILABLE", "MAINTENANCE_MODE_ENABLED", "EXTERNAL_DATABASE_UPGRADE_FAILED"}, mode = EnumSource.Mode.EXCLUDE)
    void testWhenStackUnavailableThenError(Status status) {
        Stack stack = createStack(status);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(messagesService.getMessage(CLUSTER_RDS_UPGRADE_NOT_AVAILABLE.getMessage(), List.of(status.name()))).thenReturn(ERROR_REASON);

        Assertions.assertThatCode(() -> underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ERROR_REASON);

        verifyNoInteractions(reactorFlowManager);
    }

    @ParameterizedTest
    @EnumSource(value = DatabaseServerStatus.class, names = {"AVAILABLE", "UPGRADE_FAILED"})
    void testUpgradeRdsWithValidDatabaseStuatusThenSuccess(DatabaseServerStatus status) {
        Stack stack = createStack(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10, status));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_ID);
        when(databaseUpgradeRuntimeValidator.validateRuntimeVersionForUpgrade(STACK_VERSION, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(reactorFlowManager.triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(BACKUP_LOCATION))).thenReturn(flowId);
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);

        RdsUpgradeV4Response response = underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verify(reactorFlowManager).triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION), eq(BACKUP_LOCATION));
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.FLOW_CHAIN);
        assertThat(response.getFlowIdentifier().getPollableId()).isEqualTo(FLOW_ID);
    }

    @Test
    void testUpgradeRdsWithDatabaseNullStatusThenError() {
        Stack stack = createStack(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10, null));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(databaseUpgradeRuntimeValidator.validateRuntimeVersionForUpgrade(STACK_VERSION, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);

        Assertions.assertThatCode(() -> underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Upgrading database server is not possible as database server is not available.");

        verifyNoInteractions(reactorFlowManager);
    }

    @ParameterizedTest
    @EnumSource(value = DatabaseServerStatus.class, names = {"AVAILABLE", "UPGRADE_FAILED"}, mode = EnumSource.Mode.EXCLUDE)
    void testUpgradeRdsWithDatabaseNotAvailableThenError(DatabaseServerStatus status) {
        Stack stack = createStack(Status.AVAILABLE);
        when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10, status));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(databaseUpgradeRuntimeValidator.validateRuntimeVersionForUpgrade(STACK_VERSION, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(false);

        Assertions.assertThatCode(() -> underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Upgrading database server is not possible as database server is not available, it is in %s state.", status));

        verifyNoInteractions(reactorFlowManager);
    }

    private StackDatabaseServerResponse createDatabaseServerResponse(MajorVersion majorVersion) {
        return createDatabaseServerResponse(majorVersion, DatabaseServerStatus.AVAILABLE);
    }

    private StackDatabaseServerResponse createDatabaseServerResponse(MajorVersion majorVersion, DatabaseServerStatus status) {
        StackDatabaseServerResponse stackDatabaseServerResponse = new StackDatabaseServerResponse();
        stackDatabaseServerResponse.setMajorVersion(majorVersion);
        stackDatabaseServerResponse.setStatus(status);
        return stackDatabaseServerResponse;
    }

    private Stack createStack(Status status) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setResourceCrn(STACK_CRN);
        stack.setStackVersion(STACK_VERSION);
        Workspace workspace = new Workspace();
        workspace.setName(WORKSPACE_NAME);
        Tenant tenant = new Tenant();
        tenant.setName(TENANT_NAME);
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);
        stack.setStackStatus(new StackStatus(stack, status, null, null));
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        return stack;
    }
}