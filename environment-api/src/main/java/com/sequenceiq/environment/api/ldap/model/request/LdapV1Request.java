package com.sequenceiq.environment.api.ldap.model.request;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.environment.api.ldap.model.LdapV1Base;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapV1Request extends LdapV1Base {

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
