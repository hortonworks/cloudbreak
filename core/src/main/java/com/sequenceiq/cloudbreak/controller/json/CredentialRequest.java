package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wordnik.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialRequest extends CredentialBase {
}
