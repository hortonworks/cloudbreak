package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClusterV2Request implements JsonEntity {

    @ApiModelProperty(hidden = true)
    private String name;

    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG_NAME)
    private String ldapConfigName;

    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_NAMES)
    private Set<String> rdsConfigNames = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.PROXY_NAME)
    private String proxyName;

    @Valid
    @ApiModelProperty(StackModelDescription.CLOUD_STORAGE)
    private CloudStorageRequest cloudStorage;

    @ApiModelProperty(ClusterModelDescription.EXECUTOR_TYPE)
    private ExecutorType executorType = ExecutorType.DEFAULT;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_REQUEST)
    private AmbariV2Request ambari;

    @ApiModelProperty(ClusterModelDescription.SHARED_SERVICE_REQUEST)
    private SharedServiceRequest sharedService;

    public String getLdapConfigName() {
        return ldapConfigName;
    }

    public void setLdapConfigName(String ldapConfigName) {
        this.ldapConfigName = ldapConfigName;
    }

    public void setExecutorType(ExecutorType executorType) {
        this.executorType = executorType;
    }

    @JsonIgnore
    public String getName() {
        return name;
    }

    @JsonIgnore
    public void setName(String name) {
        this.name = name;
    }

    public CloudStorageRequest getCloudStorage() {
        return cloudStorage;
    }

    public void setCloudStorage(CloudStorageRequest cloudStorage) {
        this.cloudStorage = cloudStorage;
    }

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public AmbariV2Request getAmbari() {
        return ambari;
    }

    public void setAmbari(AmbariV2Request ambari) {
        this.ambari = ambari;
    }

    public Set<String> getRdsConfigNames() {
        return rdsConfigNames;
    }

    public void setRdsConfigNames(Set<String> rdsConfigNames) {
        this.rdsConfigNames = rdsConfigNames;
    }

    public String getProxyName() {
        return proxyName;
    }

    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }

    public SharedServiceRequest getSharedService() {
        return sharedService;
    }

    public void setSharedService(SharedServiceRequest sharedService) {
        this.sharedService = sharedService;
    }
}
