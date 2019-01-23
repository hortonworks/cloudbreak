package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_SIZE;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class RootVolumeV4Request implements JsonEntity {

    @ApiModelProperty(value = VOLUME_SIZE, required = true)
    private Integer size;

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

}
