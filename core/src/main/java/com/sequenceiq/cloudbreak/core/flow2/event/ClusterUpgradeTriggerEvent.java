package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeTriggerEvent extends StackEvent {

    private final String imageId;

    private final boolean rollingUpgradeEnabled;

    private final Image originalImage;

    private final ClusterUpgradeProperties clusterUpgradeProperties;

    @JsonCreator
    public ClusterUpgradeTriggerEvent(
            @JsonProperty("selector") String event,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("originalImage") Image originalImage,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(event, resourceId, accepted);
        this.imageId = imageId;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.originalImage = originalImage;
        this.clusterUpgradeProperties = clusterUpgradeProperties;
    }

    public ClusterUpgradeProperties getClusterUpgradeProperties() {
        return clusterUpgradeProperties;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    public Image getOriginalImage() {
        return originalImage;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ClusterUpgradeTriggerEvent.class, other,
                event -> Objects.equals(imageId, event.imageId)
                        && Objects.equals(rollingUpgradeEnabled, event.rollingUpgradeEnabled)
                        && Objects.equals(originalImage, event.originalImage)
                        && Objects.equals(clusterUpgradeProperties, event.clusterUpgradeProperties));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterUpgradeTriggerEvent.class.getSimpleName() + "[", "]")
                .add("imageId='" + imageId + "'")
                .add("rollingUpgradeEnabled=" + rollingUpgradeEnabled)
                .add("originalImage=" + originalImage)
                .add("clusterUpgradeProperties=" + clusterUpgradeProperties)
                .toString();
    }
}
