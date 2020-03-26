package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public class CompactViewV4Response implements JsonEntity {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @Size(max = 100, min = 5, message = "The length of the resource's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The resource's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(ModelDescriptions.CRN)
    private String crn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }
}
