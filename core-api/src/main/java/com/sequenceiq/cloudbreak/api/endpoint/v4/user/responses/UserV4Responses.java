package com.sequenceiq.cloudbreak.api.endpoint.v4.user.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserV4Response;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserV4Responses extends GeneralSetV4Response<UserV4Response> {
    public UserV4Responses(Set<UserV4Response> responses) {
        super(responses);
    }
}
