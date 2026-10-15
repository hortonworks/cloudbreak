package com.sequenceiq.cloudbreak.core.flow2.chain.util;

import java.util.List;
import java.util.OptionalInt;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.javaversion.JavaVersionPolicy;

@Service
public class SetDefaultJavaVersionFlowChainService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetDefaultJavaVersionFlowChainService.class);

    @Inject
    private JavaVersionPolicy javaVersionPolicy;

    public List<SetDefaultJavaVersionTriggerEvent> setDefaultJavaVersionTriggerEvent(StackDto stack, String sourceRuntimeVersion,
            String targetRuntimeVersion) {
        Integer currentJavaVersion = stack.getStack().getJavaVersion();
        OptionalInt targetJavaVersion = javaVersionPolicy.autoUpgradeTarget(sourceRuntimeVersion, targetRuntimeVersion, currentJavaVersion);
        if (targetJavaVersion.isEmpty()) {
            LOGGER.debug("Java {} is kept for the upgrade from runtime '{}' to '{}'.", currentJavaVersion, sourceRuntimeVersion, targetRuntimeVersion);
            return List.of();
        }
        LOGGER.info("Java version is changed from {} to {} because of the upgrade from runtime '{}' to '{}'.",
                currentJavaVersion, targetJavaVersion.getAsInt(), sourceRuntimeVersion, targetRuntimeVersion);
        return List.of(new SetDefaultJavaVersionTriggerEvent(FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT, stack.getId(),
                String.valueOf(targetJavaVersion.getAsInt()), false, false, false));
    }
}
