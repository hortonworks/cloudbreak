package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

@JsonDeserialize(builder = DistroXDiskUpdateEvent.Builder.class)
public class DistroXDiskUpdateEvent  extends BaseFlowEvent implements Selectable {

    private final DiskUpdateRequest diskUpdateRequest;

    private final Long stackId;

    private final String clusterName;

    private final String accountId;

    private final List<Volume> volumesToBeUpdated;

    private final String cloudPlatform;

    //CHECKSTYLE:OFF:ExecutableStatementCount
    public DistroXDiskUpdateEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("clusterName") String clusterName,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("diskUpdateRequest") DiskUpdateRequest diskUpdateRequest,
            @JsonProperty("volumesToBeUpdated") List<Volume> volumesToBeUpdated,
            @JsonProperty("cloudPlatform") String cloudPlatform,
            @JsonProperty("stackId") Long stackId) {
        super(selector, resourceId, resourceCrn);
        this.diskUpdateRequest = diskUpdateRequest;
        this.clusterName = clusterName;
        this.accountId = accountId;
        this.volumesToBeUpdated = volumesToBeUpdated;
        this.cloudPlatform = cloudPlatform;
        this.stackId = stackId;
    }
    //CHECKSTYLE:ON:ExecutableStatementCount

    public DiskUpdateRequest getDiskUpdateRequest() {
        return diskUpdateRequest;
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

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private DiskUpdateRequest diskUpdateRequest;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private String clusterName;

        private String accountId;

        private List<Volume> volumesToBeUpdated;

        private String cloudPlatform;

        private Long stackId;

        private Builder() {
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
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

        public Builder withDiskUpdateRequest(DiskUpdateRequest diskUpdateRequest) {
            this.diskUpdateRequest = diskUpdateRequest;
            return this;
        }

        public Builder withVolumesToBeUpdated(List<Volume> volumesToBeUpdated) {
            this.volumesToBeUpdated = volumesToBeUpdated;
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

        public DistroXDiskUpdateEvent build() {
            return new DistroXDiskUpdateEvent(selector, resourceId, resourceCrn, clusterName, accountId,
                    diskUpdateRequest, volumesToBeUpdated, cloudPlatform, stackId);
        }
    }
}
