package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.BlueprintV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
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
