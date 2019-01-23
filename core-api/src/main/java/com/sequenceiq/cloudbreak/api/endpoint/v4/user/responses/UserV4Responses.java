package com.sequenceiq.cloudbreak.api.endpoint.v4.user.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserV4Responses extends GeneralCollectionV4Response<UserV4Response> {
    public UserV4Responses(Set<UserV4Response> responses) {
        super(responses);
    }

    public UserV4Responses() {
        super(Sets.newHashSet());
    }
}
