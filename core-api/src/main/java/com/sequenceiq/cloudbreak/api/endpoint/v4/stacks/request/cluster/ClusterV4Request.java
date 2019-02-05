package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.customcontainer.CustomContainerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClusterV4Request implements JsonEntity {

    @ApiModelProperty(hidden = true)
    private String name;

    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG_NAME)
    private String ldapName;

    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_NAMES)
    private Set<String> databases = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.PROXY_NAME)
    private String proxyName;

    @Valid
    @ApiModelProperty(StackModelDescription.CLOUD_STORAGE)
    private CloudStorageV4Request cloudStorage;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_REQUEST)
    private AmbariV4Request ambari;

    private GatewayV4Request gateway;

    @Valid
    private String kerberosName;

    @ApiModelProperty(ClusterModelDescription.CUSTOM_CONTAINERS)
    private CustomContainerV4Request customContainer;

    @ApiModelProperty(ClusterModelDescription.CUSTOM_QUEUE)
    private String customQueue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<String> databases) {
        this.databases = databases;
    }

    public String getProxyName() {
        return proxyName;
    }

    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }

    public CloudStorageV4Request getCloudStorage() {
        return cloudStorage;
    }

    public void setCloudStorage(CloudStorageV4Request cloudStorage) {
        this.cloudStorage = cloudStorage;
    }

    public AmbariV4Request getAmbari() {
        return ambari;
    }

    public void setAmbari(AmbariV4Request ambari) {
        this.ambari = ambari;
    }

    public GatewayV4Request getGateway() {
        return gateway;
    }

    public void setGateway(GatewayV4Request gateway) {
        this.gateway = gateway;
    }

    public String getLdapName() {
        return ldapName;
    }

    public void setLdapName(String ldapName) {
        this.ldapName = ldapName;
    }

    public String getKerberosName() {
        return kerberosName;
    }

    public void setKerberosName(String kerberosName) {
        this.kerberosName = kerberosName;
    }

    public String getCustomQueue() {
        return customQueue;
    }

    public void setCustomQueue(String customQueue) {
        this.customQueue = customQueue;
    }

    public CustomContainerV4Request getCustomContainer() {
        return customContainer;
    }

    public void setCustomContainer(CustomContainerV4Request customContainer) {
        this.customContainer = customContainer;
    }
}
