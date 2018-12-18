package com.sequenceiq.cloudbreak.api.model.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DefaultClusterTemplateRequest extends ClusterTemplateRequest {

    @ApiModelProperty(ModelDescriptions.ClusterTemplateModelDescription.DATALAKE_REQUIRED)
    private DatalakeRequired datalakeRequired;

    public DatalakeRequired getDatalakeRequired() {
        return datalakeRequired;
    }

    public void setDatalakeRequired(DatalakeRequired datalakeRequired) {
        this.datalakeRequired = datalakeRequired;
    }
}
