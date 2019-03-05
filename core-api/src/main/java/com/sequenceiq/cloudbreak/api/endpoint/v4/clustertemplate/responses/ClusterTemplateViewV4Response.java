package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterTemplateViewModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClusterTemplateViewV4Response extends CompactViewV4Response {

    private ResourceStatus status;

    @ApiModelProperty(ClusterTemplateViewModelDescription.DATALAKE_REQUIRED)
    private DatalakeRequired datalakeRequired;

    @ApiModelProperty(ClusterTemplateViewModelDescription.TYPE)
    private ClusterTemplateV4Type type;

    @ApiModelProperty(ClusterTemplateViewModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(ClusterTemplateViewModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(ClusterTemplateViewModelDescription.STACK_TYPE)
    private String stackType;

    @ApiModelProperty(ClusterTemplateViewModelDescription.STACK_VERSION)
    private String stackVersion;

    @ApiModelProperty
    private String environmentName;

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

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }
}
