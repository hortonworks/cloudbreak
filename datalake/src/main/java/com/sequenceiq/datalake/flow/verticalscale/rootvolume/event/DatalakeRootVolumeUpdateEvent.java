package com.sequenceiq.datalake.flow.verticalscale.rootvolume.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.events.RootVolumeUpdateRequest;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = DatalakeRootVolumeUpdateEvent.Builder.class)
public class DatalakeRootVolumeUpdateEvent extends BaseNamedFlowEvent implements Selectable {

    private final RootVolumeUpdateRequest rootVolumeUpdateRequest;

    private final String stackCrn;

    private final Long stackId;

    private final String clusterName;

    private final String accountId;

    private final String cloudPlatform;

    private final String initiatorUserCrn;

    //CHECKSTYLE:OFF:ExecutableStatementCount
    public DatalakeRootVolumeUpdateEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("stackCrn") String stackCrn,
            @JsonProperty("clusterName") String clusterName,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("rootVolumeUpdateRequest") RootVolumeUpdateRequest rootVolumeUpdateRequest,
            @JsonProperty("cloudPlatform") String cloudPlatform,
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("initiatorUserCrn") String initiatorUserCrn) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.rootVolumeUpdateRequest = rootVolumeUpdateRequest;
        this.stackCrn = stackCrn;
        this.clusterName = clusterName;
        this.accountId = accountId;
        this.cloudPlatform = cloudPlatform;
        this.stackId = stackId;
        this.initiatorUserCrn = initiatorUserCrn;
    }
    //CHECKSTYLE:ON:ExecutableStatementCount

    public RootVolumeUpdateRequest getRootVolumeUpdateRequest() {
        return rootVolumeUpdateRequest;
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public Long getStackId() {
        return stackId;
    }

    public String getInitiatorUserCrn() {
        return initiatorUserCrn;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private RootVolumeUpdateRequest rootVolumeUpdateRequest;

        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private String stackCrn;

        private String clusterName;

        private String accountId;

        private String cloudPlatform;

        private Long stackId;

        private String initiatorUserCrn;

        private Builder() {
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withStackCrn(String stackCrn) {
            this.stackCrn = stackCrn;
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

        public Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withRootVolumeUpdateRequest(RootVolumeUpdateRequest rootVolumeUpdateRequest) {
            this.rootVolumeUpdateRequest = rootVolumeUpdateRequest;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withStackId(Long stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder withInitiatorUserCrn(String initiatorUserCrn) {
            this.initiatorUserCrn = initiatorUserCrn;
            return this;
        }

        public DatalakeRootVolumeUpdateEvent build() {
            return new DatalakeRootVolumeUpdateEvent(selector, resourceId, accepted, resourceName, resourceCrn, stackCrn,
                    clusterName, accountId, rootVolumeUpdateRequest, cloudPlatform, stackId, initiatorUserCrn);
        }
    }
}
