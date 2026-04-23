package com.sequenceiq.freeipa.service.loadbalancer;

import static com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType.INTERNAL_NLB;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaLoadBalancerProvisionCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerProvisionCondition.class);

    private static final String FREEIPA_HEALTH_AGENT_PACKAGE_NAME = "freeipa-health-agent";

    private static final Versioned FREEIPA_HEALTH_AGENT_LB_VERSION = () -> "2.1.0.2-b2228";

    @Inject
    private StackService stackService;

    @Value("${freeipa.loadbalancer.supported-variants}")
    private Set<String> supportedVariants;

    @Inject
    private ImageService imageService;

    public boolean loadBalancerProvisionEnabled(Long stackId, FreeIpaLoadBalancerType loadBalancer) {
        Stack stack = stackService.getStackById(stackId);
        if (loadBalancerCreationSupported(stack) && internalLoadbalancerRequested(loadBalancer)) {
            LOGGER.debug("Load balancer creation is enabled for FreeIPA cluster.");
            return true;
        } else {
            LOGGER.debug("Load balancer creation is not enabled for FreeIPA cluster. Platform variant supported: {}, " +
                            "Health agent version supported: {}, LoadBalancer creation in request: {}",
                    supportedVariants.contains(stack.getPlatformvariant()),
                    isHealthAgentVersionSupported(stack),
                    loadBalancer);
            return false;
        }
    }

    private boolean internalLoadbalancerRequested(FreeIpaLoadBalancerType loadBalancer) {
        return loadBalancer == INTERNAL_NLB;
    }

    private boolean loadBalancerCreationSupported(Stack stack) {
        return supportedVariants.contains(stack.getPlatformvariant())
                && isHealthAgentVersionSupported(stack);
    }

    private boolean isHealthAgentVersionSupported(Stack stack) {
        String healthAgentVersion = Optional.ofNullable(imageService.getImageForStack(stack))
                .map(Image::getPackageVersions)
                .map(pkgVer -> pkgVer.get(FREEIPA_HEALTH_AGENT_PACKAGE_NAME))
                .orElse("");
        if (StringUtils.hasLength(healthAgentVersion)) {
            VersionComparator versionComparator = new VersionComparator();
            if (versionComparator.compare(() -> healthAgentVersion, FREEIPA_HEALTH_AGENT_LB_VERSION) >= 0) {
                LOGGER.debug("freeipa-health-agent version is supported for FreeIPA Load Balancer: {}", healthAgentVersion);
                return true;
            } else {
                LOGGER.debug("freeipa-health-agent version is NOT supported for FreeIPA Load Balancer: {}", healthAgentVersion);
                return false;
            }
        } else {
            LOGGER.warn("freeipa-health-agent version is not listed in package versions or the format is not supported: {}", healthAgentVersion);
            return false;
        }
    }

}
