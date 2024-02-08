package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_COUNT;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_SIZE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_TYPE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VolumeV4Response implements JsonEntity {

    @Schema(description = VOLUME_COUNT)
    private Integer count;

    @Schema(description = VOLUME_TYPE)
    private String type;

    @Schema(description = VOLUME_SIZE)
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
        return "VolumeV4Response{" +
                "count=" + count +
                ", type='" + type + '\'' +
                ", size=" + size +
                '}';
    }
}
