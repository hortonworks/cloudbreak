package com.sequenceiq.cloudbreak.controller.json;

import org.codehaus.jackson.annotate.JsonProperty;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel
public class FileSystemResponse extends FileSystemBase{

    @ApiModelProperty(ModelDescriptions.ID)
    private String id;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
