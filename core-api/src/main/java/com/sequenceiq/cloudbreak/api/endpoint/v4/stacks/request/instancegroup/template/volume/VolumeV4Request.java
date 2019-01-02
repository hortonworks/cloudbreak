package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_COUNT;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_TYPE;

import io.swagger.annotations.ApiModelProperty;

public class VolumeV4Request extends RootVolumeV4Request {

    @ApiModelProperty(VOLUME_COUNT)
    private Integer count;

    @ApiModelProperty(VOLUME_TYPE)
    private String type;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
