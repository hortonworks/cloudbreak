package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class BlueprintV4Response extends BlueprintV4Base {

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    @Size(max = 100, min = 1, message = "The length of the blueprint's name has to be in range of 1 to 100 and should not contain semicolon "
            + "and percentage character.")
    @NotNull
    @Pattern(regexp = "^[^;\\/%]*$")
    private String name;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CRN)
    private String crn;

    @ApiModelProperty(BlueprintModelDescription.HOST_GROUP_COUNT)
    private Integer hostGroupCount;

    @ApiModelProperty(BlueprintModelDescription.STATUS)
    private ResourceStatus status;

    private Long created;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getHostGroupCount() {
        return hostGroupCount;
    }

    public void setHostGroupCount(Integer hostGroupCount) {
        this.hostGroupCount = hostGroupCount;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
