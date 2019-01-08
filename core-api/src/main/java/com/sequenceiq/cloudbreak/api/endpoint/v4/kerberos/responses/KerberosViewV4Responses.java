package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("KerberosViewV4Responses")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KerberosViewV4Responses {

    private Set<KerberosViewV4Response> kerberosConfigs = new HashSet<>();

    public Set<KerberosViewV4Response> getKerberosConfigs() {
        return kerberosConfigs;
    }

    public void setKerberosConfigs(Set<KerberosViewV4Response> kerberosConfigs) {
        this.kerberosConfigs = kerberosConfigs;
    }

    public static final KerberosViewV4Responses kerberosViewResponses(Set<KerberosViewV4Response> kerberosConfigs) {
        KerberosViewV4Responses kerberosViewV4Responses = new KerberosViewV4Responses();
        kerberosViewV4Responses.setKerberosConfigs(kerberosConfigs);
        return kerberosViewV4Responses;
    }

}
