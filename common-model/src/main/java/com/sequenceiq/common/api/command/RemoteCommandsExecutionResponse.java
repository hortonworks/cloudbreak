package com.sequenceiq.common.api.command;

import static com.sequenceiq.common.api.command.doc.RemoteCommandsExecutionDescription.RESULTS;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "RemoteCommandsExecutionResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoteCommandsExecutionResponse implements Serializable {

    @ApiModelProperty(RESULTS)
    @JsonProperty("results")
    private Map<String, String> results;

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }
}
