package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
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
}
