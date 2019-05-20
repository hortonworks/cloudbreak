package com.sequenceiq.freeipa.entity;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
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

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"environment", "name"}))
public class Stack {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String environment;

    private String tenant;

    private String region;

    private Long created;

    @Column(columnDefinition = "TEXT")
    private String platformvariant;

    private String availabilityZone;

    @Column(columnDefinition = "TEXT")
    private String cloudPlatform;

    private Integer gatewayport;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InstanceGroup> instanceGroups = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private SecurityConfig securityConfig;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private StackAuthentication stackAuthentication;

    private Long terminated;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    @OneToOne(cascade = {CascadeType.ALL}, orphanRemoval = true, optional = false)
    private Credential credential;

    @OneToOne(cascade = {CascadeType.ALL}, optional = false)
    private Network network;

    @OneToOne(cascade = CascadeType.ALL)
    private StackStatus stackStatus;

    private String template;

    private String owner;

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

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
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

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
