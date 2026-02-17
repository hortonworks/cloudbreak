package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.model.OsType;

public abstract class AbstractClusterUpgradeEvent extends StackEvent {

    private final Set<ClouderaManagerProduct> upgradeCandidateProducts;

    private final OsType originalOsType;

    @JsonCreator
    public AbstractClusterUpgradeEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("upgradeCandidateProducts") Set<ClouderaManagerProduct> upgradeCandidateProducts,
            @JsonProperty("originalOsType") OsType originalOsType) {
        super(stackId);
        this.upgradeCandidateProducts = upgradeCandidateProducts;
        this.originalOsType = originalOsType;
    }

    public Set<ClouderaManagerProduct> getUpgradeCandidateProducts() {
        return upgradeCandidateProducts;
    }

    public OsType getOriginalOsType() {
        return originalOsType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AbstractClusterUpgradeEvent.class.getSimpleName() + "[", "]")
                .add("upgradeCandidateProducts=" + upgradeCandidateProducts)
                .add("originalOsType=" + originalOsType)
                .add(super.toString())
                .toString();
    }
}
