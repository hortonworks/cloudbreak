package com.sequenceiq.environment.api.ldap.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.api.SecretV4Response;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.environment.api.ldap.model.LdapV1Base;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class LdapV1Response extends LdapV1Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(LdapConfigModelDescription.BIND_DN)
    private SecretV4Response bindDn;

    @ApiModelProperty(LdapConfigModelDescription.BIND_PASSWORD)
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
