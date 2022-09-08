package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
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

    private static final TargetMajorVersion TARGET_VERSION =  TargetMajorVersion.VERSION_11;

    private static final String WORKSPACE_NAME = "workspaceName";

    private static final String TENANT_NAME = "tenant";

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ReactorFlowManager reactorFlowManager;

    @Mock
    private DatabaseService databaseService;

    @InjectMocks
    private RdsUpgradeService underTest;

    @Test
    void testUpgradeRdsWithValidSetupThenSuccess() {
        Stack stack = createStack(Status.AVAILABLE);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_ID);
        when(reactorFlowManager.triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION))).thenReturn(flowId);

        RdsUpgradeV4Response response = underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verify(reactorFlowManager).triggerRdsUpgrade(eq(STACK_ID), eq(TARGET_VERSION));
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.FLOW_CHAIN);
        assertThat(response.getFlowIdentifier().getPollableId()).isEqualTo(FLOW_ID);
    }

    @Test
    void testWhenRdsAlreadyUpgradedThenError() {
        Stack stack = createStack(Status.AVAILABLE);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_11));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);

        RdsUpgradeV4Response response = underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verifyNoInteractions(reactorFlowManager);
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.NOT_TRIGGERED);
    }

    private StackDatabaseServerResponse createDatabaseServerResponse(MajorVersion majorVersion) {
        StackDatabaseServerResponse stackDatabaseServerResponse = new StackDatabaseServerResponse();
        stackDatabaseServerResponse.setMajorVersion(majorVersion);
        return stackDatabaseServerResponse;
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"AVAILABLE", "MAINTENANCE_MODE_ENABLED", "EXTERNAL_DATABASE_UPGRADE_FAILED"}, mode = EnumSource.Mode.EXCLUDE)
    void testWhenStackUnavailableThenError(Status status) {
        Stack stack = createStack(status);
        when(databaseService.getDatabaseServer(eq(STACK_NAME_OR_CRN))).thenReturn(createDatabaseServerResponse(MajorVersion.VERSION_10));
        when(stackDtoService.getStackViewByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);

        RdsUpgradeV4Response response = underTest.upgradeRds(NameOrCrn.ofCrn(STACK_CRN), TARGET_VERSION);

        verifyNoInteractions(reactorFlowManager);
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.NOT_TRIGGERED);
    }

    private Stack createStack(Status status) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setResourceCrn(STACK_CRN);
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