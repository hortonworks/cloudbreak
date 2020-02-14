package com.sequenceiq.freeipa.entity;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOPPED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_REQUESTED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.converter.TunnelConverter;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.Tunnel;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"accountid", "environmentcrn", "terminated"}))
public class Stack {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String resourceCrn;

    private String name;

    private String environmentCrn;

    @OneToMany(mappedBy = "stack", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChildEnvironment> childEnvironments = new ArrayList<>();

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
    private Set<InstanceGroup> instanceGroups = new HashSet<>();

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

    @OneToOne(cascade = {CascadeType.ALL}, optional = false)
    private Network network;

    @OneToOne(cascade = CascadeType.ALL)
    private StackStatus stackStatus;

    private String template;

    private String owner;

    private String appVersion;

    private String minaSshdServiceId;

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

    public boolean getUseCcm() {
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

    public boolean isClusterProxyRegistered() {
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

    public List<InstanceMetaData> getNotDeletedInstanceMetaDataList() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream())
                .collect(Collectors.toList());
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public List<ChildEnvironment> getChildEnvironments() {
        return childEnvironments;
    }

    public void setChildEnvironments(List<ChildEnvironment> childEnvironments) {
        this.childEnvironments = childEnvironments;
    }

    public List<String> getChildEnvironmentCrns() {
        return childEnvironments.stream()
                .map(ChildEnvironment::getEnvironmentCrn)
                .collect(Collectors.toList());
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    public String getMinaSshdServiceId() {
        return minaSshdServiceId;
    }

    public void setMinaSshdServiceId(String minaSshdServiceId) {
        this.minaSshdServiceId = minaSshdServiceId;
    }

    public void attachChildEnvironment(String childEnvironmentCrn) {
        ChildEnvironment childEnvironment = new ChildEnvironment();
        childEnvironment.setEnvironmentCrn(childEnvironmentCrn);
        childEnvironment.setStack(this);
        childEnvironments.add(childEnvironment);
    }
}
