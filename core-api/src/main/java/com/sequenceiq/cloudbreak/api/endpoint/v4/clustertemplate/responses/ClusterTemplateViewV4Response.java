package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterTemplateViewModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClusterTemplateViewV4Response extends CompactViewV4Response {

    private ResourceStatus status;

    @Schema(description = ClusterTemplateViewModelDescription.DATALAKE_REQUIRED)
    private DatalakeRequired datalakeRequired;

    @Schema(description = ClusterTemplateViewModelDescription.TYPE)
    private ClusterTemplateV4Type type;

    @Schema(description = ClusterTemplateViewModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @Schema(description = ClusterTemplateViewModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @Schema(description = ClusterTemplateViewModelDescription.STACK_TYPE)
    private String stackType;

    @Schema(description = ClusterTemplateViewModelDescription.STACK_VERSION)
    private String stackVersion;

    @Schema
    private String environmentCrn;

    @Schema
    private String environmentName;

    private Long created;

    private Long lastUpdated;

    private FeatureState featureState;

    public FeatureState getFeatureState() {
        return featureState;
    }

    public void setFeatureState(FeatureState featureState) {
        this.featureState = featureState;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public DatalakeRequired getDatalakeRequired() {
        return datalakeRequired;
    }

    public void setDatalakeRequired(DatalakeRequired datalakeRequired) {
        this.datalakeRequired = datalakeRequired;
    }

    public ClusterTemplateV4Type getType() {
        return type;
    }

    public void setType(ClusterTemplateV4Type type) {
        this.type = type;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getStackType() {
        return stackType;
    }

    public void setStackType(String stackType) {
        this.stackType = stackType;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getLastupdated() {
        return lastUpdated;
    }

    public void setLastupdated(Long lastupdated) {
        this.lastUpdated = lastupdated;
    }
}
