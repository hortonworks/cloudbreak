package com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.ClusterTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterTemplateV4Response extends ClusterTemplateV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    private ResourceStatus status;

    @ApiModelProperty(value = ModelDescriptions.ClusterTemplateModelDescription.DATALAKE_REQUIRED,
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
