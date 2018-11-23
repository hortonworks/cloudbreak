package com.sequenceiq.cloudbreak.api.model.ldap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.LdapConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LdapConfigResponse extends LdapConfigBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount = true;

    @ApiModelProperty(value = LdapConfigModelDescription.BIND_DN)
    private SecretResponse bindDn;

    @ApiModelProperty(value = LdapConfigModelDescription.BIND_PASSWORD)
    private SecretResponse bindPassword;

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecretResponse getBindDn() {
        return bindDn;
    }

    public void setBindDn(SecretResponse bindDn) {
        this.bindDn = bindDn;
    }

    public SecretResponse getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(SecretResponse bindPassword) {
        this.bindPassword = bindPassword;
    }
}
