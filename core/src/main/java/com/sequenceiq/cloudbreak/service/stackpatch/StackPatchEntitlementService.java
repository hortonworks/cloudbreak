package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.StackPatchTypeConfig;

@Service
public class StackPatchEntitlementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPatchEntitlementService.class);

    @Inject
    private ExistingStackPatcherConfig existingStackPatcherConfig;

    @Inject
    private EntitlementService entitlementService;

    public boolean isEntitled(Stack stack, StackPatchType stackPatchType) {
        Map<StackPatchType, StackPatchTypeConfig> patchConfigs = existingStackPatcherConfig.getPatchConfigs();
        if (patchConfigs == null) {
            return true;
        }
        StackPatchTypeConfig patchConfig = patchConfigs.get(stackPatchType);
        if (patchConfig == null || StringUtils.isBlank(patchConfig.getEntitlement())) {
            return true;
        }
        Entitlement requiredEntitlement;
        try {
            requiredEntitlement = Entitlement.valueOf(patchConfig.getEntitlement().trim());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown entitlement {} configured for patch type {}, denying patch apply",
                    patchConfig.getEntitlement(), stackPatchType);
            return false;
        }
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        boolean entitled = entitlementService.isEntitledFor(accountId, requiredEntitlement);
        if (!entitled) {
            LOGGER.info("Entitlement {} is not enabled for account {} and patch type {}",
                    requiredEntitlement, accountId, stackPatchType);
        }
        return entitled;
    }
}
