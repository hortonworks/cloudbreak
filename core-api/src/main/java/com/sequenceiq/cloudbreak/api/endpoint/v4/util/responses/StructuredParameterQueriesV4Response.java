package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredParameterQueriesV4Response implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.ENTRIES, required = true)
    private List<StructuredParameterQueryV4Response> entries;

    public List<StructuredParameterQueryV4Response> getEntries() {
        return entries;
    }

    public void setEntries(List<StructuredParameterQueryV4Response> entries) {
        this.entries = entries;
    }
}
