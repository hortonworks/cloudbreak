package com.sequenceiq.datalake.flow.verticalscale.diskupdate.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = DatalakeDiskUpdateEvent.Builder.class)
public class DatalakeDiskUpdateEvent extends BaseNamedFlowEvent implements Selectable {

    private final DiskUpdateRequest datalakeDiskUpdateRequest;

    private final String stackCrn;

    private final Long stackId;

    private final String clusterName;

    private final String accountId;

    private final List<Volume> volumesToBeUpdated;

    private final String cloudPlatform;

    //CHECKSTYLE:OFF:ExecutableStatementCount
    public DatalakeDiskUpdateEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("stackCrn") String stackCrn,
            @JsonProperty("clusterName") String clusterName,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("datalakeDiskUpdateRequest") DiskUpdateRequest datalakeDiskUpdateRequest,
            @JsonProperty("volumesToBeUpdated") List<Volume> volumesToBeUpdated,
            @JsonProperty("cloudPlatform") String cloudPlatform,
            @JsonProperty("stackId") Long stackId) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.datalakeDiskUpdateRequest = datalakeDiskUpdateRequest;
        this.stackCrn = stackCrn;
        this.clusterName = clusterName;
        this.accountId = accountId;
        this.volumesToBeUpdated = volumesToBeUpdated;
        this.cloudPlatform = cloudPlatform;
        this.stackId = stackId;
    }
    //CHECKSTYLE:ON:ExecutableStatementCount

    public DiskUpdateRequest getDatalakeDiskUpdateRequest() {
        return datalakeDiskUpdateRequest;
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

    public List<Volume> getVolumesToBeUpdated() {
        return volumesToBeUpdated;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public Long getStackId() {
        return stackId;
    }

    public static DatalakeDiskUpdateEvent.Builder builder() {
        return new DatalakeDiskUpdateEvent.Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private DiskUpdateRequest datalakeDiskUpdateRequest;

        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private String stackCrn;

        private String clusterName;

        private String accountId;

        private List<Volume> volumesToBeUpdated;

        private String cloudPlatform;

        private Long stackId;

        private Builder() {
        }

        public DatalakeDiskUpdateEvent.Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withStackCrn(String stackCrn) {
            this.stackCrn = stackCrn;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withDatalakeDiskUpdateRequest(DiskUpdateRequest datalakeDiskUpdateRequest) {
            this.datalakeDiskUpdateRequest = datalakeDiskUpdateRequest;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withVolumesToBeUpdated(List<Volume> volumesToBeUpdated) {
            this.volumesToBeUpdated = volumesToBeUpdated;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withStackId(Long stackId) {
            this.stackId = stackId;
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withDatalakeDiskUpdateEvent(DatalakeDiskUpdateEvent event) {
            this.resourceCrn = event.getResourceCrn();
            this.resourceId = event.getResourceId();
            this.resourceName = event.getResourceName();
            this.datalakeDiskUpdateRequest = event.datalakeDiskUpdateRequest;
            this.stackCrn = event.getStackCrn();
            this.clusterName = event.getClusterName();
            this.accountId = event.getAccountId();
            return this;
        }

        public DatalakeDiskUpdateEvent.Builder withStackV4Response(StackV4Response stackV4Response) {
            this.cloudPlatform = stackV4Response.getCloudPlatform().name();
            this.stackId = stackV4Response.getId();
            return this;
        }

        public DatalakeDiskUpdateEvent build() {
            return new DatalakeDiskUpdateEvent(selector, resourceId, accepted, resourceName, resourceCrn, stackCrn,
                    clusterName, accountId, datalakeDiskUpdateRequest, volumesToBeUpdated, cloudPlatform, stackId);
        }
    }
}
