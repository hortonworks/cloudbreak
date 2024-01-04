package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterTemplateModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClusterTemplateV4Response extends ClusterTemplateV4Base {

    @Schema(description = ModelDescriptions.ID)
    private Long id;

    private ResourceStatus status;

    @Schema(description = ClusterTemplateModelDescription.DATALAKE_REQUIRED)
    private DatalakeRequired datalakeRequired = DatalakeRequired.OPTIONAL;

    private String environmentCrn;

    private String environmentName;

    private Long created;

    private Long lastUpdated;

    private FeatureState featureState;

    @Schema(description = ModelDescriptions.ClusterTemplateViewModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @Schema(description = ModelDescriptions.ClusterTemplateViewModelDescription.STACK_TYPE)
    private String stackType;

    @Schema(description = ModelDescriptions.ClusterTemplateViewModelDescription.STACK_VERSION)
    private String stackVersion;

    @NotNull
    @Schema(description = ModelDescriptions.CRN)
    private String crn;

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

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
