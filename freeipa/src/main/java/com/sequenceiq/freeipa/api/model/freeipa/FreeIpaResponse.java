package com.sequenceiq.freeipa.api.model.freeipa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaResponse extends FreeIpaBase {
}
