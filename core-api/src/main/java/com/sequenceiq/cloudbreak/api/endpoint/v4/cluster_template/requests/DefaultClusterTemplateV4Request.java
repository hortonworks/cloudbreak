package com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.DatalakeRequired;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DefaultClusterTemplateV4Request extends ClusterTemplateV4Request {

    @ApiModelProperty(value = ModelDescriptions.ClusterTemplateModelDescription.DATALAKE_REQUIRED,
            allowableValues = "NONE,OPTIONAL,REQUIRED")
    private DatalakeRequired datalakeRequired = DatalakeRequired.OPTIONAL;

    public DatalakeRequired getDatalakeRequired() {
        return datalakeRequired;
    }

    public void setDatalakeRequired(DatalakeRequired datalakeRequired) {
        this.datalakeRequired = datalakeRequired;
    }
}
