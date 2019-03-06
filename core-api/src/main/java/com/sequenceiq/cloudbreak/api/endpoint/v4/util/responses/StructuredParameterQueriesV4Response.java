package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StructuredParameterQueriesV4Response implements JsonEntity {

    @ApiModelProperty(value = StackModelDescription.ENTRIES, required = true)
    private List<StructuredParameterQueryV4Response> entries;

    public List<StructuredParameterQueryV4Response> getEntries() {
        return entries;
    }

    public void setEntries(List<StructuredParameterQueryV4Response> entries) {
        this.entries = entries;
    }
}
