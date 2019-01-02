package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.LdapConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LdapV4Response extends LdapV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(value = LdapConfigModelDescription.BIND_DN)
    private SecretV4Response bindDn;

    @ApiModelProperty(value = LdapConfigModelDescription.BIND_PASSWORD)
    private SecretV4Response bindPassword;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecretV4Response getBindDn() {
        return bindDn;
    }

    public void setBindDn(SecretV4Response bindDn) {
        this.bindDn = bindDn;
    }

    public SecretV4Response getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(SecretV4Response bindPassword) {
        this.bindPassword = bindPassword;
    }
}
