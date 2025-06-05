package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_UPGRADE_CCM_ALREADY_UPGRADED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_UPGRADE_CCM_ERROR_ENVIRONMENT_IS_NOT_LATEST;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_UPGRADE_CCM_NOT_AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_UPGRADE_CCM_NOT_UPGRADEABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class StackCcmUpgradeServiceTest {

    private static final Long CLUSTER_ID = 123L;

    private static final Long STACK_ID = 234L;

    private static final String ENV_CRN = "envCrn";

    private static final String STACK_CRN = "crn";

    private static final String FLOW_ID = "Mocked flowId";

    private static final String ERROR_REASON = "reason";

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackService stackService;

    @Mock
    private ReactorNotifier reactorNotifier;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @InjectMocks
    private StackCcmUpgradeService underTest;

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = {"CCM", "CCMV2"}, mode = Mode.INCLUDE)
    void testUpgradeCcm(Tunnel tunnel) {
        Stack stack = createStack(tunnel, Status.AVAILABLE);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.latestUpgradeTarget());
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(environment);
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_ID);
        when(reactorNotifier.notify(eq(STACK_ID), eq("UPGRADE_CCM_CHAIN_TRIGGER_EVENT"), any(Acceptable.class))).thenReturn(flowId);

        StackCcmUpgradeV4Response response = underTest.upgradeCcm(NameOrCrn.ofCrn(STACK_CRN));

        ArgumentCaptor<UpgradeCcmFlowChainTriggerEvent> requestCaptor = ArgumentCaptor.forClass(UpgradeCcmFlowChainTriggerEvent.class);
        verify(reactorNotifier).notify(eq(STACK_ID), eq("UPGRADE_CCM_CHAIN_TRIGGER_EVENT"), requestCaptor.capture());
        UpgradeCcmFlowChainTriggerEvent request = requestCaptor.getValue();
        assertThat(request.getResourceId()).isEqualTo(STACK_ID);
        assertThat(request.getClusterId()).isEqualTo(CLUSTER_ID);
        assertThat(request.getOldTunnel()).isEqualTo(tunnel);
        assertThat(response.getResponseType()).isEqualTo(CcmUpgradeResponseType.TRIGGERED);
        assertThat(response.getResourceCrn()).isEqualTo(STACK_CRN);
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.FLOW_CHAIN);
        assertThat(response.getFlowIdentifier().getPollableId()).isEqualTo(FLOW_ID);
    }

    @Test
    void environmentNotLatest() {
        Stack stack = createStack(Tunnel.CCM, Status.AVAILABLE);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.DIRECT);
        environment.setCrn(ENV_CRN);
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(environment);
        when(messagesService.getMessage(DATAHUB_UPGRADE_CCM_ERROR_ENVIRONMENT_IS_NOT_LATEST.getMessage(), List.of(ENV_CRN))).thenReturn(ERROR_REASON);

        assertThatThrownBy(() -> underTest.upgradeCcm(NameOrCrn.ofCrn(STACK_CRN)))
                .hasMessage(ERROR_REASON)
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(reactorNotifier);
    }

    @Test
    void testAlreadyUpgraded() {
        Stack stack = createStack(Tunnel.latestUpgradeTarget(), Status.AVAILABLE);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.latestUpgradeTarget());
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(environment);
        when(messagesService.getMessage(DATAHUB_UPGRADE_CCM_ALREADY_UPGRADED.getMessage())).thenReturn(ERROR_REASON);

        StackCcmUpgradeV4Response response = underTest.upgradeCcm(NameOrCrn.ofCrn(STACK_CRN));

        verifyNoInteractions(reactorNotifier);
        assertThat(response.getResponseType()).isEqualTo(CcmUpgradeResponseType.SKIP);
        assertThat(response.getResourceCrn()).isEqualTo(STACK_CRN);
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
        assertThat(response.getReason()).isEqualTo(ERROR_REASON);
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.NOT_TRIGGERED);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"AVAILABLE", "MAINTENANCE_MODE_ENABLED", "UPGRADE_CCM_FAILED"}, mode = Mode.EXCLUDE)
    void testStackUnavailable(Status status) {
        Stack stack = createStack(Tunnel.CCM, status);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.latestUpgradeTarget());
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(environment);
        when(messagesService.getMessage(DATAHUB_UPGRADE_CCM_NOT_AVAILABLE.getMessage())).thenReturn(ERROR_REASON);

        StackCcmUpgradeV4Response response = underTest.upgradeCcm(NameOrCrn.ofCrn(STACK_CRN));

        verifyNoInteractions(reactorNotifier);
        assertThat(response.getResponseType()).isEqualTo(CcmUpgradeResponseType.ERROR);
        assertThat(response.getResourceCrn()).isEqualTo(STACK_CRN);
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
        assertThat(response.getReason()).isEqualTo(ERROR_REASON);
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.NOT_TRIGGERED);
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = {"CCM", "CCMV2", "CCMV2_JUMPGATE"}, mode = Mode.EXCLUDE)
    void testWrongOldTunnel(Tunnel tunnel) {
        Stack stack = createStack(tunnel, Status.AVAILABLE);
        DetailedEnvironmentResponse environment = createEnvironment(Tunnel.latestUpgradeTarget());
        when(stackService.getByNameOrCrnInWorkspace(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stack);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(environment);
        when(messagesService.getMessage(DATAHUB_UPGRADE_CCM_NOT_UPGRADEABLE.getMessage())).thenReturn(ERROR_REASON);

        StackCcmUpgradeV4Response response = underTest.upgradeCcm(NameOrCrn.ofCrn(STACK_CRN));

        verifyNoInteractions(reactorNotifier);
        assertThat(response.getResponseType()).isEqualTo(CcmUpgradeResponseType.ERROR);
        assertThat(response.getResourceCrn()).isEqualTo(STACK_CRN);
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
        assertThat(response.getReason()).isEqualTo(ERROR_REASON);
        assertThat(response.getFlowIdentifier().getType()).isEqualTo(FlowType.NOT_TRIGGERED);
    }

    private DetailedEnvironmentResponse createEnvironment(Tunnel tunnel) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setTunnel(tunnel);
        return environment;
    }

    private Stack createStack(Tunnel tunnel, Status status) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setTunnel(tunnel);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setResourceCrn(STACK_CRN);
        stack.setStackStatus(new StackStatus(stack, status, null, null));
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        return stack;
    }
}
