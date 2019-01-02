package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_SIZE;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class RootVolumeV4Response implements JsonEntity {

    @ApiModelProperty(VOLUME_SIZE)
    private Integer size;

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
