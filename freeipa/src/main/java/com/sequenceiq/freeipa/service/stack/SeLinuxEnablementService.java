package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.SET_SELINUX_TO_ENFORCING_EVENT;

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

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.ModifySeLinuxResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxEvent;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class SeLinuxEnablementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeLinuxEnablementService.class);

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

    public void enableSeLinuxOnAllNodes(Stack stack) throws CloudbreakOrchestratorException {
        LOGGER.debug("Setting SeLinux to 'ENFORCING' on stack for FreeIpa - {}", stack.getResourceCrn());
        Set<InstanceMetaData> instanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfigForSalt(stack);
        Set<String> allHostNames = instanceMetaDataSet.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toCollection(HashSet::new));
        StackBasedExitCriteriaModel exitCriteriaModel = new StackBasedExitCriteriaModel(stack.getId());
        LOGGER.debug("SeLinuxEnablementService - calling hostOrchestrator for modifying SeLinux on stack for FreeIpa - {}", stack.getResourceCrn());
        hostOrchestrator.executeSaltState(primaryGatewayConfig, allHostNames, List.of("freeipa.selinux-mode"),
                exitCriteriaModel, Optional.of(SELINUX_ENFORCING_RETRY_ATTEMPTS), Optional.of(SELINUX_ENFORCING_RETRY_ATTEMPTS));
    }

    public ModifySeLinuxResponse setSeLinuxToEnforcingByCrn(String environmentCrn, String accountId) {
        LOGGER.debug("Starting flow for setting SeLinux to 'ENFORCING' on stack for FreeIpa - {}", environmentCrn);
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        Operation operation = operationService.startOperation(accountId, OperationType.MODIFY_SELINUX_MODE,
                Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        try {
            FlowIdentifier flowIdentifier = flowManager.notify(SET_SELINUX_TO_ENFORCING_EVENT.event(),
                    new FreeIpaEnableSeLinuxEvent(SET_SELINUX_TO_ENFORCING_EVENT.event(), stack.getId(),
                            operation.getOperationId()));
            return new ModifySeLinuxResponse(flowIdentifier);
        } catch (Exception e) {
            LOGGER.error("Couldn't start SELinux enablement flow", e);
            operationService.failOperation(accountId, operation.getOperationId(), "Couldn't start Freeipa SELinux enablement flow: " + e.getMessage());
            throw new CloudbreakServiceException("Couldn't start Freeipa SELinux enablement flow: " + e.getMessage() + ", exception : " + e);
        }
    }
}
