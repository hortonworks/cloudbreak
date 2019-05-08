package com.sequenceiq.environment.api.environment.model.request;

import java.util.Set;

import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RegisterDatalakeV1Request {
    @ApiModelProperty(EnvironmentModelDescription.LDAP_CONFIG_NAME)
    private String ldapName;

    @ApiModelProperty(EnvironmentModelDescription.RDSCONFIG_NAMES)
    private Set<String> databaseNames;

    @ApiModelProperty(EnvironmentModelDescription.KERBEROSCONFIG_NAME)
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
