package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.common.model.OsType;

public class ClusterUpgradePreparationTriggerEvent extends StackEvent {

    private final ImageChangeDto imageChangeDto;

    private final String runtimeVersion;

    private final OsType currentOsType;

    private final ClusterUpgradeProperties clusterUpgradeProperties;

    @JsonCreator
    public ClusterUpgradePreparationTriggerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto,
            @JsonProperty("runtimeVersion") String runtimeVersion,
            @JsonProperty("currentOsType") OsType currentOsType,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT.event(), resourceId, accepted);
        this.imageChangeDto = imageChangeDto;
        this.runtimeVersion = runtimeVersion;
        this.currentOsType = currentOsType;
        this.clusterUpgradeProperties = clusterUpgradeProperties;
    }

    public ClusterUpgradeProperties getClusterUpgradeProperties() {
        return clusterUpgradeProperties;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    @JsonProperty("currentOsType")
    private OsType getCurrentOsType() {
        return currentOsType;
    }

    @Override
    public String toString() {
        return "ClusterUpgradePreparationTriggerEvent{" +
                "imageChangeDto=" + imageChangeDto +
                ", runtimeVersion='" + runtimeVersion + '\'' +
                ", currentOsType='" + currentOsType + '\'' +
                ", clusterUpgradeProperties='" + clusterUpgradeProperties + '\'' +
                "} " + super.toString();
    }
}
