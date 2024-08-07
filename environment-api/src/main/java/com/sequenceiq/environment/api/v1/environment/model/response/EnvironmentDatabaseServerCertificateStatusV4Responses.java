package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.GeneralCollectionV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = EnvironmentModelDescription.DATABASE_SERVER_CERTIFICATE_RESPONSES)
public class EnvironmentDatabaseServerCertificateStatusV4Responses extends GeneralCollectionV4Response<EnvironmentDatabaseServerCertificateStatusV4Response> {
    public EnvironmentDatabaseServerCertificateStatusV4Responses(Set<EnvironmentDatabaseServerCertificateStatusV4Response> responses) {
        super(responses);
    }

    public EnvironmentDatabaseServerCertificateStatusV4Responses() {
        super(Sets.newHashSet());
    }

    @Override
    public String toString() {
        return "EnvironmentDatabaseServerCertificateStatusV4Responses{}";
    }
}
