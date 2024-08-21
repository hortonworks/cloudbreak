package com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.response;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A set of multiple database server SSL certificate responses")
public class StackDatabaseServerCertificateStatusV4Responses extends GeneralCollectionV4Response<StackDatabaseServerCertificateStatusV4Response> {
    public StackDatabaseServerCertificateStatusV4Responses(Set<StackDatabaseServerCertificateStatusV4Response> responses) {
        super(responses);
    }

    public StackDatabaseServerCertificateStatusV4Responses() {
        super(Sets.newHashSet());
    }

    @Override
    public String toString() {
        return "StackDatabaseServerCertificateStatusV4Responses{" + getResponses() + "}";
    }
}
