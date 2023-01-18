package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.credential;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialRequest extends CredentialBase {

}
