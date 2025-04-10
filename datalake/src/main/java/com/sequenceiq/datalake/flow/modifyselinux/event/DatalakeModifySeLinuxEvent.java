package com.sequenceiq.datalake.flow.modifyselinux.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = DatalakeModifySeLinuxEvent.Builder.class)
public class DatalakeModifySeLinuxEvent extends BaseNamedFlowEvent {

    private final SeLinux selinuxMode;

    public DatalakeModifySeLinuxEvent(String selector, Long resourceId, String resourceName, String resourceCrn, SeLinux selinuxMode) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.selinuxMode = selinuxMode;
    }

    public DatalakeModifySeLinuxEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName,
        String resourceCrn, SeLinux selinuxMode) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.selinuxMode = selinuxMode;
    }

    public SeLinux getSelinuxMode() {
        return selinuxMode;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private SeLinux selinuxMode;

        private Builder() {
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withSelinuxMode(SeLinux selinuxMode) {
            this.selinuxMode = selinuxMode;
            return this;
        }

        public DatalakeModifySeLinuxEvent build() {
            return new DatalakeModifySeLinuxEvent(selector, resourceId, accepted, resourceName, resourceCrn, selinuxMode);
        }
    }
}
