package com.sequenceiq.cloudbreak.domain.stack.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.MAINTENANCE_MODE_ENABLED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Secret;
import com.sequenceiq.cloudbreak.domain.SecretToString;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.type.KerberosType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Cluster implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cluster_generator")
    @SequenceGenerator(name = "cluster_generator", sequenceName = "cluster_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Stack stack;

    @Column(columnDefinition = "TEXT")
    private String variant;

    @ManyToOne
    @JoinColumn(name = "blueprint_id")
    private ClusterDefinition clusterDefinition;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private ExecutorType executorType;

    private Long creationStarted;

    private Long creationFinished;

    private Long upSince;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String statusReason;

    private String ambariIp;

    @Column(nullable = false)
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret userName = Secret.EMPTY;

    @Column(nullable = false)
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret password = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cloudbreakAmbariUser = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cloudbreakAmbariPassword = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret dpAmbariUser = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret dpAmbariPassword = Secret.EMPTY;

    @ManyToOne
    private KerberosConfig kerberosConfig;

    @Column(nullable = false)
    private Boolean topologyValidation = Boolean.TRUE;

    @Convert(converter = SecretToString.class)
    @SecretValue
    @Column(name = "extendedBlueprintText", nullable = false)
    private Secret extendedClusterDefinitionText = Secret.EMPTY;

    @OneToOne(mappedBy = "cluster", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private Gateway gateway;

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HostGroup> hostGroups = new HashSet<>();

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ClusterComponent> components = new HashSet<>();

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Container> containers = new HashSet<>();

    @ManyToMany
    private Set<RDSConfig> rdsConfigs;

    @ManyToOne
    private FileSystem fileSystem;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConfigStrategy configStrategy;

    @ManyToOne
    private LdapConfig ldapConfig;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret attributes = Secret.EMPTY;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json customContainerDefinition;

    private String uptime;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret ambariSecurityMasterKey = Secret.EMPTY;

    @ManyToOne
    private ProxyConfig proxyConfig;

    @ManyToOne
    private Workspace workspace;

    @ManyToOne
    private EnvironmentView environment;

    public EnvironmentView getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentView environment) {
        this.environment = environment;
    }

    public boolean hasGateway() {
        return gateway != null && gateway.isGatewayEnabled();
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public ClusterDefinition getClusterDefinition() {
        return clusterDefinition;
    }

    public void setClusterDefinition(ClusterDefinition clusterDefinition) {
        this.clusterDefinition = clusterDefinition;
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

    public Long getCreationStarted() {
        return creationStarted;
    }

    public void setCreationStarted(Long creationStarted) {
        this.creationStarted = creationStarted;
    }

    public Long getCreationFinished() {
        return creationFinished;
    }

    public void setCreationFinished(Long creationFinished) {
        this.creationFinished = creationFinished;
    }

    public Long getUpSince() {
        return upSince;
    }

    public void setUpSince(Long upSince) {
        this.upSince = upSince;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Set<HostGroup> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroup> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Set<Container> getContainers() {
        return containers;
    }

    public void setContainers(Set<Container> containers) {
        this.containers = containers;
    }

    public boolean isCreateFailed() {
        return Status.CREATE_FAILED.equals(status);
    }

    public Set<RDSConfig> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<RDSConfig> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getUserName() {
        return userName.getRaw();
    }

    public void setUserName(String userName) {
        this.userName = new Secret(userName);
    }

    public String getPassword() {
        return password.getRaw();
    }

    public void setPassword(String password) {
        this.password = new Secret(password);
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    public boolean isClusterReadyForStop() {
        return AVAILABLE.equals(status) || STOPPED.equals(status);
    }

    public boolean isAvailable() {
        return AVAILABLE.equals(status);
    }

    public boolean isMaintenanceModeEnabled() {
        return MAINTENANCE_MODE_ENABLED.equals(status);
    }

    public boolean isStopped() {
        return STOPPED.equals(status);
    }

    public boolean isStopFailed() {
        return STOP_FAILED.equals(status);
    }

    public boolean isStartFailed() {
        return START_FAILED.equals(status);
    }

    public boolean isStartRequested() {
        return START_REQUESTED.equals(status);
    }

    public boolean isStopInProgress() {
        return STOP_IN_PROGRESS.equals(status) || STOP_REQUESTED.equals(status);
    }

    public boolean isRequested() {
        return REQUESTED.equals(status);
    }

    public boolean isDeleteInProgress() {
        return DELETE_IN_PROGRESS.equals(status);
    }

    public boolean isDeleteCompleted() {
        return DELETE_COMPLETED.equals(status);
    }

    public boolean isClusterReadyForStart() {
        return STOPPED.equals(status) || START_REQUESTED.equals(status);
    }

    public boolean isModificationInProgress() {
        return CREATE_IN_PROGRESS.equals(status)
                || UPDATE_IN_PROGRESS.equals(status)
                || STOP_IN_PROGRESS.equals(status)
                || START_IN_PROGRESS.equals(status)
                || DELETE_IN_PROGRESS.equals(status);
    }

    public String getAttributes() {
        return attributes.getRaw();
    }

    public void setAttributes(String attributes) {
        this.attributes = new Secret(attributes);
    }

    public Gateway getGateway() {
        return hasGateway() ? gateway : null;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }

    public ConfigStrategy getConfigStrategy() {
        return configStrategy;
    }

    public void setConfigStrategy(ConfigStrategy configStrategy) {
        this.configStrategy = configStrategy;
    }

    public LdapConfig getLdapConfig() {
        return ldapConfig;
    }

    public void setLdapConfig(LdapConfig ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public String getCloudbreakAmbariUser() {
        return cloudbreakAmbariUser.getRaw();
    }

    public String getCloudbreakAmbariUserSecret() {
        return cloudbreakAmbariUser.getSecret();
    }

    public void setCloudbreakAmbariUser(String cloudbreakAmbariUser) {
        this.cloudbreakAmbariUser = new Secret(cloudbreakAmbariUser);
    }

    public String getCloudbreakAmbariPassword() {
        return cloudbreakAmbariPassword.getRaw();
    }

    public String getCloudbreakAmbariPasswordSecret() {
        return cloudbreakAmbariPassword.getSecret();
    }

    public void setCloudbreakAmbariPassword(String cloudbreakAmbariPassword) {
        this.cloudbreakAmbariPassword = new Secret(cloudbreakAmbariPassword);
    }

    public String getDpAmbariUser() {
        return dpAmbariUser.getRaw();
    }

    public String getDpAmbariUserSecret() {
        return dpAmbariUser.getSecret();
    }

    public void setDpAmbariUser(String dpAmbariUser) {
        this.dpAmbariUser = new Secret(dpAmbariUser);
    }

    public String getDpAmbariPassword() {
        return dpAmbariPassword.getRaw();
    }

    public String getDpAmbariPasswordSecret() {
        return dpAmbariPassword.getSecret();
    }

    public void setDpAmbariPassword(String dpAmbariPassword) {
        this.dpAmbariPassword = new Secret(dpAmbariPassword);
    }

    public KerberosConfig getKerberosConfig() {
        return kerberosConfig;
    }

    public void setKerberosConfig(KerberosConfig kerberosConfig) {
        this.kerberosConfig = kerberosConfig;
    }

    public Boolean getTopologyValidation() {
        return topologyValidation;
    }

    public void setTopologyValidation(Boolean topologyValidation) {
        this.topologyValidation = topologyValidation;
    }

    public Json getCustomContainerDefinition() {
        return customContainerDefinition;
    }

    public void setCustomContainerDefinition(Json customContainerDefinition) {
        this.customContainerDefinition = customContainerDefinition;
    }

    public Set<ClusterComponent> getComponents() {
        return components;
    }

    public void setComponents(Set<ClusterComponent> components) {
        this.components = components;
    }

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public void setExecutorType(ExecutorType executorType) {
        this.executorType = executorType;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public String getAmbariSecurityMasterKey() {
        return ambariSecurityMasterKey.getRaw();
    }

    public void setAmbariSecurityMasterKey(String ambariSecurityMasterKey) {
        this.ambariSecurityMasterKey = new Secret(ambariSecurityMasterKey);
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public String getExtendedClusterDefinitionText() {
        return extendedClusterDefinitionText.getRaw();
    }

    public void setExtendedClusterDefinitionText(String extendedClusterDefinitionText) {
        this.extendedClusterDefinitionText = new Secret(extendedClusterDefinitionText);
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STACK;
    }

    public boolean isAdJoinable() {
        return kerberosConfig != null && kerberosConfig.getType() == KerberosType.ACTIVE_DIRECTORY;
    }

    public boolean isIpaJoinable() {
        return kerberosConfig != null && kerberosConfig.getType() == KerberosType.FREEIPA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Cluster that = (Cluster) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
