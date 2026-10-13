package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeFlowChainStartEvent extends SdxEvent {

    public static final String DATALAKE_UPGRADE_FLOW_CHAIN_EVENT = "DatalakeUpgradeFlowChainEvent";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ClouderaManagerV4Request components;

    private final String imageId;

    private final boolean replaceVms;

    private final String backupLocation;

    private final DatalakeDrSkipOptions skipOptions;

    private final boolean rollingUpgradeEnabled;

    private final boolean keepVariant;

    //CHECKSTYLE:OFF
    @JsonCreator
    public DatalakeUpgradeFlowChainStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("replaceVms") boolean replaceVms,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("skipOptions") DatalakeDrSkipOptions skipOptions,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("keepVariant") boolean keepVariant,
            @JsonProperty("components") ClouderaManagerV4Request components) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
        this.replaceVms = replaceVms;
        this.backupLocation = backupLocation;
        this.skipOptions = skipOptions;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.keepVariant = keepVariant;
        this.components = components;
    }
    //CHECKSTYLE:ON

    public ClouderaManagerV4Request getComponents() {
        return components;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean isReplaceVms() {
        return replaceVms;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public DatalakeDrSkipOptions getSkipOptions() {
        return skipOptions;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    @Override
    public String selector() {
        return DATALAKE_UPGRADE_FLOW_CHAIN_EVENT;
    }

    public boolean isKeepVariant() {
        return keepVariant;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeUpgradeFlowChainStartEvent.class, other,
                event -> Objects.equals(imageId, event.imageId)
                        && replaceVms == event.replaceVms
                        && Objects.equals(backupLocation, event.backupLocation)
                        && Objects.equals(components, event.components));
    }

    @Override
    public String toString() {
        return "DatalakeUpgradeFlowChainStartEvent{" +
                "imageId='" + imageId + '\'' +
                ", replaceVms=" + replaceVms +
                ", backupLocation='" + backupLocation + '\'' +
                ", skipOptions=" + skipOptions +
                ", rollingUpgradeEnabled=" + rollingUpgradeEnabled +
                ", keepVariant=" + keepVariant +
                ", components=" + components +
                "} " + super.toString();
    }
}
