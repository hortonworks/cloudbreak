package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus.NON_RECOVERABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus.RECOVERABLE;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class ClusterRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRecoveryService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private StackStatusService stackStatusService;

    @Inject
    private FreeipaService freeipaService;

    public FlowIdentifier recoverCluster(Long workspaceId, NameOrCrn stackNameOrCrn) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(stackNameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Recovery has been initiated for stack {}", stackNameOrCrn.getNameOrCrn());
        return flowManager.triggerDatalakeClusterRecovery(stack.getId());
    }

    public RecoveryValidationV4Response validateRecovery(Long workspaceId, NameOrCrn stackNameOrCrn) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(stackNameOrCrn, workspaceId);
        return validateFreeIpaStatus(stack.getEnvironmentCrn(), stack.getName()).merge(validateStackStatus(stack));
    }

    private RecoveryValidationV4Response validateStackStatus(Stack stack) {
        List<StackStatus> statusList = stackStatusService.findAllStackStatusesById(stack.getId());
        List<DetailedStackStatus> detailedStackStatusList = statusList.stream()
                .map(StackStatus::getDetailedStackStatus)
                .collect(Collectors.toList());
        int lastRecoverySuccess = getLastRecoverySuccess(detailedStackStatusList);
        int lastRecoveryFailure = getLastRecoveryFailure(detailedStackStatusList);
        int lastUpgradeSuccess = getLastUpgradeSuccess(detailedStackStatusList);
        int lastUpgradeFailure = getLastUpgradeFailure(detailedStackStatusList);
        String logMessage =
                Stream.of(createLogEntry(lastUpgradeSuccess, DetailedStackStatus.CLUSTER_UPGRADE_FINISHED),
                                createLogEntry(lastUpgradeFailure, DetailedStackStatus.CLUSTER_UPGRADE_FAILED),
                                createLogEntry(lastRecoverySuccess, DetailedStackStatus.CLUSTER_RECOVERY_FINISHED),
                                createLogEntry(lastRecoveryFailure, DetailedStackStatus.CLUSTER_RECOVERY_FAILED))
                        .flatMap(Optional::stream)
                        .collect(Collectors.joining(". "));
        LOGGER.debug(logMessage);

        int maximumInt = IntStream.of(lastUpgradeFailure, lastRecoveryFailure, lastRecoverySuccess, lastUpgradeSuccess)
                .max()
                .getAsInt();
        String reason;
        RecoveryStatus status;
        if (maximumInt == -1) {
            reason = "There has been no failed upgrades for this cluster hence recovery is not permitted.";
            status = NON_RECOVERABLE;
        } else if (maximumInt == lastRecoveryFailure) {
            reason = "Last cluster recovery has failed, recovery can be retried.";
            status = RECOVERABLE;
        } else if (maximumInt == lastUpgradeFailure) {
            reason = "Last cluster upgrade has failed, recovery can be launched to restore the cluster to its pre-upgrade state.";
            status = RECOVERABLE;
        } else {
            reason = "Cluster is not in a recoverable state now, neither uncorrected upgrade or recovery failures are present.";
            status = NON_RECOVERABLE;
        }
        LOGGER.info(reason);
        return new RecoveryValidationV4Response(reason, status);
    }

    private RecoveryValidationV4Response validateFreeIpaStatus(String envCrn, String stackName) {
        boolean freeIpaAvailable = freeipaService.checkFreeipaRunning(envCrn, stackName);
        String reason = "";
        RecoveryStatus status;
        if (!freeIpaAvailable) {
            reason = "Recovery cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state and try again.";
            status = NON_RECOVERABLE;
            LOGGER.info(reason);
        } else {
            status = RECOVERABLE;
            LOGGER.debug("Recovery can be performed because the FreeIPA is available.");
        }
        return new RecoveryValidationV4Response(reason, status);
    }

    private int getLastUpgradeSuccess(List<DetailedStackStatus> detailedStackStatusList) {
        return getLastStatusFromListByMultipleStatus(detailedStackStatusList,
                List.of(DetailedStackStatus.CLUSTER_UPGRADE_FINISHED, DetailedStackStatus.CLUSTER_ROLLING_UPGRADE_FINISHED));
    }

    private int getLastRecoverySuccess(List<DetailedStackStatus> detailedStackStatusList) {
        return detailedStackStatusList.lastIndexOf(DetailedStackStatus.CLUSTER_RECOVERY_FINISHED);
    }

    private int getLastRecoveryFailure(List<DetailedStackStatus> detailedStackStatusList) {
        return detailedStackStatusList.lastIndexOf(DetailedStackStatus.CLUSTER_RECOVERY_FAILED);
    }

    private int getLastUpgradeFailure(List<DetailedStackStatus> detailedStackStatusList) {
        return getLastStatusFromListByMultipleStatus(detailedStackStatusList, DetailedStackStatus.getUpgradeFailureStatuses());
    }

    private int getLastStatusFromListByMultipleStatus(List<DetailedStackStatus> detailedStackStatusList, List<DetailedStackStatus> expectedStatusList) {
        return Collections.max(expectedStatusList
                .stream()
                .map(detailedStackStatusList::lastIndexOf)
                .toList());
    }

    private Optional<String> createLogEntry(int index, DetailedStackStatus status) {
        if (index == -1) {
            return Optional.empty();
        }
        return Optional.of(String.format("Last index for status %s is %d", status.name(), index));
    }
}
