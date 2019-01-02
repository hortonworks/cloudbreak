package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KerberosViewV4Responses extends GeneralSetV4Response<KerberosViewV4Response> {

    public KerberosViewV4Responses(Set<KerberosViewV4Response> responses) {
        super(responses);
    }
}
