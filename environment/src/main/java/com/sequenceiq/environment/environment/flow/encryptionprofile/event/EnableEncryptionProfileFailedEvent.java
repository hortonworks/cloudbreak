package com.sequenceiq.environment.environment.flow.encryptionprofile.event;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnableEncryptionProfileFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final boolean encryptionProfileApplied;

    @JsonCreator
    public EnableEncryptionProfileFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("encryptionProfileApplied") boolean encryptionProfileApplied) {
        super(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.name(), resourceId, resourceName, resourceCrn, exception);
        this.encryptionProfileApplied = encryptionProfileApplied;
    }

    public boolean isEncryptionProfileApplied() {
        return encryptionProfileApplied;
    }

    public static EnableEncryptionProfileFailedEvent from(BaseNamedFlowEvent event, Exception exception) {
        return new EnableEncryptionProfileFailedEvent(event.getResourceId(), event.getResourceName(), event.getResourceCrn(), exception, true);
    }

    public static EnableEncryptionProfileFailedEvent validationFailed(BaseNamedFlowEvent event, Exception exception) {
        return new EnableEncryptionProfileFailedEvent(event.getResourceId(), event.getResourceName(), event.getResourceCrn(), exception, false);
    }

    @Override
    public String selector() {
        return FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.name();
    }
}
