package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.logging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.LoggingV4Base;
import com.sequenceiq.cloudbreak.validation.ValidLoggingV4Request;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidLoggingV4Request
public class LoggingV4Request extends LoggingV4Base {
}
