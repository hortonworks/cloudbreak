package com.sequenceiq.cloudbreak.service.upgrade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradeParcelSettingsPreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

/**
 * Builds {@link ClusterUpgradeProperties} from validation requests and resolves properties carried by upgrade flow events.
 * <p>
 * Validation requests carry the target image and upgrade options. Subsequent events carry properties in persisted flow JSON. Legacy events
 * only have {@code imageId} (and legacy upgrade flags on {@link ClusterUpgradeServiceValidationEvent}). When a
 * flow is resumed from such payload, {@code clusterUpgradeProperties} is null and must be rebuilt via
 * {@link ClusterUpgradePropertiesFactory} so actions and handlers can run without NPE.
 * <p>
 * TODO CB-33421: Remove legacy event fallbacks once in-flight flow events always carry clusterUpgradeProperties in JSON.
 */
@Service
public class ClusterUpgradePropertiesResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradePropertiesResolver.class);

    private static final boolean PREPARATION_ROLLING_UPGRADE_ENABLED = false;

    private static final boolean DEFAULT_LOCK_COMPONENTS = false;

    private static final boolean DEFAULT_ROLLING_UPGRADE_ENABLED = true;

    private static final boolean DEFAULT_REPLACE_VMS = false;

    private final ClusterUpgradePropertiesFactory clusterUpgradePropertiesFactory;

    public ClusterUpgradePropertiesResolver(ClusterUpgradePropertiesFactory clusterUpgradePropertiesFactory) {
        this.clusterUpgradePropertiesFactory = clusterUpgradePropertiesFactory;
    }

    public ClusterUpgradeProperties resolve(ClusterUpgradeValidationTriggerEvent event) {
        if (event.getClusterUpgradeProperties() != null) {
            return event.getClusterUpgradeProperties();
        }
        LOGGER.debug("Resolving cluster upgrade properties for validation request, target image {}", event.getImageId());
        ImageChangeDto imageChangeDto = event.getImageChangeDto() != null ? event.getImageChangeDto()
                : new ImageChangeDto(event.getResourceId(), event.getImageId());
        return clusterUpgradePropertiesFactory.create(imageChangeDto, event.isLockComponents(), event.isRollingUpgradeEnabled(), event.isReplaceVms());
    }

    public ClusterUpgradeProperties resolve(ClusterUpgradePreparationTriggerEvent event) {
        return resolvePreparation(event.getImageChangeDto(), event.getClusterUpgradeProperties());
    }

    public ClusterUpgradeProperties resolve(ClusterUpgradeParcelSettingsPreparationEvent event) {
        return resolvePreparation(event.getImageChangeDto(), event.getClusterUpgradeProperties());
    }

    public ClusterUpgradeProperties resolve(ClusterUpgradePreparationEvent event) {
        if (event.getClusterUpgradeProperties() != null) {
            return event.getClusterUpgradeProperties();
        }
        // Legacy download handlers also used the current catalog; these persisted events contain no target catalog to recover.
        LOGGER.warn("Resuming legacy preparation for image {} without target catalog metadata; using the current image catalog", event.getImageId());
        return resolvePreparation(new ImageChangeDto(event.getResourceId(), event.getImageId()), null);
    }

    private ClusterUpgradeProperties resolvePreparation(ImageChangeDto imageChangeDto, ClusterUpgradeProperties properties) {
        if (properties != null) {
            return properties;
        }
        LOGGER.debug("Rebuilding cluster upgrade properties for resumed preparation flow, target image {}", imageChangeDto.getImageId());
        return clusterUpgradePropertiesFactory.create(imageChangeDto, DEFAULT_LOCK_COMPONENTS, PREPARATION_ROLLING_UPGRADE_ENABLED, DEFAULT_REPLACE_VMS);
    }

    public ClusterUpgradeProperties resolve(ClusterUpgradeValidationEvent event) {
        ClusterUpgradeProperties properties = event.getClusterUpgradeProperties();
        if (properties != null) {
            return properties;
        }
        String targetImageId = event.getImageId();
        LOGGER.debug("Rebuilding cluster upgrade properties for resumed validation flow, target image {}", targetImageId);
        if (event instanceof ClusterUpgradeServiceValidationEvent serviceValidationEvent) {
            return clusterUpgradePropertiesFactory.create(new ImageChangeDto(event.getResourceId(), targetImageId),
                    serviceValidationEvent.isLockComponents(), serviceValidationEvent.isRollingUpgradeEnabled(),
                    serviceValidationEvent.isReplaceVms());
        }
        return clusterUpgradePropertiesFactory.create(new ImageChangeDto(event.getResourceId(), targetImageId), DEFAULT_LOCK_COMPONENTS,
                DEFAULT_ROLLING_UPGRADE_ENABLED, DEFAULT_REPLACE_VMS);
    }
}
