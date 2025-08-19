package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.MODIFY_SELINUX_START_EVENT;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.ModifySeLinuxResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxEvent;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.validation.SeLinuxValidationService;

@Service
public class SeLinuxModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeLinuxModificationService.class);

    private static final int SELINUX_ENFORCING_RETRY_ATTEMPTS = 5;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackService stackService;

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private SeLinuxValidationService seLinuxValidationService;

    public void modifySeLinuxOnAllNodes(Stack stack) throws CloudbreakOrchestratorException {
        LOGGER.debug("Modifying SeLinux to {} on stack for FreeIpa - {}", stack.getResourceCrn());
        Set<InstanceMetaData> instanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfigForSalt(stack);
        Set<String> allHostNames = instanceMetaDataSet.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toCollection(HashSet::new));
        StackBasedExitCriteriaModel exitCriteriaModel = new StackBasedExitCriteriaModel(stack.getId());
        LOGGER.debug("Calling hostOrchestrator for modifying SeLinux on stack for FreeIpa - {}", stack.getResourceCrn());
        hostOrchestrator.executeSaltState(primaryGatewayConfig, allHostNames, List.of("freeipa.selinux-mode"),
                exitCriteriaModel, Optional.of(SELINUX_ENFORCING_RETRY_ATTEMPTS), Optional.of(SELINUX_ENFORCING_RETRY_ATTEMPTS));
    }

    public ModifySeLinuxResponse modifySeLinuxByCrn(String environmentCrn, String accountId, SeLinux selinuxMode) {
        validateSelinux(selinuxMode);
        LOGGER.debug("Starting flow for modifying SeLinux to {} on stack for FreeIpa - {}", selinuxMode, environmentCrn);
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        Operation operation = operationService.startOperation(accountId, OperationType.MODIFY_SELINUX_MODE,
                Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        try {
            FlowIdentifier flowIdentifier = flowManager.notify(MODIFY_SELINUX_START_EVENT.event(),
                    new FreeIpaModifySeLinuxEvent(MODIFY_SELINUX_START_EVENT.event(), stack.getId(),
                            operation.getOperationId(), selinuxMode));
            return new ModifySeLinuxResponse(flowIdentifier);
        } catch (Exception e) {
            LOGGER.error("Couldn't start SELinux enablement flow", e);
            operationService.failOperation(accountId, operation.getOperationId(), "Couldn't start Freeipa SELinux enablement flow: " + e.getMessage());
            throw new CloudbreakServiceException("Couldn't start Freeipa SELinux enablement flow: " + e.getMessage() + ", exception : " + e);
        }
    }

    private void validateSelinux(SeLinux targetSelinuxMode) {
        try {
            seLinuxValidationService.validateSeLinuxEntitlementGranted(targetSelinuxMode);
        } catch (CloudbreakServiceException e) {
            throw new BadRequestException(e);
        }
    }
}
