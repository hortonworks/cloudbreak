package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterUpgradePreparationEvent extends StackEvent {

    private final Set<ClouderaManagerProduct> clouderaManagerProducts;

    private final String imageId;

    private final ClusterUpgradeProperties clusterUpgradeProperties;

    @JsonCreator
    public ClusterUpgradePreparationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("clouderaManagerProducts") Set<ClouderaManagerProduct> clouderaManagerProducts,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(selector, resourceId);
        this.clouderaManagerProducts = clouderaManagerProducts;
        this.imageId = imageId;
        this.clusterUpgradeProperties = clusterUpgradeProperties;
    }

    public ClusterUpgradeProperties getClusterUpgradeProperties() {
        return clusterUpgradeProperties;
    }

    public Set<ClouderaManagerProduct> getClouderaManagerProducts() {
        return clouderaManagerProducts;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public String toString() {
        return "ClusterUpgradePreparationEvent{" +
                "selector='" + selector() + '\'' +
                ", clouderaManagerProducts='" + clouderaManagerProducts + '\'' +
                ", imageId='" + imageId + '\'' +
                ", clusterUpgradeProperties='" + clusterUpgradeProperties + '\'' +
                '}' + super.toString();
    }
}
