package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileSystemParametersV4Response implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.ENTRIES, required = true)
    private List<FileSystemParameterV4Response> entries;

    public List<FileSystemParameterV4Response> getEntries() {
        return entries;
    }

    public void setEntries(List<FileSystemParameterV4Response> entries) {
        this.entries = entries;
    }
}
