package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class OrchestratorBase implements JsonEntity {
    @ApiModelProperty(value = ModelDescriptions.OrchestratorModelDescription.PARAMETERS)
    private Map<String, Object> parameters = new HashMap<>();
    @ApiModelProperty(value = ModelDescriptions.OrchestratorModelDescription.ENDPOINT)
    private String apiEndpoint;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.OrchestratorModelDescription.TYPE, required = true)
    private String type;

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
