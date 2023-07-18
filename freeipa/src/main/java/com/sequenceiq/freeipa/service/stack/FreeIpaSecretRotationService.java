package com.sequenceiq.freeipa.service.stack;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

@Service
public class FreeIpaSecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSecretRotationService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private EntitlementService entitlementService;

    public FlowIdentifier rotateSecretsByCrn(String accountId, String environmentCrn, FreeIpaSecretRotationRequest request) {
        LOGGER.info("Requested secret rotation. Account id: {}, environment crn: {}, request: {}", accountId, environmentCrn, request);
        if (!entitlementService.isSecretRotationEnabled(accountId)) {
            throw new BadRequestException("Account is not entitled to execute secret rotation.");
        }
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(request.getSecrets());
        String selector = EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class);
        return flowManager.notify(selector,
                new SecretRotationFlowChainTriggerEvent(selector, stack.getId(), stack.getEnvironmentCrn(), secretTypes, request.getExecutionType()));
    }
}
