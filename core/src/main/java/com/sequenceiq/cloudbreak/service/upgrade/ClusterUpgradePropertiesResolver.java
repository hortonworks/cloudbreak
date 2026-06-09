package com.sequenceiq.cloudbreak.service.upgrade;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;

/**
 * Resolves {@link ClusterUpgradeProperties} from cluster upgrade validation flow events.
 * <p>
 * New events carry {@code clusterUpgradeProperties} in persisted flow JSON. Events saved before that migration
 * only have {@code imageId} (and legacy upgrade flags on {@link ClusterUpgradeServiceValidationEvent}). When a
 * flow is resumed from such payload, {@code clusterUpgradeProperties} is null and must be rebuilt via
 * {@link ClusterUpgradePropertiesFactory} so actions and handlers can run without NPE.
 * <p>
 * TODO CB-33421: Remove once in-flight flow events always carry clusterUpgradeProperties in JSON.
 */
@Service
public class ClusterUpgradePropertiesResolver {

    private static final boolean DEFAULT_LOCK_COMPONENTS = false;

    private static final boolean DEFAULT_ROLLING_UPGRADE_ENABLED = true;

    private static final boolean DEFAULT_REPLACE_VMS = false;

    private final ClusterUpgradePropertiesFactory clusterUpgradePropertiesFactory;

    public ClusterUpgradePropertiesResolver(ClusterUpgradePropertiesFactory clusterUpgradePropertiesFactory) {
        this.clusterUpgradePropertiesFactory = clusterUpgradePropertiesFactory;
    }

    public ClusterUpgradeProperties resolveUnchecked(ClusterUpgradeValidationEvent event) {
        try {
            return resolve(event);
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException("Failed to rebuild cluster upgrade properties for resumed flow", e);
        }
    }

    public ClusterUpgradeProperties resolve(ClusterUpgradeValidationEvent event)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ClusterUpgradeProperties properties = event.getClusterUpgradeProperties();
        if (properties != null) {
            return properties;
        }
        String targetImageId = event.getImageId();
        if (event instanceof ClusterUpgradeServiceValidationEvent serviceValidationEvent) {
            return clusterUpgradePropertiesFactory.create(event.getResourceId(), targetImageId,
                    serviceValidationEvent.isLockComponents(), serviceValidationEvent.isRollingUpgradeEnabled(),
                    serviceValidationEvent.isReplaceVms());
        }
        return clusterUpgradePropertiesFactory.create(event.getResourceId(), targetImageId, DEFAULT_LOCK_COMPONENTS,
                DEFAULT_ROLLING_UPGRADE_ENABLED, DEFAULT_REPLACE_VMS);
    }
}
