package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradePreparationEvent extends StackEvent {

    private final Set<ClouderaManagerProduct> clouderaManagerProducts;

    @JsonCreator
    public ClusterUpgradePreparationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("clouderaManagerProducts") Set<ClouderaManagerProduct> clouderaManagerProducts) {
        super(selector, resourceId);
        this.clouderaManagerProducts = clouderaManagerProducts;
    }

    public Set<ClouderaManagerProduct> getClouderaManagerProducts() {
        return clouderaManagerProducts;
    }

    @Override
    public String toString() {
        return "ClusterUpgradePreparationEvent{" +
                "selector='" + selector() + '\'' +
                ", clouderaManagerProducts='" + clouderaManagerProducts + '\'' +
                '}' + super.toString();
    }
}
