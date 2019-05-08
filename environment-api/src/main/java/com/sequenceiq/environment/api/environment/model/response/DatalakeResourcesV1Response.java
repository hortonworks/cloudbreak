package com.sequenceiq.environment.api.environment.model.response;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DatalakeResourcesV1Response {
    @ApiModelProperty(EnvironmentModelDescription.AMBARI_URL)
    private String ambariUrl;

    @ApiModelProperty(EnvironmentModelDescription.LDAP_CONFIG_NAME)
    private String ldapName;

    @ApiModelProperty(EnvironmentModelDescription.RDSCONFIG_NAMES)
    private Set<String> databaseNames;

    @ApiModelProperty(EnvironmentModelDescription.KERBEROSCONFIG_NAME)
    private String kerberosName;

    @ApiModelProperty(EnvironmentModelDescription.SERVICE_DESCRIPTORS)
    private Map<String, ServiceDescriptorV1Response> serviceDescriptorMap;

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

    public Map<String, ServiceDescriptorV1Response> getServiceDescriptorMap() {
        return serviceDescriptorMap;
    }

    public void setServiceDescriptorMap(Map<String, ServiceDescriptorV1Response> serviceDescriptorMap) {
        this.serviceDescriptorMap = serviceDescriptorMap;
    }
}
