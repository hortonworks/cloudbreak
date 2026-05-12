package com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmEvent;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class UpdateTrustedRealmTriggerEvent extends BaseFlowEvent {

    private final String environmentCrn;

    private final String realm;

    private final boolean remove;

    @JsonCreator
    public UpdateTrustedRealmTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("environmentCrn") String environmentCrn,
            @JsonProperty("realm") String realm,
            @JsonProperty("remove") boolean remove,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
        this.environmentCrn = environmentCrn;
        this.realm = realm;
        this.remove = remove;
    }

    public static UpdateTrustedRealmTriggerEvent fromChainTrigger(UpdateTrustedRealmChainTriggerEvent chainEvent) {
        return new UpdateTrustedRealmTriggerEvent(
                UpdateTrustedRealmEvent.UPDATE_TRUSTED_REALM_TRIGGER_EVENT.event(),
                chainEvent.getResourceId(),
                chainEvent.getResourceCrn(),
                chainEvent.getEnvironmentCrn(),
                chainEvent.getRealm(),
                chainEvent.isRemove(),
                chainEvent.accepted());
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getRealm() {
        return realm;
    }

    public boolean isRemove() {
        return remove;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(UpdateTrustedRealmTriggerEvent.class, other,
                event -> Objects.equals(environmentCrn, event.environmentCrn)
                        && Objects.equals(realm, event.realm)
                        && remove == event.remove);
    }

    @Override
    public String toString() {
        return "UpdateTrustedRealmTriggerEvent{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", realm='" + realm + '\'' +
                ", remove=" + remove +
                '}';
    }
}
