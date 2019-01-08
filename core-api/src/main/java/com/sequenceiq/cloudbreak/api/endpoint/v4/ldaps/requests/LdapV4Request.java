package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.LdapConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapV4Request extends LdapV4Base {

    @NotNull
    @ApiModelProperty(value = LdapConfigModelDescription.BIND_DN, required = true)
    private String bindDn;

    @NotNull
    @ApiModelProperty(value = LdapConfigModelDescription.BIND_PASSWORD, required = true)
    private String bindPassword;

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

}
