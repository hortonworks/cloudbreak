package com.sequenceiq.cloudbreak.service.salt;

import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.SaltMinimumAuthVersionConstants.APPLIED_MINIMUM_AUTH_VERSION;
import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.SaltMinimumAuthVersionConstants.UNKNOWN_MINIMUM_AUTH_VERSION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.SaltMinimumAuthVersionCalculator;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

/**
 * Keeps the salt master {@code minimum_auth_version} in line with the oldest salt minion of the cluster, so that a Salt version upgrade
 * by node replacement can run with mixed versions, and enforcement is restored once the last old minion is gone.
 * The value the running masters use is stored in the salt state cluster component; the masters are restarted whenever it has to change.
 */
@Service
public class SaltMinimumAuthVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltMinimumAuthVersionService.class);

    @Inject
    private SaltMinimumAuthVersionCalculator saltMinimumAuthVersionCalculator;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public void configureBootstrapParams(StackDto stack, List<GatewayConfig> gatewayConfigs, BootstrapParams params) {
        calculateMinimumAuthVersion(stack, gatewayConfigs).ifPresent(minimumAuthVersion -> {
            params.setSaltMasterMinimumAuthVersion(minimumAuthVersion);
            Optional<Integer> appliedMinimumAuthVersion = getAppliedMinimumAuthVersion(stack);
            if (appliedMinimumAuthVersion.filter(minimumAuthVersion::equals).isEmpty()) {
                if (isFirstBootstrap(stack)) {
                    LOGGER.info("Salt masters will start with minimum_auth_version {} on their first bootstrap, no restart is needed", minimumAuthVersion);
                    markAppliedMinimumAuthVersionUnknown(stack);
                } else {
                    LOGGER.info("Salt masters have to be restarted to change minimum_auth_version from {} to {}",
                            appliedMinimumAuthVersion.map(String::valueOf).orElse(UNKNOWN_MINIMUM_AUTH_VERSION), minimumAuthVersion);
                    params.setSaltMasterRestartNeeded(true);
                    if (appliedMinimumAuthVersion.isPresent()) {
                        markAppliedMinimumAuthVersionUnknown(stack);
                    }
                }
            }
        });
    }

    public void saveAppliedMinimumAuthVersion(StackDto stack, List<GatewayConfig> gatewayConfigs, BootstrapParams params) {
        if (params.getSaltMasterMinimumAuthVersion() != null
                && getAppliedMinimumAuthVersion(stack).filter(params.getSaltMasterMinimumAuthVersion()::equals).isEmpty()) {
            Set<String> gatewaysLeftOut = getGatewaysLeftOut(stack, gatewayConfigs);
            if (gatewaysLeftOut.isEmpty()) {
                LOGGER.info("Salt masters run with minimum_auth_version {}", params.getSaltMasterMinimumAuthVersion());
                updateSaltStateAttributes(stack, attributes -> attributes.put(APPLIED_MINIMUM_AUTH_VERSION, params.getSaltMasterMinimumAuthVersion()));
            } else {
                LOGGER.warn("Salt masters of the unreachable gateways {} were not restarted with minimum_auth_version {}, "
                        + "every salt master will be restarted at the next bootstrap again", gatewaysLeftOut, params.getSaltMasterMinimumAuthVersion());
            }
        }
    }

    private Set<String> getGatewaysLeftOut(StackDto stack, List<GatewayConfig> gatewayConfigs) {
        Set<String> reconciledGatewayAddresses = gatewayConfigs.stream()
                .map(GatewayConfig::getPrivateAddress)
                .collect(Collectors.toSet());
        return stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().stream()
                .filter(gateway -> !reconciledGatewayAddresses.contains(gateway.getPrivateIp()))
                .map(InstanceMetadataView::getInstanceId)
                .collect(Collectors.toSet());
    }

    private void markAppliedMinimumAuthVersionUnknown(StackDto stack) {
        updateSaltStateAttributes(stack, attributes -> attributes.put(APPLIED_MINIMUM_AUTH_VERSION, UNKNOWN_MINIMUM_AUTH_VERSION));
    }

    private Optional<Integer> calculateMinimumAuthVersion(StackDto stack, List<GatewayConfig> gatewayConfigs) {
        List<String> saltMasterVersions = gatewayConfigs.stream()
                .map(gatewayConfig -> gatewayConfig.getSaltVersion().orElse(null))
                .toList();
        List<String> saltMinionVersions = stack.getAllAvailableAndProviderDeletedInstances().stream()
                .map(tlsSecurityService::getInstanceSaltVersion)
                .toList();
        return saltMinimumAuthVersionCalculator.calculate(saltMasterVersions, saltMinionVersions);
    }

    private boolean isFirstBootstrap(StackDto stack) {
        ClusterComponent component = getSaltStateComponent(stack);
        return stack.getOrchestrator() != null && StringUtils.isBlank(stack.getOrchestrator().getApiEndpoint())
                && component != null
                && (component.getAttributes() == null || !component.getAttributes().getMap().containsKey(APPLIED_MINIMUM_AUTH_VERSION));
    }

    private Optional<Integer> getAppliedMinimumAuthVersion(StackDto stack) {
        return Optional.ofNullable(getSaltStateComponent(stack))
                .map(ClusterComponent::getAttributes)
                .map(Json::getMap)
                .map(attributes -> attributes.get(APPLIED_MINIMUM_AUTH_VERSION))
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast);
    }

    private void updateSaltStateAttributes(StackDto stack, Consumer<Map<String, Object>> attributeUpdate) {
        ClusterComponent saltStateComponent = getSaltStateComponent(stack);
        if (saltStateComponent == null) {
            LOGGER.warn("Salt state component of stack {} is missing, the salt masters will be restarted at the next bootstrap again", stack.getName());
        } else {
            Map<String, Object> attributes = Optional.ofNullable(saltStateComponent.getAttributes()).map(Json::getMap).orElseGet(HashMap::new);
            attributeUpdate.accept(attributes);
            saltStateComponent.setAttributes(new Json(attributes));
            clusterComponentConfigProvider.store(saltStateComponent);
        }
    }

    private ClusterComponent getSaltStateComponent(StackDto stack) {
        return clusterComponentConfigProvider.getComponent(stack.getCluster().getId(), ComponentType.SALT_STATE);
    }
}
