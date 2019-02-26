package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterTemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DefaultClusterTemplateV4Request extends ClusterTemplateV4Request {

    @ApiModelProperty(value = ClusterTemplateModelDescription.DATALAKE_REQUIRED,
            allowableValues = "NONE,OPTIONAL,REQUIRED")
    private DatalakeRequired datalakeRequired = DatalakeRequired.OPTIONAL;

    public DatalakeRequired getDatalakeRequired() {
        return datalakeRequired;
    }

    public void setDatalakeRequired(DatalakeRequired datalakeRequired) {
        this.datalakeRequired = datalakeRequired;
    }
}
