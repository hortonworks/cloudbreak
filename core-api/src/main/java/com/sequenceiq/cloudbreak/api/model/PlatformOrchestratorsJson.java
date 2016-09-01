package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformOrchestratorsJson implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.ORCHESTRATORS)
    private Map<String, Collection<String>> orchestrators;
    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.DEFAULT_ORCHESTRATORS)
    private Map<String, String> defaults;

    public Map<String, Collection<String>> getOrchestrators() {
        return orchestrators;
    }

    public void setOrchestrators(Map<String, Collection<String>> orchestrators) {
        this.orchestrators = orchestrators;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults;
    }
}
