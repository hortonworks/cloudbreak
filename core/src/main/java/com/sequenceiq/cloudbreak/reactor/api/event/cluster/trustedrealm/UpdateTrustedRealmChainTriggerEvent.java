package com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class UpdateTrustedRealmChainTriggerEvent extends BaseFlowEvent {

    private final String environmentCrn;

    private final String realm;

    private final boolean saltUpdateRequired;

    private final boolean remove;

    @JsonCreator
    public UpdateTrustedRealmChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("environmentCrn") String environmentCrn,
            @JsonProperty("realm") String realm,
            @JsonProperty("saltUpdateRequired") boolean saltUpdateRequired,
            @JsonProperty("remove") boolean remove) {
        super(selector, resourceId, resourceCrn);
        this.environmentCrn = environmentCrn;
        this.realm = realm;
        this.saltUpdateRequired = saltUpdateRequired;
        this.remove = remove;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getRealm() {
        return realm;
    }

    public boolean isSaltUpdateRequired() {
        return saltUpdateRequired;
    }

    public boolean isRemove() {
        return remove;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(UpdateTrustedRealmChainTriggerEvent.class, other,
                event -> Objects.equals(environmentCrn, event.environmentCrn)
                        && Objects.equals(realm, event.realm)
                        && saltUpdateRequired == event.saltUpdateRequired
                        && remove == event.remove);
    }

    @Override
    public String toString() {
        return "UpdateTrustedRealmChainTriggerEvent{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", realm='" + realm + '\'' +
                ", saltUpdateRequired=" + saltUpdateRequired +
                ", remove=" + remove +
                ", resourceId=" + getResourceId() +
                ", resourceCrn='" + getResourceCrn() + '\'' +
                '}';
    }
}
