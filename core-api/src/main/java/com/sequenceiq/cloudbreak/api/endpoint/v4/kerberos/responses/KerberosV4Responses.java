package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class KerberosV4Responses extends GeneralCollectionV4Response<KerberosV4Response> {

    public KerberosV4Responses(Set<KerberosV4Response> responses) {
        super(responses);
    }

    public KerberosV4Responses() {
        super(Sets.newHashSet());
    }
}
