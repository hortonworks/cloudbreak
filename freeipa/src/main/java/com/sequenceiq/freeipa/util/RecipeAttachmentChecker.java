package com.sequenceiq.freeipa.util;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class RecipeAttachmentChecker extends AvailabilityChecker {

    private static final int CONNECT_TIMEOUT_MS = 5_000;

    private static final int READ_TIMEOUT_MS = 15_000;

    // feature supported from 2.59
    private static final Versioned FMS_RECIPES_AFTER_VERSION = () -> "2.59.0-b21";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    public boolean isRecipeAttachmentAvailable(long stackId) {
        Stack stack = stackService.getStackById(stackId);
        boolean newFreeipaClusterWithRecipeStates = isAvailable(stack, FMS_RECIPES_AFTER_VERSION);
        if (!newFreeipaClusterWithRecipeStates) {
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            if (stack.isAvailable()) {
                try {
                    return hostOrchestrator.doesPhaseSlsExistWithTimeouts(gatewayConfig, "recipes.runner", CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
                } catch (CloudbreakOrchestratorFailedException e) {
                    throw new BadRequestException("We can not check if your FreeIPA supports recipes: " + e.getMessage(), e);
                }
            } else {
                throw new BadRequestException("We can not check if your FreeIPA supports recipes because it is not in available state");
            }
        }
        return true;
    }

}
