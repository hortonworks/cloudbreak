package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.GeneralCollectionV4Response;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;

@ApiModel(description = ModelDescriptions.DATABASE_SERVER_RESPONSES)
public class DatabaseServerV4Responses extends GeneralCollectionV4Response<DatabaseServerV4Response> {
    public DatabaseServerV4Responses(Set<DatabaseServerV4Response> responses) {
        super(responses);
    }

    public DatabaseServerV4Responses() {
        super(Sets.newHashSet());
    }
}
