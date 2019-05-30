package com.sequenceiq.freeipa.api.v1.ldap.model.describe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.freeipa.api.v1.ldap.model.LdapConfigBase;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("DescribeLdapConfigV1Response")
@JsonInclude(Include.NON_NULL)
public class DescribeLdapConfigResponse extends LdapConfigBase {

    @ApiModelProperty(ModelDescriptions.CRN)
    private String crn;

    @ApiModelProperty(LdapConfigModelDescription.BIND_DN)
    private SecretResponse bindDn;

    @ApiModelProperty(LdapConfigModelDescription.BIND_PASSWORD)
    private SecretResponse bindPassword;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
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
