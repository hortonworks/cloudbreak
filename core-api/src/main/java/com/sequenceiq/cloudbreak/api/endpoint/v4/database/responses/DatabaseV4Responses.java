package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class DatabaseV4Responses extends GeneralSetV4Response<DatabaseV4Response> {
    public DatabaseV4Responses(Set<DatabaseV4Response> responses) {
        super(responses);
    }
}
