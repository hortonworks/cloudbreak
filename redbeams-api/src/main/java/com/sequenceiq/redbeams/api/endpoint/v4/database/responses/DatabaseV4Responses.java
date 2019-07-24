package com.sequenceiq.redbeams.api.endpoint.v4.database.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;

@ApiModel(description = ModelDescriptions.DATABASE_RESPONSES)
public class DatabaseV4Responses extends GeneralCollectionV4Response<DatabaseV4Response> {
    public DatabaseV4Responses(Set<DatabaseV4Response> responses) {
        super(responses);
    }

    public DatabaseV4Responses() {
        super(Sets.newHashSet());
    }
}
