package com.sequenceiq.environment.environment.flow.encryptionprofile.event;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class DisableEncryptionProfileFailedEvent extends BaseFailedFlowEvent implements Selectable {

    @JsonCreator
    public DisableEncryptionProfileFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("exception") Exception exception) {
        super(FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT.name(), resourceId, resourceName, resourceCrn, exception);
    }

    public static DisableEncryptionProfileFailedEvent from(BaseNamedFlowEvent event, Exception exception) {
        return new DisableEncryptionProfileFailedEvent(event.getResourceId(), event.getResourceName(), event.getResourceCrn(), exception);
    }

    @Override
    public String selector() {
        return FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT.name();
    }
}
