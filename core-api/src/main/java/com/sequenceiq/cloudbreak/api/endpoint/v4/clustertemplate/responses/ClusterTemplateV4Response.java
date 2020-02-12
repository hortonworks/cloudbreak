package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterTemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClusterTemplateV4Response extends ClusterTemplateV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    private ResourceStatus status;

    @ApiModelProperty(value = ClusterTemplateModelDescription.DATALAKE_REQUIRED,
            allowableValues = "NONE,OPTIONAL,REQUIRED")
    private DatalakeRequired datalakeRequired = DatalakeRequired.OPTIONAL;

    private String environmentCrn;

    private String environmentName;

    private Long created;

    private FeatureState featureState;

    @ApiModelProperty(ModelDescriptions.ClusterTemplateViewModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(ModelDescriptions.ClusterTemplateViewModelDescription.STACK_TYPE)
    private String stackType;

    @ApiModelProperty(ModelDescriptions.ClusterTemplateViewModelDescription.STACK_VERSION)
    private String stackVersion;

    public FeatureState getFeatureState() {
        return featureState;
    }

    public void setFeatureState(FeatureState featurseState) {
        this.featureState = featurseState;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DatalakeRequired getDatalakeRequired() {
        return datalakeRequired;
    }

    public void setDatalakeRequired(DatalakeRequired datalakeRequired) {
        this.datalakeRequired = datalakeRequired;
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
