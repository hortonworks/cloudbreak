package com.sequenceiq.freeipa.service.crossrealm;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_REQUIRED;

import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupEvent;
import com.sequenceiq.freeipa.repository.CrossRealmTrustRepository;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class TrustSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustSetupService.class);

    private static final Set<DetailedStackStatus> ENABLED_STATUSES_FOR_TRUST_SETUP_FINISH = Set.of(TRUST_SETUP_FINISH_REQUIRED,
            TRUST_SETUP_FINISH_FAILED, AVAILABLE);

    private static final Set<TrustStatus> ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_FINISH = Set.of(TrustStatus.TRUST_SETUP_FINISH_REQUIRED,
            TrustStatus.TRUST_SETUP_FINISH_FAILED);

    private static final Set<TrustStatus> ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_COMMANDS = Set.of(TrustStatus.TRUST_SETUP_FINISH_REQUIRED,
            TrustStatus.TRUST_SETUP_FINISH_IN_PROGRESS, TrustStatus.TRUST_SETUP_FINISH_FAILED, TrustStatus.TRUST_ACTIVE);

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
    private CrossRealmTrustRepository crossRealmTrustRepository;

    @Inject
    private TrustCommandsGeneratorService trustCommandsGeneratorService;

    public PrepareCrossRealmTrustResponse setupTrust(String accountId, PrepareCrossRealmTrustRequest request) {
        String environmentCrn = request.getEnvironmentCrn();
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);

        createOrUpdateCrossRealmConfigs(request, stack);

        Operation operation = operationService.startOperation(accountId, OperationType.TRUST_SETUP, Set.of(environmentCrn), Set.of());
        TrustSetupEvent trustSetupEvent = new TrustSetupEvent(stack.getId(), operation.getOperationId());
        FlowIdentifier flowIdentifier = flowManager.notify(trustSetupEvent.selector(), trustSetupEvent);

        PrepareCrossRealmTrustResponse response = new PrepareCrossRealmTrustResponse();
        response.setFlowIdentifier(flowIdentifier);
        response.setOperationStatus(operationConverter.convert(operation));
        BeanUtils.copyProperties(request, response);

        return response;
    }

    private void createOrUpdateCrossRealmConfigs(PrepareCrossRealmTrustRequest request, Stack stack) {
        CrossRealmTrust crossRealmTrust = crossRealmTrustRepository.findByStackId(stack.getId())
                .orElse(new CrossRealmTrust());
        crossRealmTrust.setStack(stack);
        crossRealmTrust.setEnvironmentCrn(request.getEnvironmentCrn());
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
        crossRealmTrust = crossRealmTrustRepository.save(crossRealmTrust);
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
        FinishTrustSetupEvent finishTrustSetupEvent = new FinishTrustSetupEvent(stack.getId(), operation.getOperationId());
        FlowIdentifier flowIdentifier = flowManager.notify(finishTrustSetupEvent.selector(), finishTrustSetupEvent);

        FinishSetupCrossRealmTrustResponse response = new FinishSetupCrossRealmTrustResponse();
        response.setFlowIdentifier(flowIdentifier);
        response.setOperationStatus(operationConverter.convert(operation));
        BeanUtils.copyProperties(request, response);

        return response;
    }

    public TrustSetupCommandsResponse getTrustSetupCommands(String accountId, TrustSetupCommandsRequest request) {
        String environmentCrn = request.getEnvironmentCrn();
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stack.getId());
        if (!ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_COMMANDS.contains(crossRealmTrust.getTrustStatus())) {
            throw new BadRequestException(stack.getName() + " trust is not in state, where trust setup commands can be generated. " +
                    "Current state is " + stack.getStackStatus().getDetailedStackStatus() +
                    ", required states: " + ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_COMMANDS);
        }
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        return trustCommandsGeneratorService.getTrustSetupCommands(request, stack, freeIpa, crossRealmTrust);
    }

    private boolean isFinishTrustSetupPossible(Stack stack, CrossRealmTrust crossRealmTrust) {
        return ENABLED_STATUSES_FOR_TRUST_SETUP_FINISH.contains(stack.getStackStatus().getDetailedStackStatus()) &&
                ENABLED_TRUSTSTATUSES_FOR_TRUST_SETUP_FINISH.contains(crossRealmTrust.getTrustStatus());
    }
}
