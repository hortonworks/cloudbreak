package com.sequenceiq.cloudbreak.domain;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.HashSet;
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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.json.EncryptedJsonToString;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(name = "Cluster", uniqueConstraints = @UniqueConstraint(columnNames = {"account", "name"}))
public class Cluster implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cluster_generator")
    @SequenceGenerator(name = "cluster_generator", sequenceName = "cluster_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Stack stack;

    @ManyToOne
    private Blueprint blueprint;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String account;

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

    @Type(type = "encrypted_string")
    @Column(nullable = false)
    private String userName;

    @Type(type = "encrypted_string")
    @Column(nullable = false)
    private String password;

    private String cloudbreakAmbariUser;

    @Type(type = "encrypted_string")
    private String cloudbreakAmbariPassword;

    @Column(nullable = false)
    private Boolean secure;

    @ManyToOne
    private KerberosConfig kerberosConfig;

    @Column(nullable = false)
    private Boolean emailNeeded;

    @Column(nullable = false)
    private Boolean topologyValidation = Boolean.TRUE;

    private String emailTo;

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

    @Convert(converter = EncryptedJsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    @Convert(converter = EncryptedJsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json blueprintInputs;

    @Type(type = "encrypted_string")
    @Column(columnDefinition = "TEXT")
    private String blueprintCustomProperties;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json customContainerDefinition;

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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
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

    public Boolean getEmailNeeded() {
        return emailNeeded;
    }

    public void setEmailNeeded(Boolean emailNeeded) {
        this.emailNeeded = emailNeeded;
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
        return status.equals(Status.CREATE_FAILED);
    }

    public boolean isSecure() {
        return secure == null ? false : secure;
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
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
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

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public Gateway getGateway() {
        return gateway;
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

    public Json getBlueprintInputs() {
        return blueprintInputs;
    }

    public void setBlueprintInputs(Json blueprintInputs) {
        this.blueprintInputs = blueprintInputs;
    }

    public String getCloudbreakAmbariUser() {
        return cloudbreakAmbariUser;
    }

    public void setCloudbreakAmbariUser(String cloudbreakAmbariUser) {
        this.cloudbreakAmbariUser = cloudbreakAmbariUser;
    }

    public String getCloudbreakAmbariPassword() {
        return cloudbreakAmbariPassword;
    }

    public void setCloudbreakAmbariPassword(String cloudbreakAmbariPassword) {
        this.cloudbreakAmbariPassword = cloudbreakAmbariPassword;
    }

    public String getBlueprintCustomProperties() {
        return blueprintCustomProperties;
    }

    public void setBlueprintCustomProperties(String blueprintCustomProperties) {
        this.blueprintCustomProperties = blueprintCustomProperties;
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
}
