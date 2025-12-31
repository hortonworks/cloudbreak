package com.sequenceiq.redbeams.api.endpoint.v4.support;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.support.AbstractPlatformSupportRequirements;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedBeamsPlatformSupportRequirements extends AbstractPlatformSupportRequirements {

}
