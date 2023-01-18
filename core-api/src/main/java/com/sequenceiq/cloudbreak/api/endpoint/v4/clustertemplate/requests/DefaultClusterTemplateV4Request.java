package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterTemplateModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DefaultClusterTemplateV4Request extends ClusterTemplateV4Request {

    @Schema(description = ClusterTemplateModelDescription.DATALAKE_REQUIRED)
    private DatalakeRequired datalakeRequired = DatalakeRequired.OPTIONAL;

    private FeatureState featureState;

    public FeatureState getFeatureState() {
        return featureState;
    }

    public void setFeatureState(FeatureState featureState) {
        this.featureState = featureState;
    }

    public DatalakeRequired getDatalakeRequired() {
        return datalakeRequired;
    }

    public void setDatalakeRequired(DatalakeRequired datalakeRequired) {
        this.datalakeRequired = datalakeRequired;
    }
}
