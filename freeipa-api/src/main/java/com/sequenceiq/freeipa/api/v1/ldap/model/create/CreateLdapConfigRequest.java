package com.sequenceiq.freeipa.api.v1.ldap.model.create;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.freeipa.api.v1.ldap.model.LdapConfigBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CreateLdapConfigV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateLdapConfigRequest extends LdapConfigBase {
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
