package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.CloudbreakDetailsModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudbreakDetailsV4Response implements JsonEntity {

    @Schema(description = CloudbreakDetailsModelDescription.VERSION)
    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
