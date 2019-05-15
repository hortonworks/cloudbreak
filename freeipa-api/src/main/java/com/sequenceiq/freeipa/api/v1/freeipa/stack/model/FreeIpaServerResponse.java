package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("FreeIpaServerV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaServerResponse extends FreeIpaServerBase {
}
