package com.sequenceiq.freeipa.entity;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOPPED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_REQUESTED;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.common.domain.IdAware;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestrationNode;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.converter.ArchitectureConverter;
import com.sequenceiq.cloudbreak.converter.TunnelConverter;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.secret.SecretGetter;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.SecretSetter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"accountid", "environmentcrn", "terminated"}))
public class Stack implements AccountAwareResource, OrchestratorAware, IdAware {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String resourceCrn;

    private String name;

    private String environmentCrn;

    private String accountId;

    private String region;

    private Long created;

    @Column(columnDefinition = "TEXT")
    private String platformvariant;

    private String availabilityZone;

    @Column(columnDefinition = "TEXT")
    private String cloudPlatform;

    private Integer gatewayport;

    /**
     * @deprecated use {@link #tunnel} instead
     */
    @Deprecated()
    private Boolean useCcm = Boolean.FALSE;

    @Convert(converter = TunnelConverter.class)
    private Tunnel tunnel = Tunnel.DIRECT;

    private Boolean clusterProxyRegistered = Boolean.FALSE;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<InstanceGroup> instanceGroups = new HashSet<>();

    @OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Set<DynamicEntitlement> dynamicEntitlements;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private SecurityConfig securityConfig;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private StackAuthentication stackAuthentication;

    private Long terminated = -1L;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json telemetry;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json backup;

    @OneToOne(cascade = {CascadeType.ALL}, optional = false)
    private Network network;

    @OneToOne(cascade = CascadeType.ALL)
    @JsonManagedReference
    private StackStatus stackStatus;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "stack")
    private ImageEntity image;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret databusCredential = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret monitoringCredential = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret cdpNodeStatusMonitorPassword = Secret.EMPTY;

    private String template;

    private String owner;

    private String appVersion;

    private String minaSshdServiceId;

    private String ccmV2AgentCrn;

    @Version
    private Long version;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret ccmParameters = Secret.EMPTY;

    private boolean multiAz;

    private String supportedImdsVersion;

    @Convert(converter = ArchitectureConverter.class)
    private Architecture architecture;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getPlatformvariant() {
        return platformvariant;
    }

    public void setPlatformvariant(String platformvariant) {
        this.platformvariant = platformvariant;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Integer getGatewayport() {
        return gatewayport;
    }

    public void setGatewayport(Integer gatewayport) {
        this.gatewayport = gatewayport;
    }

    public Boolean getUseCcm() {
        return Boolean.TRUE.equals(useCcm);
    }

    public void setUseCcm(Boolean useCcm) {
        this.useCcm = useCcm;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public Boolean getClusterProxyRegistered() {
        return Boolean.TRUE.equals(clusterProxyRegistered);
    }

    public void setClusterProxyRegistered(Boolean clusterProxyRegistered) {
        this.clusterProxyRegistered = clusterProxyRegistered;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public StackAuthentication getStackAuthentication() {
        return stackAuthentication;
    }

    public void setStackAuthentication(StackAuthentication stackAuthentication) {
        this.stackAuthentication = stackAuthentication;
    }

    public Set<InstanceGroup> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Long getTerminated() {
        return terminated;
    }

    public void setTerminated(Long terminated) {
        this.terminated = terminated;
    }

    public Json getTags() {
        return tags;
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public Telemetry getTelemetry() {
        if (telemetry != null && telemetry.getValue() != null) {
            return JsonUtil.readValueOpt(telemetry.getValue(), Telemetry.class).orElse(null);
        }
        return null;
    }

    public void setTelemetry(Telemetry telemetry) {
        if (telemetry != null) {
            this.telemetry = new Json(telemetry);
        }
    }

    public Set<DynamicEntitlement> getDynamicEntitlements() {
        return dynamicEntitlements;
    }

    public void setDynamicEntitlements(Set<DynamicEntitlement> dynamicEntitlements) {
        this.dynamicEntitlements = dynamicEntitlements;
    }

    public Backup getBackup() {
        if (backup != null && backup.getValue() != null) {
            return JsonUtil.readValueOpt(backup.getValue(), Backup.class).orElse(null);
        }
        return null;
    }

    public void setBackup(Backup backup) {
        if (backup != null) {
            this.backup = new Json(backup);
        }
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public StackStatus getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(StackStatus stackStatus) {
        this.stackStatus = stackStatus;
    }

    public ImageEntity getImage() {
        return image;
    }

    public void setImage(ImageEntity image) {
        this.image = image;
    }

    public String getDatabusCredential() {
        return databusCredential.getRaw();
    }

    @SecretGetter(marker = SecretMarker.DBUS_CREDENTIAL)
    public Secret getDatabusCredentialSecret() {
        return databusCredential;
    }

    public void setDatabusCredential(String databusCredential) {
        this.databusCredential = new Secret(databusCredential);
    }

    @SecretSetter(marker = SecretMarker.DBUS_CREDENTIAL)
    public void setDatabusCredentialSecret(Secret databusCredential) {
        this.databusCredential = databusCredential;
    }

    public String getMonitoringCredential() {
        return monitoringCredential.getRaw();
    }

    public void setMonitoringCredential(String monitoringCredential) {
        this.monitoringCredential = new Secret(monitoringCredential);
    }

    public String getCdpNodeStatusMonitorPassword() {
        return getIfNotNull(cdpNodeStatusMonitorPassword, Secret::getRaw);
    }

    @SecretGetter(marker = SecretMarker.NODE_STATUS_MONITOR_PWD)
    public Secret getCdpNodeStatusMonitorPasswordSecret() {
        return cdpNodeStatusMonitorPassword;
    }

    public void setCdpNodeStatusMonitorPassword(String cdpNodeStatusMonitorPassword) {
        this.cdpNodeStatusMonitorPassword = new Secret(cdpNodeStatusMonitorPassword);
    }

    @SecretSetter(marker = SecretMarker.NODE_STATUS_MONITOR_PWD)
    public void setCdpNodeStatusMonitorPasswordSecret(Secret cdpNodeStatusMonitorPassword) {
        this.cdpNodeStatusMonitorPassword = cdpNodeStatusMonitorPassword;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean isStackInDeletionPhase() {
        return DELETE_COMPLETED.equals(stackStatus.getStatus()) || DELETE_IN_PROGRESS.equals(stackStatus.getStatus());
    }

    public Set<InstanceMetaData> getNotDeletedInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getNotTerminatedInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getNotTerminatedInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public List<InstanceMetaData> getAllInstanceMetaDataList() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getAllInstanceMetaData().stream())
                .collect(Collectors.toList());
    }

    public Optional<InstanceMetaData> getPrimaryGateway() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getAllInstanceMetaData().stream())
                .filter(instanceMetaData -> InstanceMetadataType.GATEWAY_PRIMARY.equals(instanceMetaData.getInstanceMetadataType()))
                .findFirst();
    }

    public InstanceMetaData getPrimaryGatewayAndThrowExceptionIfEmpty() {
        return getPrimaryGateway().orElseThrow(() -> new CloudbreakRuntimeException("Can't find any primary GW"));
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getResourceName() {
        return name;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public boolean isStopRequested() {
        return STOP_REQUESTED.equals(stackStatus.getStatus());
    }

    public boolean isStopInProgress() {
        return STOP_IN_PROGRESS.equals(stackStatus.getStatus()) || STOP_REQUESTED.equals(stackStatus.getStatus());
    }

    public boolean isAvailable() {
        return AVAILABLE.equals(stackStatus.getStatus());
    }

    public boolean isStopped() {
        return STOPPED.equals(stackStatus.getStatus());
    }

    public boolean isStopFailed() {
        return STOP_FAILED.equals(stackStatus.getStatus());
    }

    public boolean isDeleteCompleted() {
        return DELETE_COMPLETED.equals(stackStatus.getStatus());
    }

    public String getMinaSshdServiceId() {
        return minaSshdServiceId;
    }

    public void setMinaSshdServiceId(String minaSshdServiceId) {
        this.minaSshdServiceId = minaSshdServiceId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getCcmV2AgentCrn() {
        return ccmV2AgentCrn;
    }

    public void setCcmV2AgentCrn(String ccmV2AgentCrn) {
        this.ccmV2AgentCrn = ccmV2AgentCrn;
    }

    public CcmConnectivityParameters getCcmParameters() {
        if (ccmParameters != null && ccmParameters.getRaw() != null) {
            return JsonUtil.readValueOpt(ccmParameters.getRaw(), CcmConnectivityParameters.class).orElse(null);
        }
        return new CcmConnectivityParameters();
    }

    public void setCcmParameters(CcmConnectivityParameters ccmParameters) {
        this.ccmParameters = new Secret(JsonUtil.writeValueAsStringSilent(ccmParameters));
    }

    /**
     * Need this for Jackson deserialization
     */
    private void setCcmParameters(String rawCcmParameters) {
        this.ccmParameters = new Secret(rawCcmParameters);
    }

    public boolean isMultiAz() {
        return multiAz;
    }

    public void setMultiAz(boolean multiAz) {
        this.multiAz = multiAz;
    }

    public String getSupportedImdsVersion() {
        return supportedImdsVersion;
    }

    public void setSupportedImdsVersion(String supportedImdsVersion) {
        this.supportedImdsVersion = supportedImdsVersion;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public void setArchitecture(Architecture architecture) {
        this.architecture = architecture;
    }

    @Override
    public String toString() {
        return "Stack{" +
                "id=" + id +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", name='" + name + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", accountId='" + accountId + '\'' +
                ", region='" + region + '\'' +
                ", created=" + created +
                ", platformvariant='" + platformvariant + '\'' +
                ", availabilityZone='" + availabilityZone + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", gatewayport=" + gatewayport +
                ", useCcm=" + useCcm +
                ", tunnel=" + tunnel +
                ", clusterProxyRegistered=" + clusterProxyRegistered +
                ", securityConfig=" + securityConfig +
                ", stackAuthentication=" + stackAuthentication +
                ", terminated=" + terminated +
                ", tags=" + tags +
                ", telemetry=" + telemetry +
                ", network=" + network +
                ", stackStatus=" + stackStatus +
                ", owner='" + owner + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", minaSshdServiceId='" + minaSshdServiceId + '\'' +
                ", ccmV2AgentCrn='" + ccmV2AgentCrn + '\'' +
                ", multiAz='" + multiAz + '\'' +
                ", supportedImdsVersion='" + supportedImdsVersion + '\'' +
                ", architecture='" + architecture + '\'' +
                '}';
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            Stack stack = (Stack) o;
            return Objects.equals(id, stack.id)
                    && Objects.equals(resourceCrn, stack.resourceCrn)
                    && Objects.equals(name, stack.name)
                    && Objects.equals(environmentCrn, stack.environmentCrn)
                    && Objects.equals(accountId, stack.accountId)
                    && Objects.equals(region, stack.region)
                    && Objects.equals(created, stack.created)
                    && Objects.equals(platformvariant, stack.platformvariant)
                    && Objects.equals(availabilityZone, stack.availabilityZone)
                    && Objects.equals(cloudPlatform, stack.cloudPlatform)
                    && Objects.equals(gatewayport, stack.gatewayport)
                    && Objects.equals(useCcm, stack.useCcm)
                    && tunnel == stack.tunnel
                    && Objects.equals(clusterProxyRegistered, stack.clusterProxyRegistered)
                    && Objects.equals(terminated, stack.terminated)
                    && Objects.equals(tags, stack.tags)
                    && Objects.equals(telemetry, stack.telemetry)
                    && Objects.equals(backup, stack.backup)
                    && Objects.equals(template, stack.template)
                    && Objects.equals(owner, stack.owner)
                    && Objects.equals(appVersion, stack.appVersion)
                    && Objects.equals(minaSshdServiceId, stack.minaSshdServiceId)
                    && Objects.equals(ccmV2AgentCrn, stack.ccmV2AgentCrn)
                    && Objects.equals(version, stack.version)
                    && Objects.equals(ccmParameters, stack.ccmParameters)
                    && multiAz == stack.multiAz
                    && Objects.equals(architecture, stack.architecture);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resourceCrn, name, environmentCrn, accountId, region, created, platformvariant, availabilityZone, cloudPlatform, gatewayport,
                useCcm, tunnel, clusterProxyRegistered, terminated, tags, telemetry, backup, template, owner, appVersion, minaSshdServiceId, ccmV2AgentCrn,
                version, ccmParameters, multiAz, architecture);
    }

    @Override
    public Set<Node> getAllFunctioningNodes() {
        return getAllNotDeletedNodes();
    }

    @Override
    public Set<Node> getAllNotDeletedNodes() {
        return new HashSet<>(getNotDeletedInstanceMetaDataSet()).stream().map(OrchestrationNode::getNode).collect(Collectors.toSet());
    }
}
