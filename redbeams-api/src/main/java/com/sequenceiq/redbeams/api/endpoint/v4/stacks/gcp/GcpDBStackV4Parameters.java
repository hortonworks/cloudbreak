package com.sequenceiq.redbeams.api.endpoint.v4.stacks.gcp;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GcpDBStackV4Parameters extends MappableBase {

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return GCP;
    }
}
