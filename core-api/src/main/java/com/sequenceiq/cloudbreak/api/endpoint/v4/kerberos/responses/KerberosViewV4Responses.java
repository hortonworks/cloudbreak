package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KerberosViewV4Responses extends GeneralCollectionV4Response<KerberosViewV4Response> {

    public KerberosViewV4Responses(Set<KerberosViewV4Response> responses) {
        super(responses);
    }

    public KerberosViewV4Responses() {
        super(Sets.newHashSet());
    }
}
