package com.sequenceiq.environment.api.environment.model.requests;

import java.util.Set;

import com.sequenceiq.environment.api.environment.doc.EnvironmentDatalakeDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RegisterDatalakeV4Request {
    @ApiModelProperty(EnvironmentDatalakeDescription.LDAP_CONFIG_NAME)
    private String ldapName;

    @ApiModelProperty(EnvironmentDatalakeDescription.RDSCONFIG_NAMES)
    private Set<String> databaseNames;

    @ApiModelProperty(EnvironmentDatalakeDescription.KERBEROSCONFIG_NAME)
    private String kerberosName;

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
