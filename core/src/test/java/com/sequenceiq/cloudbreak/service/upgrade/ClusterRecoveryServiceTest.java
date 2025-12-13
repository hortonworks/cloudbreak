package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;

@ExtendWith(MockitoExtension.class)
class ClusterRecoveryServiceTest {

    private static final Stack STACK = TestUtil.stack();

    private static final long WORKSPACE_ID = 0L;

    private static final String STACK_NAME = STACK.getName();

    @Mock
    private StackService stackService;

    @Mock
    private StackStatusService stackStatusService;

    @Mock
    private FreeipaService freeipaService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ReactorFlowManager flowManager;

    @InjectMocks
    private ClusterRecoveryService underTest;

    @MethodSource("data")
    @ParameterizedTest
    void testIfClusterRecoverable(List<StackStatus> stackStatusList, boolean freeIpaStatus, RecoveryStatus expectedRecoveryStatus, String expectedMessage) {
        NameOrCrn stackNameOrCrn = NameOrCrn.ofName(STACK_NAME);

        when(stackService.getByNameOrCrnInWorkspace(stackNameOrCrn, WORKSPACE_ID)).thenReturn(STACK);
        when(stackStatusService.findAllStackStatusesById(STACK.getId())).thenReturn(stackStatusList);
        when(freeipaService.checkFreeipaRunning(STACK.getEnvironmentCrn(), STACK_NAME)).thenReturn(freeIpaStatus);

        RecoveryValidationV4Response response = underTest.validateRecovery(WORKSPACE_ID, stackNameOrCrn);

        assertEquals(expectedRecoveryStatus, response.getStatus());
        assertEquals(expectedMessage, response.getReason());
    }

    @Test
    void testRestorePreviousImageVersion() throws Exception {
        NameOrCrn stackNameOrCrn = NameOrCrn.ofName(STACK.getName());
        when(stackService.getByNameOrCrnInWorkspace(stackNameOrCrn, WORKSPACE_ID)).thenReturn(STACK);
        Component component = new Component();
        String imageId = "image-id";
        component.setAttributes(new Json(Image.builder().withImageId(imageId).build()));
        when(componentConfigProviderService.getImageComponent(STACK.getId())).thenReturn(component);
        underTest.recoverCluster(WORKSPACE_ID, stackNameOrCrn);
        verify(componentConfigProviderService).restoreSecondToLastVersion(component);
        verify(flowManager).triggerDatalakeClusterRecovery(STACK.getId());
    }

    static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE)),
                        true,
                        RecoveryStatus.NON_RECOVERABLE,
                        "There has been no failed upgrades for this cluster hence recovery is not permitted."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_UPGRADE_FAILED)),
                        true,
                        RecoveryStatus.RECOVERABLE,
                        "Last cluster upgrade has failed, recovery can be launched to restore the cluster to its pre-upgrade state."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_UPGRADE_FAILED),
                        getStackStatus(DetailedStackStatus.CLUSTER_RECOVERY_FAILED)),
                        true,
                        RecoveryStatus.RECOVERABLE,
                        "Last cluster recovery has failed, recovery can be retried."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_RECOVERY_FAILED)),
                        true,
                        RecoveryStatus.RECOVERABLE,
                        "Last cluster recovery has failed, recovery can be retried."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_RECOVERY_FAILED),
                        getStackStatus(DetailedStackStatus.CLUSTER_RECOVERY_FINISHED),
                        getStackStatus(DetailedStackStatus.AVAILABLE)),
                        true,
                        RecoveryStatus.NON_RECOVERABLE,
                        "Cluster is not in a recoverable state now, neither uncorrected upgrade or recovery failures are present."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_UPGRADE_FAILED),
                        getStackStatus(DetailedStackStatus.CLUSTER_UPGRADE_FINISHED),
                        getStackStatus(DetailedStackStatus.AVAILABLE)),
                        true,
                        RecoveryStatus.NON_RECOVERABLE,
                        "Cluster is not in a recoverable state now, neither uncorrected upgrade or recovery failures are present."},


                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE)),
                        false,
                        RecoveryStatus.NON_RECOVERABLE,
                        "Recovery cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state and try again."
                                + " Next issue: There has been no failed upgrades for this cluster hence recovery is not permitted."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_UPGRADE_FAILED)),
                        false,
                        RecoveryStatus.NON_RECOVERABLE,
                        "Recovery cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state and try again."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_UPGRADE_FAILED),
                        getStackStatus(DetailedStackStatus.CLUSTER_RECOVERY_FAILED)),
                        false,
                        RecoveryStatus.NON_RECOVERABLE,
                        "Recovery cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state and try again."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_RECOVERY_FAILED)),
                        false,
                        RecoveryStatus.NON_RECOVERABLE,
                        "Recovery cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state and try again."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_RECOVERY_FAILED),
                        getStackStatus(DetailedStackStatus.CLUSTER_RECOVERY_FINISHED),
                        getStackStatus(DetailedStackStatus.AVAILABLE)),
                        false,
                        RecoveryStatus.NON_RECOVERABLE,
                        "Recovery cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state and try again. "
                                + "Next issue: Cluster is not in a recoverable state now, neither uncorrected upgrade or recovery failures are present."},
                {List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE),
                        getStackStatus(DetailedStackStatus.CLUSTER_UPGRADE_FAILED),
                        getStackStatus(DetailedStackStatus.CLUSTER_UPGRADE_FINISHED),
                        getStackStatus(DetailedStackStatus.AVAILABLE)),
                        false,
                        RecoveryStatus.NON_RECOVERABLE,
                        "Recovery cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state and try again. "
                                + "Next issue: Cluster is not in a recoverable state now, neither uncorrected upgrade or recovery failures are present."},

        });
    }

    private static StackStatus getStackStatus(DetailedStackStatus detailedStackStatus) {
        return new StackStatus(STACK, detailedStackStatus);
    }

}