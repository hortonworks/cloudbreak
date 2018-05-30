package com.sequenceiq.cloudbreak.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredParameterQueriesResponse implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.ENTRIES, required = true)
    private List<StructuredParameterQueryResponse> entries;

    public List<StructuredParameterQueryResponse> getEntries() {
        return entries;
    }

    public void setEntries(List<StructuredParameterQueryResponse> entries) {
        this.entries = entries;
    }
}
