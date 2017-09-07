package com.sequenceiq.cloudbreak.api.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecialParametersJson implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.SPECIAL_PARAMETERS)
    private Map<String, Boolean> specialParameters;

    public Map<String, Boolean> getSpecialParameters() {
        return specialParameters;
    }

    public void setSpecialParameters(Map<String, Boolean> specialParameters) {
        this.specialParameters = specialParameters;
    }
}
