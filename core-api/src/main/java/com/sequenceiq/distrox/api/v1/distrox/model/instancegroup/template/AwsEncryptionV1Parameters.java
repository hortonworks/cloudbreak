package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsEncryptionV1Parameters extends EncryptionParametersV1Base {

}
