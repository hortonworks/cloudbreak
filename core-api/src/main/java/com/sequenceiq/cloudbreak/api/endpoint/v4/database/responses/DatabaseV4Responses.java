package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class DatabaseV4Responses extends GeneralCollectionV4Response<DatabaseV4Response> {
    public DatabaseV4Responses(Set<DatabaseV4Response> responses) {
        super(responses);
    }

    public DatabaseV4Responses() {
        super(Sets.newHashSet());
    }
}
