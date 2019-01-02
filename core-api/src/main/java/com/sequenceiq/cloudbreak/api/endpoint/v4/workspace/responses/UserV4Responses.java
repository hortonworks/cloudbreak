package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserV4Responses extends GeneralCollectionV4Response<UserV4Response> {
    public UserV4Responses(Set<UserV4Response> responses) {
        super(responses);
    }

    public UserV4Responses() {
        super(Sets.newHashSet());
    }
}
