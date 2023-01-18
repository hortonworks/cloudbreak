package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema
public class DurationV4Request extends DurationV4Base implements JsonEntity {
}
