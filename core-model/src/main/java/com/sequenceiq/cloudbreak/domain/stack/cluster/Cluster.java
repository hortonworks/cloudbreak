package com.sequenceiq.cloudbreak.domain.stack.cluster;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.common.api.type.CertExpirationState;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Cluster implements ProvisionEntity, WorkspaceAwareResource, ClusterView {

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
    @Deprecated
    private ExecutorType executorType = ExecutorType.DEFAULT;

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
    private Secret cloudbreakClusterManagerUser = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cloudbreakClusterManagerMonitoringUser = Secret.EMPTY;

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
    private Secret dpClusterManagerUser = Secret.EMPTY;

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

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret monitoringCredential = Secret.EMPTY;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json customContainerDefinition;

    private String uptime;

    @ManyToOne
    private Workspace workspace;

    private String environmentCrn;

    private String proxyConfigCrn;

    private String databaseServerCrn;

    @Column(nullable = false)
    private Boolean autoTlsEnabled = Boolean.FALSE;

    @Column(name = "ranger_raz_enabled")
    private boolean rangerRazEnabled;

    @Column(name = "ranger_rms_enabled")
    private boolean rangerRmsEnabled;

    @Convert(converter = CertExpirationStateConverter.class)
    private CertExpirationState certExpirationState = CertExpirationState.VALID;

    @Column(nullable = false)
    private Boolean embeddedDatabaseOnAttachedDisk = Boolean.FALSE;

    @Column(columnDefinition = "TEXT")
    private String dbSslRootCertBundle;

    private Boolean dbSslEnabled;

    private String certExpirationDetails;

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

    public void setDatabusCredentialSecret(Secret databusCredential) {
        this.databusCredential = databusCredential;
    }

    public String getMonitoringCredential() {
        return monitoringCredential.getRaw();
    }

    public void setMonitoringCredential(String monitoringCredential) {
        this.monitoringCredential = new Secret(monitoringCredential);
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

    public String getCloudbreakClusterManagerUser() {
        return getIfNotNull(cloudbreakClusterManagerUser, Secret::getRaw);
    }

    public String getCloudbreakClusterManagerUserSecret() {
        return getIfNotNull(cloudbreakClusterManagerUser, Secret::getSecret);
    }

    public String getCloudbreakClusterManagerMonitoringUser() {
        return getIfNotNull(cloudbreakClusterManagerMonitoringUser, Secret::getRaw);
    }

    @Override
    public Secret getCloudbreakClusterManagerUserSecretObject() {
        return cloudbreakClusterManagerUser;
    }

    @Override
    public Secret getCloudbreakClusterManagerPasswordSecretObject() {
        return cloudbreakClusterManagerPassword;
    }

    public void setCloudbreakClusterManagerUser(String cloudbreakClusterManagerUser) {
        this.cloudbreakClusterManagerUser = new Secret(cloudbreakClusterManagerUser);
    }

    public void setCloudbreakClusterManagerUserSecret(Secret cloudbreakClusterManagerUser) {
        this.cloudbreakClusterManagerUser = cloudbreakClusterManagerUser;
    }

    public void setCloudbreakClusterManagerMonitoringUser(String cloudbreakClusterManagerMonitoringUser) {
        this.cloudbreakClusterManagerMonitoringUser = new Secret(cloudbreakClusterManagerMonitoringUser);
    }

    public void setCloudbreakClusterManagerMonitoringUserSecret(Secret cloudbreakClusterManagerMonitoringUser) {
        this.cloudbreakClusterManagerMonitoringUser = cloudbreakClusterManagerMonitoringUser;
    }

    public String getCloudbreakClusterManagerPassword() {
        return getIfNotNull(cloudbreakClusterManagerPassword, Secret::getRaw);
    }

    @Override
    public Secret getCloudbreakClusterManagerMonitoringUserSecret() {
        return cloudbreakClusterManagerMonitoringUser;
    }

    @Override
    public Secret getCloudbreakClusterManagerMonitoringPasswordSecret() {
        return cloudbreakClusterManagerMonitoringPassword;
    }

    @Override
    public Secret getCdpNodeStatusMonitorPasswordSecret() {
        return cdpNodeStatusMonitorPassword;
    }

    @Override
    public Secret getDatabusCredentialSecret() {
        return databusCredential;
    }

    @Override
    public Secret getMonitoringCredentialSecret() {
        return monitoringCredential;
    }

    @Override
    public Secret getKeyStorePwdSecret() {
        return keyStorePwd;
    }

    @Override
    public Secret getTrustStorePwdSecret() {
        return trustStorePwd;
    }

    @Override
    public Secret getPasswordSecret() {
        return password;
    }

    @Override
    public Secret getUserNameSecret() {
        return userName;
    }

    @Override
    public String getResourceName() {
        return ClusterView.super.getResourceName();
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

    public void setCloudbreakClusterManagerPassword(String password) {
        cloudbreakClusterManagerPassword = new Secret(password);
    }

    public void setCloudbreakClusterManagerPasswordSecret(Secret password) {
        cloudbreakClusterManagerPassword = password;
    }

    public void setCloudbreakClusterManagerMonitoringPassword(String cloudbreakClusterManagerMonitoringPassword) {
        this.cloudbreakClusterManagerMonitoringPassword = new Secret(cloudbreakClusterManagerMonitoringPassword);
    }

    public void setCloudbreakClusterManagerMonitoringPasswordSecret(Secret cloudbreakClusterManagerMonitoringPassword) {
        this.cloudbreakClusterManagerMonitoringPassword = cloudbreakClusterManagerMonitoringPassword;
    }

    public void setCdpNodeStatusMonitorPassword(String cdpNodeStatusMonitorPassword) {
        this.cdpNodeStatusMonitorPassword = new Secret(cdpNodeStatusMonitorPassword);
    }

    public void setCdpNodeStatusMonitorPasswordSecret(Secret cdpNodeStatusMonitorPassword) {
        this.cdpNodeStatusMonitorPassword = cdpNodeStatusMonitorPassword;
    }

    @Override
    public Secret getDpClusterManagerUserSecret() {
        return dpClusterManagerUser;
    }

    public void setDpClusterManagerUser(String user) {
        dpClusterManagerUser = new Secret(user);
    }

    public void setDpClusterManagerUserSecret(Secret user) {
        dpClusterManagerUser = user;
    }

    @Override
    public Secret getDpClusterManagerPasswordSecret() {
        return dpClusterManagerPassword;
    }

    public void setDpClusterManagerPassword(String password) {
        dpClusterManagerPassword = new Secret(password);
    }

    public void setDpClusterManagerPasswordSecret(Secret password) {
        dpClusterManagerPassword = password;
    }

    public String getKeyStorePwd() {
        String pwd = getIfNotNull(keyStorePwd, Secret::getRaw);
        return isNotEmpty(pwd) ? pwd : getCloudbreakClusterManagerPassword();
    }

    public void setKeyStorePwd(String keyStorePwd) {
        this.keyStorePwd = new Secret(keyStorePwd);
    }

    public void setKeyStorePwdSecret(Secret keyStorePwd) {
        this.keyStorePwd = keyStorePwd;
    }

    public String getTrustStorePwd() {
        String pwd = getIfNotNull(trustStorePwd, Secret::getRaw);
        return isNotEmpty(pwd) ? pwd : getCloudbreakClusterManagerPassword();
    }

    public void setTrustStorePwd(String trustStorePwd) {
        this.trustStorePwd = new Secret(trustStorePwd);
    }

    public void setTrustStorePwdSecret(Secret trustStorePwd) {
        this.trustStorePwd = trustStorePwd;
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

    /**
     * Need this for Jackson deserialization
     *
     * @param executorType executorType
     */
    public void setExecutorType(ExecutorType executorType) {
        this.executorType = executorType;
    }

    @Override
    public Secret getExtendedBlueprintTextSecret() {
        return extendedBlueprintText;
    }

    @Override
    public Secret getAttributesSecret() {
        return attributes;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
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

    public Boolean isAutoTlsEnabled() {
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

    public boolean isRangerRmsEnabled() {
        return rangerRmsEnabled;
    }

    public void setRangerRazEnabled(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    public void setRangerRmsEnabled(boolean rangerRmsEnabled) {
        this.rangerRmsEnabled = rangerRmsEnabled;
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

    public Boolean isEmbeddedDatabaseOnAttachedDisk() {
        return embeddedDatabaseOnAttachedDisk;
    }

    public void setEmbeddedDatabaseOnAttachedDisk(Boolean embeddedDatabaseOnAttachedDisk) {
        this.embeddedDatabaseOnAttachedDisk = embeddedDatabaseOnAttachedDisk;
    }

    public CustomConfigurations getCustomConfigurations() {
        return customConfigurations;
    }

    @Override
    public Boolean getEmbeddedDatabaseOnAttachedDisk() {
        return embeddedDatabaseOnAttachedDisk;
    }

    @Override
    public Boolean getAutoTlsEnabled() {
        return autoTlsEnabled;
    }

    public void setCustomConfigurations(CustomConfigurations customConfigurations) {
        this.customConfigurations = customConfigurations;
    }

    @Override
    public String getDbSslRootCertBundle() {
        return dbSslRootCertBundle;
    }

    public void setDbSslRootCertBundle(String dbSslRootCertBundle) {
        this.dbSslRootCertBundle = dbSslRootCertBundle;
    }

    @Override
    public Boolean getDbSslEnabled() {
        return dbSslEnabled;
    }

    public void setDbSslEnabled(Boolean dbSslEnabled) {
        this.dbSslEnabled = dbSslEnabled;
    }

    public String getCertExpirationDetails() {
        return certExpirationDetails;
    }

    public void setCertExpirationDetails(String certExpiryDetails) {
        this.certExpirationDetails = certExpiryDetails;
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
        return "Cluster{" +
                "id=" + id +
                ", stackResourceCrn='" + getResourceCrn() + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                '}';
    }

    @Override
    public String getResourceCrn() {
        String resourceCrn = "Stack not set yet";
        if (stack != null) {
            resourceCrn = stack.getResourceCrn();
        }
        return resourceCrn;
    }
}
