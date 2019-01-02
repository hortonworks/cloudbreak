package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class LdapV4Responses extends GeneralCollectionV4Response<LdapV4Response> {
    public LdapV4Responses(Set<LdapV4Response> responses) {
        super(responses);
    }

    public LdapV4Responses() {
        super(Sets.newHashSet());
    }
}
