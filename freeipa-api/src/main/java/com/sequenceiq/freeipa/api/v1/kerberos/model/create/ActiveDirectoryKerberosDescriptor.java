package com.sequenceiq.freeipa.api.v1.kerberos.model.create;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigModelDescription;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ActiveDirectoryKerberosV1Descriptor")
public class ActiveDirectoryKerberosDescriptor extends KerberosDescriptorBase {
    @ApiModelProperty(value = KerberosConfigModelDescription.KERBEROS_URL, required = true)
    @NotNull
    @NotEmpty
    private String url;

    @ApiModelProperty(value = KerberosConfigModelDescription.KERBEROS_ADMIN_URL, required = true)
    @NotNull
    @NotEmpty
    private String adminUrl;

    @ApiModelProperty(value = KerberosConfigModelDescription.KERBEROS_REALM, required = true)
    @NotNull
    @NotEmpty
    private String realm;

    @ApiModelProperty(value = KerberosConfigModelDescription.KERBEROS_LDAP_URL, required = true)
    @NotNull
    @NotEmpty
    private String ldapUrl;

    @ApiModelProperty(value = KerberosConfigModelDescription.KERBEROS_CONTAINER_DN, required = true)
    @NotNull
    @NotEmpty
    private String containerDn;

    @ApiModelProperty(hidden = true)
    @Override
    public KerberosType getType() {
        return KerberosType.ACTIVE_DIRECTORY;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getContainerDn() {
        return containerDn;
    }

    public void setContainerDn(String containerDn) {
        this.containerDn = containerDn;
    }
}
