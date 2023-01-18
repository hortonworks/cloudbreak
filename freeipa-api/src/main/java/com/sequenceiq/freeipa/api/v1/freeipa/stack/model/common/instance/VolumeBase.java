package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.TemplateModelDescription;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class VolumeBase implements Serializable {
    @Schema(description = TemplateModelDescription.VOLUME_COUNT)
    private Integer count;

    @Schema(description = TemplateModelDescription.VOLUME_TYPE)
    private String type;

    @Schema(description = TemplateModelDescription.VOLUME_SIZE, required = true)
    private Integer size;

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

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

    @Override
    public String toString() {
        return "VolumeBase{" +
                "count=" + count +
                ", type='" + type + '\'' +
                ", size=" + size +
                '}';
    }
}
