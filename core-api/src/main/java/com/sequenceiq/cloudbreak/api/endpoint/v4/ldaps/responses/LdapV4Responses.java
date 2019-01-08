package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel
public class LdapV4Responses {

    private Set<LdapV4Response> ldaps = new HashSet<>();

    public Set<LdapV4Response> getLdaps() {
        return ldaps;
    }

    public void setLdaps(Set<LdapV4Response> ldaps) {
        this.ldaps = ldaps;
    }

    public static final LdapV4Responses ldapV4Responses(Set<LdapV4Response> ldaps) {
        LdapV4Responses ldapV4Responses = new LdapV4Responses();
        ldapV4Responses.setLdaps(ldaps);
        return ldapV4Responses;
    }
}
