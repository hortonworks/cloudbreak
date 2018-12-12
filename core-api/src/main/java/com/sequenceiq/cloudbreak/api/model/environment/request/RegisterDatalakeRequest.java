package com.sequenceiq.cloudbreak.api.model.environment.request;

import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RegisterDatalakeRequest {
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.LDAP_CONFIG_NAME)
    private String ldapName;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.RDSCONFIG_NAMES)
    private Set<String> rdsNames;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.KERBEROSCONFIG_NAME)
    private String kerberosName;

    public String getLdapName() {
        return ldapName;
    }

    public void setLdapName(String ldapName) {
        this.ldapName = ldapName;
    }

    public Set<String> getRdsNames() {
        return rdsNames;
    }

    public void setRdsNames(Set<String> rdsNames) {
        this.rdsNames = rdsNames;
    }

    public String getKerberosName() {
        return kerberosName;
    }

    public void setKerberosName(String kerberosName) {
        this.kerberosName = kerberosName;
    }
}
