package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.validation.ValidCredentialRequest;
import com.wordnik.swagger.annotations.ApiModel;

@ApiModel
@ValidCredentialRequest
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialRequest extends CredentialBase {
}
