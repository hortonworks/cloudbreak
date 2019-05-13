package com.sequenceiq.environment.api.ldap.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.environment.api.ldap.model.LdapV1Base;
import com.sequenceiq.secret.model.SecretResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class LdapV1Response extends LdapV1Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(LdapConfigModelDescription.BIND_DN)
    private SecretResponse bindDn;

    @ApiModelProperty(LdapConfigModelDescription.BIND_PASSWORD)
    private SecretResponse bindPassword;

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
