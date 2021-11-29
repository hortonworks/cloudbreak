package com.sequenceiq.cloudbreak.domain.stack.cluster;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.converter.CertExpirationStateConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.converter.ConfigStrategyConverter;
import com.sequenceiq.cloudbreak.domain.converter.ExecutorTypeConverter;
import com.sequenceiq.cloudbreak.domain.converter.StatusConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.common.api.type.CertExpirationState;

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
    private Blueprint blueprint;

    @ManyToOne(fetch = FetchType.LAZY)
    private CustomConfigurations customConfigurations;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Convert(converter = StatusConverter.class)
    private Status status;

    @Convert(converter = ExecutorTypeConverter.class)
    private ExecutorType executorType;

    private Long creationStarted;

    private Long creationFinished;

    private Long upSince;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String statusReason;

    private String clusterManagerIp;

    private String fqdn;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret userName = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret password = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cloudbreakAmbariUser = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cloudbreakClusterManagerUser = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cloudbreakClusterManagerMonitoringUser = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cdpNodeStatusMonitorUser = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cloudbreakAmbariPassword = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cloudbreakClusterManagerPassword = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cloudbreakClusterManagerMonitoringPassword = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cdpNodeStatusMonitorPassword = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret dpAmbariUser = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret dpClusterManagerUser = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret dpAmbariPassword = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret dpClusterManagerPassword = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret keyStorePwd = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret trustStorePwd = Secret.EMPTY;

    @Column(nullable = false)
    private Boolean topologyValidation = Boolean.TRUE;

    @Convert(converter = SecretToString.class)
    @SecretValue
    @Column(nullable = false)
    private Secret extendedBlueprintText = Secret.EMPTY;

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

    @ManyToOne
    private FileSystem additionalFileSystem;

    @Column(nullable = false)
    @Convert(converter = ConfigStrategyConverter.class)
    private ConfigStrategy configStrategy;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret attributes = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret databusCredential = Secret.EMPTY;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json customContainerDefinition;

    private String uptime;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret ambariSecurityMasterKey = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret clusterManagerSecurityMasterKey = Secret.EMPTY;

    @ManyToOne
    private Workspace workspace;

    private String environmentCrn;

    private String proxyConfigCrn;

    private String databaseServerCrn;

    @Column(nullable = false)
    private Boolean autoTlsEnabled = Boolean.FALSE;

    @Column(name = "ranger_raz_enabled")
    private boolean rangerRazEnabled;

    @Convert(converter = CertExpirationStateConverter.class)
    private CertExpirationState certExpirationState = CertExpirationState.VALID;

    @Column(nullable = false)
    private Boolean embeddedDatabaseOnAttachedDisk = Boolean.FALSE;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public boolean hasGateway() {
        return gateway != null;
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

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(Blueprint blueprint) {
        this.blueprint = blueprint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @deprecated {@link #getStatus} was replaced by {@link com.sequenceiq.cloudbreak.domain.stack.StackStatus#getStatus}.
     */
    @Deprecated
    public Status getStatus() {
        return status;
    }

    /**
     * @deprecated {@link #setStatus} was replaced by {@link com.sequenceiq.cloudbreak.domain.stack.StackStatus#setStatus}.
     */
    @Deprecated
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

    /**
     * @deprecated {@link #getStatusReason} was replaced by {@link com.sequenceiq.cloudbreak.domain.stack.StackStatus#getStatusReason}.
     */
    @Deprecated
    public String getStatusReason() {
        return statusReason;
    }

    /**
     * @deprecated {@link #setStatusReason} was replaced by {@link com.sequenceiq.cloudbreak.domain.stack.StackStatus#setStatusReason}.
     */
    @Deprecated
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

    public FileSystem getAdditionalFileSystem() {
        return additionalFileSystem;
    }

    public void setAdditionalFileSystem(FileSystem additionalFileSystem) {
        this.additionalFileSystem = additionalFileSystem;
    }

    public String getUserName() {
        return userName.getRaw();
    }

    public void setUserName(String userName) {
        if (userName != null) {
            this.userName = new Secret(userName);
        }
    }

    public String getPassword() {
        return password.getRaw();
    }

    public void setPassword(String password) {
        if (password != null) {
            this.password = new Secret(password);
        }
    }

    public String getClusterManagerIp() {
        return clusterManagerIp;
    }

    public void setClusterManagerIp(String clusterManagerIp) {
        this.clusterManagerIp = clusterManagerIp;
    }

    public String getAttributes() {
        return attributes.getRaw();
    }

    public void setAttributes(String attributes) {
        this.attributes = new Secret(attributes);
    }

    public String getDatabusCredential() {
        return databusCredential.getRaw();
    }

    public void setDatabusCredential(String databusCredential) {
        this.databusCredential = new Secret(databusCredential);
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

    public String getCloudbreakAmbariUser() {
        return isNotEmpty(getCloudbreakClusterManagerUser()) ? getCloudbreakClusterManagerUser() : cloudbreakAmbariUser.getRaw();
    }

    public String getCloudbreakClusterManagerUser() {
        return getIfNotNull(cloudbreakClusterManagerUser, Secret::getRaw);
    }

    public String getCloudbreakAmbariUserSecret() {
        return isNotEmpty(getCloudbreakClusterManagerUserSecret()) ? getCloudbreakClusterManagerUserSecret() : cloudbreakAmbariUser.getSecret();
    }

    public String getCloudbreakClusterManagerUserSecret() {
        return getIfNotNull(cloudbreakClusterManagerUser, Secret::getSecret);
    }

    public String getCloudbreakClusterManagerMonitoringUser() {
        return getIfNotNull(cloudbreakClusterManagerMonitoringUser, Secret::getRaw);
    }

    public String getCdpNodeStatusMonitorUser() {
        return getIfNotNull(cdpNodeStatusMonitorUser, Secret::getRaw);
    }

    public void setCloudbreakAmbariUser(String cloudbreakAmbariUser) {
        this.cloudbreakAmbariUser = new Secret(cloudbreakAmbariUser);
    }

    public void setCloudbreakUser(String cloudbreakAmbariUser) {
        this.cloudbreakAmbariUser = new Secret(cloudbreakAmbariUser);
        this.cloudbreakClusterManagerUser = new Secret(cloudbreakAmbariUser);
    }

    public void setCloudbreakClusterManagerUser(String cloudbreakClusterManagerUser) {
        this.cloudbreakClusterManagerUser = new Secret(cloudbreakClusterManagerUser);
    }

    public void setCloudbreakClusterManagerMonitoringUser(String cloudbreakClusterManagerMonitoringUser) {
        this.cloudbreakClusterManagerMonitoringUser = new Secret(cloudbreakClusterManagerMonitoringUser);
    }

    public void setCdpNodeStatusMonitorUser(String cdpNodeStatusMonitorUser) {
        this.cdpNodeStatusMonitorUser = new Secret(cdpNodeStatusMonitorUser);
    }

    public String getCloudbreakAmbariPassword() {
        return isNotEmpty(getCloudbreakClusterManagerPassword()) ? getCloudbreakClusterManagerPassword() : cloudbreakAmbariPassword.getRaw();
    }

    public String getCloudbreakClusterManagerPassword() {
        return getIfNotNull(cloudbreakClusterManagerPassword, Secret::getRaw);
    }

    public String getCloudbreakAmbariPasswordSecret() {
        return isNotEmpty(getCloudbreakClusterManagerPasswordSecret()) ? getCloudbreakClusterManagerPasswordSecret() : cloudbreakAmbariPassword.getSecret();
    }

    public String getCloudbreakClusterManagerPasswordSecret() {
        return getIfNotNull(cloudbreakClusterManagerPassword, Secret::getSecret);
    }

    public String getCloudbreakClusterManagerMonitoringPassword() {
        return getIfNotNull(cloudbreakClusterManagerMonitoringPassword, Secret::getRaw);
    }

    public String getCdpNodeStatusMonitorPassword() {
        return getIfNotNull(cdpNodeStatusMonitorPassword, Secret::getRaw);
    }

    public void setCloudbreakAmbariPassword(String cloudbreakAmbariPassword) {
        this.cloudbreakAmbariPassword = new Secret(cloudbreakAmbariPassword);
    }

    public void setCloudbreakPassword(String cloudbreakAmbariPassword) {
        this.cloudbreakAmbariPassword = new Secret(cloudbreakAmbariPassword);
        this.cloudbreakClusterManagerPassword = new Secret(cloudbreakAmbariPassword);
    }

    public void setCloudbreakClusterManagerPassword(String password) {
        cloudbreakClusterManagerPassword = new Secret(password);
    }

    public void setCloudbreakClusterManagerMonitoringPassword(String cloudbreakClusterManagerMonitoringPassword) {
        this.cloudbreakClusterManagerMonitoringPassword = new Secret(cloudbreakClusterManagerMonitoringPassword);
    }

    public void setCdpNodeStatusMonitorPassword(String cdpNodeStatusMonitorPassword) {
        this.cdpNodeStatusMonitorPassword = new Secret(cdpNodeStatusMonitorPassword);
    }

    public String getDpAmbariUser() {
        return isNotEmpty(getDpClusterManagerUser()) ? getDpClusterManagerUser() : dpAmbariUser.getRaw();
    }

    public String getDpClusterManagerUser() {
        return getIfNotNull(dpClusterManagerUser, Secret::getRaw);
    }

    public String getDpAmbariUserSecret() {
        return isNotEmpty(getDpClusterManagerUserSecret()) ? getDpClusterManagerUserSecret() : dpAmbariUser.getSecret();
    }

    public String getDpClusterManagerUserSecret() {
        return getIfNotNull(dpClusterManagerUser, Secret::getSecret);
    }

    public void setDpAmbariUser(String dpAmbariUser) {
        this.dpAmbariUser = new Secret(dpAmbariUser);
    }

    public void setDpUser(String dpAmbariUser) {
        this.dpAmbariUser = new Secret(dpAmbariUser);
        dpClusterManagerUser = new Secret(dpAmbariUser);
    }

    public void setDpClusterManagerUser(String user) {
        dpClusterManagerUser = new Secret(user);
    }

    public String getDpAmbariPassword() {
        return isNotEmpty(getDpClusterManagerPassword()) ? getDpClusterManagerPassword() : dpAmbariPassword.getRaw();
    }

    public String getDpClusterManagerPassword() {
        return getIfNotNull(dpClusterManagerPassword, Secret::getRaw);
    }

    public String getDpAmbariPasswordSecret() {
        return isNotEmpty(getDpClusterManagerPasswordSecret()) ? getDpClusterManagerPasswordSecret() : dpAmbariPassword.getSecret();
    }

    public String getDpClusterManagerPasswordSecret() {
        return getIfNotNull(dpClusterManagerPassword, Secret::getSecret);
    }

    public void setDpAmbariPassword(String dpAmbariPassword) {
        this.dpAmbariPassword = new Secret(dpAmbariPassword);
    }

    public void setDpPassword(String dpAmbariPassword) {
        this.dpAmbariPassword = new Secret(dpAmbariPassword);
        this.dpClusterManagerPassword = new Secret(dpAmbariPassword);
    }

    public void setDpClusterManagerPassword(String password) {
        dpClusterManagerPassword = new Secret(password);
    }

    public String getKeyStorePwd() {
        String pwd = getIfNotNull(keyStorePwd, Secret::getRaw);
        return isNotEmpty(pwd) ? pwd : getCloudbreakAmbariPassword();
    }

    public void setKeyStorePwd(String keyStorePwd) {
        this.keyStorePwd = new Secret(keyStorePwd);
    }

    public String getTrustStorePwd() {
        String pwd = getIfNotNull(trustStorePwd, Secret::getRaw);
        return isNotEmpty(pwd) ? pwd : getCloudbreakAmbariPassword();
    }

    public void setTrustStorePwd(String trustStorePwd) {
        this.trustStorePwd = new Secret(trustStorePwd);
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
        return isNotEmpty(getClusterManagerSecurityMasterKey()) ? getClusterManagerSecurityMasterKey() : ambariSecurityMasterKey.getRaw();
    }

    public String getClusterManagerSecurityMasterKey() {
        return getIfNotNull(clusterManagerSecurityMasterKey, Secret::getRaw);
    }

    public void setAmbariSecurityMasterKey(String ambariSecurityMasterKey) {
        this.ambariSecurityMasterKey = new Secret(ambariSecurityMasterKey);
    }

    public void setSecurityMasterKey(String ambariSecurityMasterKey) {
        this.ambariSecurityMasterKey = new Secret(ambariSecurityMasterKey);
        this.clusterManagerSecurityMasterKey = new Secret(ambariSecurityMasterKey);
    }

    public void setClusterManagerSecurityMasterKey(String securityMasterKey) {
        clusterManagerSecurityMasterKey = new Secret(securityMasterKey);
    }

    public String getExtendedBlueprintText() {
        return extendedBlueprintText.getRaw();
    }

    public void setExtendedBlueprintText(String extendedBlueprintText) {
        this.extendedBlueprintText = new Secret(extendedBlueprintText);
    }

    public String getProxyConfigCrn() {
        return proxyConfigCrn;
    }

    public void setProxyConfigCrn(String proxyConfigCrn) {
        this.proxyConfigCrn = proxyConfigCrn;
    }

    public String getDatabaseServerCrn() {
        return databaseServerCrn;
    }

    public void setDatabaseServerCrn(String databaseServerCrn) {
        this.databaseServerCrn = databaseServerCrn;
    }

    public Boolean getAutoTlsEnabled() {
        return autoTlsEnabled;
    }

    public void setAutoTlsEnabled(Boolean autoTlsEnabled) {
        this.autoTlsEnabled = autoTlsEnabled;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public boolean isRangerRazEnabled() {
        return rangerRazEnabled;
    }

    public void setRangerRazEnabled(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public CertExpirationState getCertExpirationState() {
        return certExpirationState;
    }

    public void setCertExpirationState(CertExpirationState certExpirationState) {
        this.certExpirationState = certExpirationState;
    }

    public Boolean getEmbeddedDatabaseOnAttachedDisk() {
        return embeddedDatabaseOnAttachedDisk;
    }

    public void setEmbeddedDatabaseOnAttachedDisk(Boolean embeddedDatabaseOnAttachedDisk) {
        this.embeddedDatabaseOnAttachedDisk = embeddedDatabaseOnAttachedDisk;
    }

    public CustomConfigurations getCustomConfigurations() {
        return customConfigurations;
    }

    public void setCustomConfigurations(CustomConfigurations customConfigurations) {
        this.customConfigurations = customConfigurations;
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

    @Override
    public String toString() {
        String resourceCrn = "Stack not set yet";
        if (stack != null) {
            resourceCrn = stack.getResourceCrn();
        }
        return "Cluster{" +
                "id=" + id +
                ", stackResourceCrn='" + resourceCrn + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}
