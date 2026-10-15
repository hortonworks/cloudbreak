package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.RELEASE_VERSION_TAG;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties.FLOW_VARIABLE_NAME;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.chain.util.SetDefaultJavaVersionFlowChainService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.event.ClusterServicesRestartTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class SetDefaultJavaVersionFlowChainFactory implements FlowEventChainFactory<SetDefaultJavaVersionTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetDefaultJavaVersionFlowChainFactory.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private SetDefaultJavaVersionFlowChainService setDefaultJavaVersionFlowChainService;

    @Inject
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(SetDefaultJavaVersionTriggerEvent event, Map<Object, Object> contextParams) {
        if (event instanceof SetDefaultJavaVersionForUpgradeTriggerEvent) {
            ClusterUpgradeProperties properties = (ClusterUpgradeProperties) contextParams.get(FLOW_VARIABLE_NAME);
            if (properties == null) {
                LOGGER.warn("Upgrade properties are missing; keeping the current Java version.");
                return new FlowTriggerEventQueue(getName(), event, new ConcurrentLinkedQueue<>());
            }
            StackDto stack = stackDtoService.getByIdWithoutResources(event.getResourceId());
            String runtimeVersion = getTargetRuntimeVersion(properties.targetImage());
            String sourceRuntimeVersion = clouderaManagerProductsProvider.getCdhProduct(
                    Optional.ofNullable(properties.currentImage().products()).orElse(Set.of()))
                    .map(ClouderaManagerProduct::getVersion)
                    .filter(StringUtils::isNotBlank)
                    .map(CdhVersionProvider::getCdhFullVersionFromVersionString)
                    .filter(StringUtils::isNotBlank)
                    .orElse(StringUtils.defaultString(properties.currentImage().runtimeVersion()));
            List<SetDefaultJavaVersionTriggerEvent> javaEvents = setDefaultJavaVersionFlowChainService.setDefaultJavaVersionTriggerEvent(
                    stack, sourceRuntimeVersion, runtimeVersion);
            if (javaEvents.isEmpty()) {
                return new FlowTriggerEventQueue(getName(), event, new ConcurrentLinkedQueue<>());
            }
            return createFlowTriggerEventQueue(javaEvents.getFirst());
        }
        return createFlowTriggerEventQueue(event);
    }

    private String getTargetRuntimeVersion(ClusterUpgradeProperties.TargetImageUpgradeContext targetImage) {
        if (!targetImage.isBaseImage()) {
            String releaseVersion = Optional.ofNullable(targetImage.tags()).map(tags -> tags.get(RELEASE_VERSION_TAG.getKey())).orElse(null);
            if (StringUtils.isNotBlank(releaseVersion)) {
                return releaseVersion;
            }
        }
        return Optional.ofNullable(targetImage.cdhParcel())
                .map(ClouderaManagerProduct::getVersion)
                .filter(StringUtils::isNotBlank)
                .map(CdhVersionProvider::getCdhFullVersionFromVersionString)
                .filter(StringUtils::isNotBlank)
                .orElse(StringUtils.defaultString(targetImage.runtimeVersion()));
    }

    @Override
    public String initEvent() {
        return FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(SetDefaultJavaVersionTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), event.getResourceId(),
                event.getDefaultJavaVersion(), event.isRestartServices(), event.isRestartCM(), event.isRollingRestart(), event.accepted()));
        flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        if (event.isRestartCM()) {
            flowEventChain.add(new StackEvent(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), event.getResourceId(),
                    event.accepted()));
        }
        if (event.isRestartServices()) {
            flowEventChain.add(new ClusterServicesRestartTriggerEvent(CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event(), event.getResourceId(), true,
                    event.isRollingRestart(), false, true, event.accepted()));
        }
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

}
