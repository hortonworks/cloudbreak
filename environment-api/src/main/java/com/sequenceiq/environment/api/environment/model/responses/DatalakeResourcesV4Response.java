package com.sequenceiq.environment.api.environment.model.responses;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.environment.api.environment.doc.EnvironmentDatalakeDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DatalakeResourcesV4Response {
    @ApiModelProperty(EnvironmentDatalakeDescription.AMBARI_URL)
    private String ambariUrl;

    @ApiModelProperty(EnvironmentDatalakeDescription.LDAP_CONFIG_NAME)
    private String ldapName;

    @ApiModelProperty(EnvironmentDatalakeDescription.RDSCONFIG_NAMES)
    private Set<String> databaseNames;

    @ApiModelProperty(EnvironmentDatalakeDescription.KERBEROSCONFIG_NAME)
    private String kerberosName;

    @ApiModelProperty(EnvironmentDatalakeDescription.SERVICE_DESCRIPTORS)
    private Map<String, ServiceDescriptorV4Response> serviceDescriptorMap;

    public String getAmbariUrl() {
        return ambariUrl;
    }

    public void setAmbariUrl(String ambariUrl) {
        this.ambariUrl = ambariUrl;
    }

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

    public Map<String, ServiceDescriptorV4Response> getServiceDescriptorMap() {
        return serviceDescriptorMap;
    }

    public void setServiceDescriptorMap(Map<String, ServiceDescriptorV4Response> serviceDescriptorMap) {
        this.serviceDescriptorMap = serviceDescriptorMap;
    }
}
