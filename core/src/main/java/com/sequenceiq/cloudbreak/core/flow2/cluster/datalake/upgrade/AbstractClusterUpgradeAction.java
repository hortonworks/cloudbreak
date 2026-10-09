package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties.FLOW_VARIABLE_NAME;

import java.util.Map;

import jakarta.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractClusterUpgradeAction<P extends Payload>
        extends AbstractAction<FlowState, FlowEvent, ClusterUpgradeContext, P> {

    protected static final String ORIGINAL_IMAGE = "ORIGINAL_IMAGE";

    protected static final String TARGET_IMAGE = "TARGET_IMAGE";

    protected static final String ROLLING_UPGRADE_ENABLED = "ROLLING_UPGRADE_ENABLED";

    @Inject
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    @Inject
    private StackService stackService;

    protected AbstractClusterUpgradeAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    protected Image getOriginalImage(Map<Object, Object> variables) {
        return (Image) variables.get(ORIGINAL_IMAGE);
    }

    protected ClusterUpgradeProperties getUpgradeProperties(ClusterUpgradeTriggerEvent payload, Map<Object, Object> variables) {
        if (payload.getClusterUpgradeProperties() != null) {
            return payload.getClusterUpgradeProperties();
        }
        return (ClusterUpgradeProperties) variables.computeIfAbsent(FLOW_VARIABLE_NAME, key -> clusterUpgradePropertiesResolver.resolve(payload));
    }

    protected ClusterUpgradeProperties getUpgradeProperties(Map<Object, Object> variables, Long stackId) {
        // TODO CB-33421: Remove fallback once in-flight flows always contain upgrade properties.
        return (ClusterUpgradeProperties) variables.computeIfAbsent(FLOW_VARIABLE_NAME,
                key -> clusterUpgradePropertiesResolver.resolve(stackId, getOriginalImage(variables), (StatedImage) variables.get(TARGET_IMAGE),
                        Boolean.TRUE.equals(variables.get(ROLLING_UPGRADE_ENABLED))));
    }

    protected StackService getStackService() {
        return stackService;
    }

}
