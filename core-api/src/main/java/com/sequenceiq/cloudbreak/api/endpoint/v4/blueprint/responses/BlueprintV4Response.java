package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;

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

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(BlueprintModelDescription.HOST_GROUP_COUNT)
    private Integer hostGroupCount;

    @ApiModelProperty(BlueprintModelDescription.STATUS)
    private ResourceStatus status;

    public Integer getHostGroupCount() {
        return hostGroupCount;
    }

    public void setHostGroupCount(Integer hostGroupCount) {
        this.hostGroupCount = hostGroupCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

}
