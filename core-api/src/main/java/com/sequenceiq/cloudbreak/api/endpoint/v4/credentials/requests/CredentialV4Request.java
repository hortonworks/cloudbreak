package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.CredentialV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel(description = EnvironmentRequestModelDescription.CREDENTIAL_DESCRIPTION, parent = CredentialV4Base.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CredentialV4Request extends CredentialV4Base {
}
