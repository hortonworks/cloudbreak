package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.ROOT_VOLUME_SIZE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RootVolumeV4Response implements JsonEntity {

    @Schema(description = ROOT_VOLUME_SIZE)
    private Integer size;

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
