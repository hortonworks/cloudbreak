package com.sequenceiq.cloudbreak.api.model.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.CompactViewResponse;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterTemplateViewResponse extends CompactViewResponse {

    private ResourceStatus status;

    @ApiModelProperty(ModelDescriptions.ClusterTemplateViewModelDescription.DATALAKE_REQUIRED)
    private DatalakeRequired datalakeRequired;

    @ApiModelProperty(ModelDescriptions.ClusterTemplateViewModelDescription.TYPE)
    private ClusterTemplateType type;

    @ApiModelProperty(ModelDescriptions.ClusterTemplateViewModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(ModelDescriptions.ClusterTemplateViewModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(ModelDescriptions.ClusterTemplateViewModelDescription.STACK_TYPE)
    private String stackType;

    @ApiModelProperty(ModelDescriptions.ClusterTemplateViewModelDescription.STACK_VERSION)
    private String stackVersion;

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

    public ClusterTemplateType getType() {
        return type;
    }

    public void setType(ClusterTemplateType type) {
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
}
