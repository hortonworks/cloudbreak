package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.AmbariV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.customcontainer.CustomContainerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class ClusterV4Response implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @ApiModelProperty(ClusterModelDescription.STATUS)
    private Status status;

    @ApiModelProperty(ClusterModelDescription.HOURS)
    private int hoursUp;

    @ApiModelProperty(ClusterModelDescription.MINUTES)
    private int minutesUp;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(ClusterModelDescription.STATUS_REASON)
    private String statusReason;

    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG)
    private LdapV4Response ldap;

    @ApiModelProperty(ClusterModelDescription.DATABASES)
    private List<DatabaseV4Response> databases;

    @ApiModelProperty(ClusterModelDescription.PROXY_NAME)
    private ProxyV4Response proxy;

    @ApiModelProperty(ClusterModelDescription.FILESYSTEM)
    private CloudStorageV4Response cloudStorage;

    private AmbariV4Response ambari;

    private GatewayV4Response gateway;

    @ApiModelProperty(ClusterModelDescription.CLUSTER_ATTRIBUTES)
    private Map<String, Object> attributes = new HashMap<>();

    @ApiModelProperty(ClusterModelDescription.CUSTOM_CONTAINERS)
    private CustomContainerV4Response customContainers;

    @ApiModelProperty(ClusterModelDescription.CUSTOM_QUEUE)
    private String customQueue;

    @ApiModelProperty(ClusterModelDescription.CREATION_FINISHED)
    private Long creationFinished;

    @ApiModelProperty(ClusterModelDescription.UPTIME)
    private Long uptime;

    @ApiModelProperty
    private KerberosV4Response kerberos;

    @ApiModelProperty(ClusterModelDescription.CLUSTER_EXPOSED_SERVICES)
    private Map<String, Collection<ClusterExposedServiceV4Response>> exposedServices;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getHoursUp() {
        return hoursUp;
    }

    public void setHoursUp(int hoursUp) {
        this.hoursUp = hoursUp;
    }

    public int getMinutesUp() {
        return minutesUp;
    }

    public void setMinutesUp(int minutesUp) {
        this.minutesUp = minutesUp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public LdapV4Response getLdap() {
        return ldap;
    }

    public void setLdap(LdapV4Response ldap) {
        this.ldap = ldap;
    }

    public CloudStorageV4Response getCloudStorage() {
        return cloudStorage;
    }

    public void setCloudStorage(CloudStorageV4Response cloudStorage) {
        this.cloudStorage = cloudStorage;
    }

    public AmbariV4Response getAmbari() {
        return ambari;
    }

    public void setAmbari(AmbariV4Response ambari) {
        this.ambari = ambari;
    }

    public GatewayV4Response getGateway() {
        return gateway;
    }

    public void setGateway(GatewayV4Response gateway) {
        this.gateway = gateway;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getCustomQueue() {
        return customQueue;
    }

    public void setCustomQueue(String customQueue) {
        this.customQueue = customQueue;
    }

    public Long getCreationFinished() {
        return creationFinished;
    }

    public void setCreationFinished(Long creationFinished) {
        this.creationFinished = creationFinished;
    }

    public Long getUptime() {
        return uptime;
    }

    public void setUptime(Long uptime) {
        this.uptime = uptime;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

    public List<DatabaseV4Response> getDatabases() {
        return databases;
    }

    public void setDatabases(List<DatabaseV4Response> databases) {
        this.databases = databases;
    }

    public ProxyV4Response getProxy() {
        return proxy;
    }

    public void setProxy(ProxyV4Response proxy) {
        this.proxy = proxy;
    }

    public CustomContainerV4Response getCustomContainers() {
        return customContainers;
    }

    public void setCustomContainers(CustomContainerV4Response customContainers) {
        this.customContainers = customContainers;
    }

    public KerberosV4Response getKerberos() {
        return kerberos;
    }

    public void setKerberos(KerberosV4Response kerberos) {
        this.kerberos = kerberos;
    }

    public Map<String, Collection<ClusterExposedServiceV4Response>> getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(Map<String, Collection<ClusterExposedServiceV4Response>> exposedServices) {
        this.exposedServices = exposedServices;
    }
}
