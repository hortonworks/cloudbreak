package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class FreeIpaLoadBalancerProvisionCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerProvisionCondition.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CrnService crnService;

    @Inject
    private StackService stackService;

    @Value("${freeipa.loadbalancer.supported-variants}")
    private Set<String> supportedVariants;

    public boolean loadBalancerProvisionEnabled(Long stackId) {
        boolean loadBalancerProvisionEntitled = loadBalancerProvisionEntitled();
        String platformVariant = stackService.getStackById(stackId).getPlatformvariant();
        boolean platformVariantSupported = supportedVariants.contains(platformVariant);
        if (platformVariantSupported && loadBalancerProvisionEntitled) {
            LOGGER.debug("Load balancer creation is enabled for FreeIPA cluster.");
            return true;
        } else {
            LOGGER.debug("Load balancer creation is not enabled for FreeIPA cluster. Entitlement enabled: {}, Platform variant supported: {}",
                    loadBalancerProvisionEntitled, platformVariantSupported);
            return false;
        }
    }

    private boolean loadBalancerProvisionEntitled() {
        String accountId = crnService.getCurrentAccountId();
        return entitlementService.isFreeIpaLoadBalancerEnabled(accountId);
    }

}
