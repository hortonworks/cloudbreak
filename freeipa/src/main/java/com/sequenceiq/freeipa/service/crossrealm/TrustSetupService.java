package com.sequenceiq.freeipa.service.crossrealm;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.CancelCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.RepairCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.repair.event.FreeIpaTrustRepairEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.config.FreeIpaTrustSetupFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.config.FreeIpaTrustSetupFinishFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishEvent;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class TrustSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustSetupService.class);

    private static final Set<DetailedStackStatus> ENABLED_STATUSES_FOR_TRUST_SETUP_FINISH = Set.of(
            DetailedStackStatus.TRUST_SETUP_FINISH_REQUIRED,
            DetailedStackStatus.TRUST_SETUP_FINISH_FAILED,
            AVAILABLE
    );

    private static final Set<TrustStatus> ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_FINISH = Set.of(
            TrustStatus.TRUST_SETUP_FINISH_REQUIRED,
            TrustStatus.TRUST_SETUP_FINISH_FAILED
    );

    private static final Set<TrustStatus> ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_COMMANDS = Set.of(
            TrustStatus.TRUST_SETUP_FINISH_REQUIRED,
            TrustStatus.TRUST_SETUP_FINISH_IN_PROGRESS,
            TrustStatus.TRUST_SETUP_FINISH_FAILED,
            TrustStatus.TRUST_BROKEN,
            TrustStatus.TRUST_ACTIVE
    );

    private static final Set<TrustStatus> ENABLED_TRUSTSTATUSES_FOR_TRUST_REPAIR_COMMANDS = Set.of(
            TrustStatus.TRUST_SETUP_FINISH_FAILED,
            TrustStatus.TRUST_BROKEN,
            TrustStatus.TRUST_ACTIVE
    );

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private OperationToOperationStatusConverter operationConverter;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private TrustCommandsGeneratorService trustCommandsGeneratorService;

    @Inject
    private FlowCancelService flowCancelService;

    @Inject
    private FlowLogDBService flowLogDBService;

    public PrepareCrossRealmTrustResponse setupTrust(String accountId, PrepareCrossRealmTrustRequest request) {
        String environmentCrn = request.getEnvironmentCrn();
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);

        createOrUpdateCrossRealmConfigs(request, stack);

        Operation operation = operationService.startOperation(accountId, OperationType.TRUST_SETUP, Set.of(environmentCrn), Set.of());
        FreeIpaTrustSetupEvent freeIPATrustSetupEvent = new FreeIpaTrustSetupEvent(stack.getId(), operation.getOperationId());
        FlowIdentifier flowIdentifier = flowManager.notify(freeIPATrustSetupEvent.selector(), freeIPATrustSetupEvent);

        PrepareCrossRealmTrustResponse response = new PrepareCrossRealmTrustResponse();
        response.setFlowIdentifier(flowIdentifier);
        response.setOperationStatus(operationConverter.convert(operation));
        BeanUtils.copyProperties(request, response);

        return response;
    }

    private void createOrUpdateCrossRealmConfigs(PrepareCrossRealmTrustRequest request, Stack stack) {
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackIdIfExists(stack.getId())
                .orElse(new CrossRealmTrust());
        crossRealmTrust.setStack(stack);
        crossRealmTrust.setEnvironmentCrn(request.getEnvironmentCrn());
        crossRealmTrust.setRemoteEnvironmentCrn(request.getRemoteEnvironmentCrn());
        crossRealmTrust.setFqdn(request.getFqdn());
        crossRealmTrust.setIp(request.getIp());
        crossRealmTrust.setRealm(request.getRealm());
        if (StringUtils.isBlank(request.getTrustSecret())) {
            LOGGER.debug("Cross realm trust secret is not provided, generating a new one.");
            crossRealmTrust.setTrustSecret(PasswordUtil.generatePassword());
        } else {
            crossRealmTrust.setTrustSecret(request.getTrustSecret());
        }
        crossRealmTrust.setTrustStatus(TrustStatus.TRUST_SETUP_REQUIRED);
        crossRealmTrust = crossRealmTrustService.save(crossRealmTrust);
        LOGGER.debug("Saved cross-realm trust configuration: {}", crossRealmTrust);
    }

    public FinishSetupCrossRealmTrustResponse finishTrustSetup(String accountId, FinishSetupCrossRealmTrustRequest request) {
        String environmentCrn = request.getEnvironmentCrn();
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stack.getId());
        if (!isFinishTrustSetupPossible(stack, crossRealmTrust)) {
            throw new BadRequestException("FreeIPA stack is not in cross-realm trust set up pending state.");
        }

        Operation operation = operationService.startOperation(accountId, OperationType.TRUST_SETUP_FINISH, Set.of(environmentCrn), Set.of());
        FreeIpaTrustSetupFinishEvent freeIPATrustSetupFinishEvent = new FreeIpaTrustSetupFinishEvent(stack.getId(), operation.getOperationId());
        FlowIdentifier flowIdentifier = flowManager.notify(freeIPATrustSetupFinishEvent.selector(), freeIPATrustSetupFinishEvent);

        FinishSetupCrossRealmTrustResponse response = new FinishSetupCrossRealmTrustResponse();
        response.setFlowIdentifier(flowIdentifier);
        response.setOperationStatus(operationConverter.convert(operation));
        BeanUtils.copyProperties(request, response);

        return response;
    }

    public TrustSetupCommandsResponse getTrustCommands(String accountId, String environmentCrn, TrustCommandType trustCommandType) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stack.getId());
        if (!ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_COMMANDS.contains(crossRealmTrust.getTrustStatus())) {
            throw new BadRequestException(stack.getName() + " trust is not in state, where trust setup commands can be generated. " +
                    "Current state is " + crossRealmTrust.getTrustStatus() +
                    ", required states: " + ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_COMMANDS);
        }
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        return trustCommandsGeneratorService.getTrustCommands(trustCommandType, environmentCrn, stack, freeIpa, crossRealmTrust);
    }

    private boolean isFinishTrustSetupPossible(Stack stack, CrossRealmTrust crossRealmTrust) {
        return ENABLED_STATUSES_FOR_TRUST_SETUP_FINISH.contains(stack.getStackStatus().getDetailedStackStatus()) &&
                ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_FINISH.contains(crossRealmTrust.getTrustStatus());
    }

    public CancelCrossRealmTrustResponse breakDownTrust(String accountId, String environmentCrn) {
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stack.getId());

        if (isOperationStillRunning(accountId, crossRealmTrust)) {
            List<FlowLog> flowLogs = flowLogDBService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(stack.getId());
            if (!flowLogs.isEmpty() && (flowLogs.getFirst().isFlowType(FreeIpaTrustSetupFlowConfig.class)
                    || flowLogs.getFirst().isFlowType(FreeIpaTrustSetupFinishFlowConfig.class))) {
                LOGGER.info("Canceling running trust setup for {}", stack.getResourceCrn());
                flowCancelService.cancelFlowSilently(flowLogs.getFirst());
            } else {
                LOGGER.debug("No running Stack config update flow to cancel");
            }
        }

        Operation operation = operationService.startOperation(accountId, OperationType.CANCEL_TRUST_SETUP, Set.of(environmentCrn), Set.of());
        FreeIpaTrustCancelEvent freeIPATrustCancelEvent = new FreeIpaTrustCancelEvent(stack.getId(), operation.getOperationId());
        FlowIdentifier flowIdentifier = flowManager.notify(freeIPATrustCancelEvent.selector(), freeIPATrustCancelEvent);

        CancelCrossRealmTrustResponse response = new CancelCrossRealmTrustResponse();
        response.setFlowIdentifier(flowIdentifier);
        response.setOperationStatus(operationConverter.convert(operation));

        return response;
    }

    private boolean isOperationStillRunning(String accountId, CrossRealmTrust crossRealmTrust) {
        try {
            Operation operation = operationService.getOperationForAccountIdAndOperationId(accountId, crossRealmTrust.getOperationId());
            return OperationState.RUNNING.equals(operation.getStatus());
        } catch (NotFoundException e) {
            LOGGER.debug("No trust setup operation found");
            return false;
        }
    }

    public RepairCrossRealmTrustResponse repairTrust(String accountId, String environmentCrn) {
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stack.getId());
        if (!ENABLED_TRUSTSTATUSES_FOR_TRUST_REPAIR_COMMANDS.contains(crossRealmTrust.getTrustStatus())) {
            throw new BadRequestException("Trust can not be repaired until it was set up and finished");
        }

        Operation operation = operationService.startOperation(accountId, OperationType.REPAIR_TRUST_SETUP, Set.of(environmentCrn), Set.of());
        FreeIpaTrustRepairEvent trustSetupEvent = new FreeIpaTrustRepairEvent(stack.getId(), operation.getOperationId());
        FlowIdentifier flowIdentifier = flowManager.notify(trustSetupEvent.selector(), trustSetupEvent);

        RepairCrossRealmTrustResponse response = new RepairCrossRealmTrustResponse();
        response.setFlowIdentifier(flowIdentifier);
        response.setOperationStatus(operationConverter.convert(operation));

        return response;
    }
}
