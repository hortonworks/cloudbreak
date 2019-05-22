package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.type.KerberosType;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KerberosViewV4Response extends CompactViewV4Response {

    private KerberosType type;

    public KerberosType getType() {
        return type;
    }

    public void setType(KerberosType type) {
        this.type = type;
    }

}
