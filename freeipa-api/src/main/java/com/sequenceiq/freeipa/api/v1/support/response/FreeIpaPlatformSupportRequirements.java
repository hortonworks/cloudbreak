package com.sequenceiq.freeipa.api.v1.support.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.support.AbstractPlatformSupportRequirements;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaPlatformSupportRequirements extends AbstractPlatformSupportRequirements {
}