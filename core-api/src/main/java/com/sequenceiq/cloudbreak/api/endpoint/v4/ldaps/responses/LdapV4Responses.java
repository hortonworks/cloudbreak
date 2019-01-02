package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class LdapV4Responses extends GeneralSetV4Response<LdapV4Response> {
    public LdapV4Responses(Set<LdapV4Response> responses) {
        super(responses);
    }
}
