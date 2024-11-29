package com.sequenceiq.freeipa.api.v1.ldap.model.describe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.freeipa.api.v1.ldap.model.LdapConfigBase;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DescribeLdapConfigV1Response")
@JsonInclude(Include.NON_NULL)
public class DescribeLdapConfigResponse extends LdapConfigBase {

    @Schema(description = ModelDescriptions.CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String crn;

    @Schema(description = LdapConfigModelDescription.BIND_DN)
    private SecretResponse bindDn;

    @Schema(description = LdapConfigModelDescription.BIND_PASSWORD)
    private SecretResponse bindPassword;

    @Schema(description = LdapConfigModelDescription.USER_GROUP)
    private String userGroup;

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

    public String getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(String userGroup) {
        this.userGroup = userGroup;
    }
}
