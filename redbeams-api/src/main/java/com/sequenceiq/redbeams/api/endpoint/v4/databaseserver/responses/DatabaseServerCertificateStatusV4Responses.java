package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.GeneralCollectionV4Response;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_SERVER_CERTIFICATE_RESPONSES)
public class DatabaseServerCertificateStatusV4Responses extends GeneralCollectionV4Response<DatabaseServerCertificateStatusV4Response> {
    public DatabaseServerCertificateStatusV4Responses(Set<DatabaseServerCertificateStatusV4Response> responses) {
        super(responses);
    }

    public DatabaseServerCertificateStatusV4Responses() {
        super(Sets.newHashSet());
    }
}
