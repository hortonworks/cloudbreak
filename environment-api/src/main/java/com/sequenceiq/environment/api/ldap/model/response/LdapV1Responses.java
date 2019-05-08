package com.sequenceiq.environment.api.ldap.model.response;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class LdapV1Responses extends GeneralCollectionV1Response<LdapV1Response> {
    public LdapV1Responses(Set<LdapV1Response> responses) {
        super(responses);
    }

    public LdapV1Responses() {
        super(Sets.newHashSet());
    }
}
