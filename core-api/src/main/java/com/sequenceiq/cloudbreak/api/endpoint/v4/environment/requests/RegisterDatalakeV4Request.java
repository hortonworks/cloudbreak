package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RegisterDatalakeV4Request {
    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG_NAME)
    @NotEmpty
    private String ldapName;

    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_NAMES)
    @NotEmpty
    private Set<String> databaseNames;

    @ApiModelProperty(ClusterModelDescription.KERBEROSCONFIG_NAME)
    private String kerberosName;

    @NotEmpty
    private String rangerAdminPassword;

    public String getLdapName() {
        return ldapName;
    }

    public void setLdapName(String ldapName) {
        this.ldapName = ldapName;
    }

    public Set<String> getDatabaseNames() {
        return databaseNames;
    }

    public void setDatabaseNames(Set<String> databaseNames) {
        this.databaseNames = databaseNames;
    }

    public String getKerberosName() {
        return kerberosName;
    }

    public void setKerberosName(String kerberosName) {
        this.kerberosName = kerberosName;
    }

    public String getRangerAdminPassword() {
        return rangerAdminPassword;
    }

    public void setRangerAdminPassword(String rangerAdminPassword) {
        this.rangerAdminPassword = rangerAdminPassword;
    }
}
