package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public abstract class AbstractClusterUpgradeEvent extends StackEvent {

    private final Set<ClouderaManagerProduct> upgradeCandidateProducts;

    public AbstractClusterUpgradeEvent(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
        this.upgradeCandidateProducts = Collections.emptySet();
    }

    @JsonCreator
    public AbstractClusterUpgradeEvent(@JsonProperty("resourceId") Long stackId,
            @JsonProperty("upgradeCandidateProducts") Set<ClouderaManagerProduct> upgradeCandidateProducts) {
        super(stackId);
        this.upgradeCandidateProducts = upgradeCandidateProducts;
    }

    public Set<ClouderaManagerProduct> getUpgradeCandidateProducts() {
        return upgradeCandidateProducts;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AbstractClusterUpgradeEvent.class.getSimpleName() + "[", "]")
                .add("upgradeCandidateProducts=" + upgradeCandidateProducts)
                .add(super.toString())
                .toString();
    }
}
