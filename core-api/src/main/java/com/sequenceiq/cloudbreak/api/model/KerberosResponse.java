package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.type.KerberosType;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KerberosResponse extends KerberosBase {

    private KerberosType type;

    public KerberosType getType() {
        return type;
    }

    public void setType(KerberosType type) {
        this.type = type;
    }
}
