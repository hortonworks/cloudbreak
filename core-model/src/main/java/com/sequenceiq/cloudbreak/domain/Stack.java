package com.sequenceiq.cloudbreak.domain;

import static com.sequenceiq.cloudbreak.domain.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.domain.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.domain.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.domain.Status.STOPPED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_REQUESTED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "Stack", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" })
})
@NamedQueries({
        @NamedQuery(
                name = "Stack.findById",
                query = "SELECT c FROM Stack c "
                        + "LEFT JOIN FETCH c.resources "
                        + "LEFT JOIN FETCH c.instanceGroups ig "
                        + "LEFT JOIN FETCH c.allowedSubnets "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE c.id= :id"),
        @NamedQuery(
                name = "Stack.findByIdWithSecurityConfig",
                query = "SELECT s FROM Stack s "
                        + "LEFT JOIN FETCH s.securityConfig "
                        + "WHERE s.id= :id"),
        @NamedQuery(
                name = "Stack.findAllStackForTemplate",
                query = "SELECT c FROM Stack c inner join c.instanceGroups tg "
                        + "LEFT JOIN FETCH c.resources "
                        + "LEFT JOIN FETCH c.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE tg.template.id= :id"),
        @NamedQuery(
                name = "Stack.findStackForCluster",
                query = "SELECT c FROM Stack c "
                        + "LEFT JOIN FETCH c.resources "
                        + "LEFT JOIN FETCH c.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE c.cluster.id= :id"),
        @NamedQuery(
                name = "Stack.findStackWithListsForCluster",
                query = "SELECT c FROM Stack c "
                        + "LEFT JOIN FETCH c.resources "
                        + "LEFT JOIN FETCH c.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE c.cluster.id= :id"),
        @NamedQuery(
                name = "Stack.findRequestedStacksWithCredential",
                query = "SELECT DISTINCT c FROM Stack c "
                        + "LEFT JOIN FETCH c.resources "
                        + "LEFT JOIN FETCH c.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE c.credential.id= :credentialId "
                        + "AND c.status= 'REQUESTED'"),
        @NamedQuery(
                name = "Stack.findOneWithLists",
                query = "SELECT c FROM Stack c "
                        + "LEFT JOIN FETCH c.resources "
                        + "LEFT JOIN FETCH c.instanceGroups ig "
                        + "LEFT JOIN FETCH c.allowedSubnets "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE c.id= :id"),
        @NamedQuery(
                name = "Stack.findByStackResourceName",
                query = "SELECT c FROM Stack c inner join c.resources res "
                        + "WHERE res.resourceName = :stackName AND res.resourceType = 'CLOUDFORMATION_STACK'"),
        @NamedQuery(
                name = "Stack.findForUser",
                query = "SELECT s FROM Stack s "
                        + "LEFT JOIN FETCH s.resources "
                        + "LEFT JOIN FETCH s.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE s.owner= :user "
                        + "AND s.status <> 'DELETE_COMPLETED' "),
        @NamedQuery(
                name = "Stack.findPublicInAccountForUser",
                query = "SELECT s FROM Stack s "
                        + "LEFT JOIN FETCH s.resources "
                        + "LEFT JOIN FETCH s.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE ((s.account= :account AND s.publicInAccount= true) OR s.owner= :user) "
                        + "AND s.status <> 'DELETE_COMPLETED' "),
        @NamedQuery(
                name = "Stack.findAllInAccount",
                query = "SELECT s FROM Stack s "
                        + "LEFT JOIN FETCH s.resources "
                        + "LEFT JOIN FETCH s.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE s.account= :account "
                        + "AND s.status <> 'DELETE_COMPLETED' "),
        @NamedQuery(
                name = "Stack.findByAmbari",
                query = "SELECT s from Stack s "
                        + "LEFT JOIN FETCH s.resources "
                        + "LEFT JOIN FETCH s.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE s.cluster.ambariIp= :ambariIp "
                        + "AND s.status <> 'DELETE_COMPLETED' "),
        @NamedQuery(
                name = "Stack.findOneByName",
                query = "SELECT c FROM Stack c "
                        + "LEFT JOIN FETCH c.resources "
                        + "LEFT JOIN FETCH c.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE c.name= :name and c.account= :account"),
        @NamedQuery(
                name = "Stack.findByIdInAccount",
                query = "SELECT s FROM Stack s "
                        + "LEFT JOIN FETCH s.resources "
                        + "LEFT JOIN FETCH s.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE s.id= :id and s.account= :account"),
        @NamedQuery(
                name = "Stack.findByNameInAccount",
                query = "SELECT s FROM Stack s "
                        + "LEFT JOIN FETCH s.resources "
                        + "LEFT JOIN FETCH s.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE s.name= :name and ((s.account= :account and s.publicInAccount=true) or s.owner= :owner)"),
        @NamedQuery(
                name = "Stack.findByNameInUser",
                query = "SELECT t FROM Stack t "
                        + "LEFT JOIN FETCH t.resources "
                        + "LEFT JOIN FETCH t.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE t.owner= :owner and t.name= :name"),
        @NamedQuery(
                name = "Stack.findByCredential",
                query = "SELECT t FROM Stack t "
                        + "LEFT JOIN FETCH t.resources "
                        + "LEFT JOIN FETCH t.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE t.credential.id= :credentialId "),
        @NamedQuery(
                name = "Stack.findAllByNetwork",
                query = "SELECT t FROM Stack t "
                        + "LEFT JOIN FETCH t.resources "
                        + "LEFT JOIN FETCH t.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE t.network.id= :networkId ")
})
public class Stack implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_table")
    private Long id;
    @Column(nullable = false)
    private String name;
    private String owner;
    private String account;
    private boolean publicInAccount;
    private String region;
    private String image;
    private int consulServers;
    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String statusReason;
    @Enumerated(EnumType.STRING)
    private Status status;
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000)
    private Map<String, String> parameters;
    @OneToOne
    private Credential credential;
    @OneToOne(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Cluster cluster;
    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<Resource> resources = new HashSet<>();
    @Enumerated(EnumType.STRING)
    private OnFailureAction onFailureActionAction = OnFailureAction.ROLLBACK;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private FailurePolicy failurePolicy;
    @OneToOne(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private SecurityConfig securityConfig;
    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<InstanceGroup> instanceGroups = new HashSet<>();
    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<Subnet> allowedSubnets = new HashSet<>();
    @Version
    private Long version;
    @ManyToOne
    private Network network;

    public Set<InstanceGroup> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getConsulServers() {
        return consulServers;
    }

    public void setConsulServers(int consulServers) {
        this.consulServers = consulServers;
    }

    public OnFailureAction getOnFailureActionAction() {
        return onFailureActionAction;
    }

    public void setOnFailureActionAction(OnFailureAction onFailureActionAction) {
        this.onFailureActionAction = onFailureActionAction;
    }

    public FailurePolicy getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicy failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public List<Resource> getResourcesByType(ResourceType resourceType) {
        List<Resource> resourceList = new ArrayList<>();
        for (Resource resource : resources) {
            if (resourceType.equals(resource.getResourceType())) {
                resourceList.add(resource);
            }
        }
        return resourceList;
    }

    public Resource getResourceByType(ResourceType resourceType) {
        for (Resource resource : resources) {
            if (resourceType.equals(resource.getResourceType())) {
                return resource;
            }
        }
        return null;
    }

    public InstanceGroup getInstanceGroupByInstanceGroupName(String group) {
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (group.equals(instanceGroup.getGroupName())) {
                return instanceGroup;
            }
        }
        return null;
    }

    public Integer getFullNodeCount() {
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            nodeCount += instanceGroup.getNodeCount();
        }
        return nodeCount;
    }

    public Integer getFullNodeCountWithoutDecommissionedNodes() {
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                if (!instanceMetaData.getInstanceStatus().equals(InstanceStatus.DECOMMISSIONED)) {
                    nodeCount++;
                }
            }
        }
        return nodeCount;
    }

    public Integer getFullNodeCountWithoutDecommissionedAndUnRegisteredNodes() {
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                if (instanceMetaData.getInstanceStatus().equals(InstanceStatus.REGISTERED)) {
                    nodeCount++;
                }
            }
        }
        return nodeCount;
    }

    public Set<InstanceMetaData> getRunningInstanceMetaData() {
        Set<InstanceMetaData> instanceMetadata = new HashSet<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
            instanceMetadata.addAll(instanceGroup.getInstanceMetaData());
        }
        return instanceMetadata;
    }

    public List<InstanceGroup> getInstanceGroupsAsList() {
        return new ArrayList<>(instanceGroups);
    }

    public CloudPlatform cloudPlatform() {
        return credential.cloudPlatform();
    }

    public boolean isStackInDeletionPhase() {
        return status.equals(DELETE_COMPLETED) || status.equals(DELETE_IN_PROGRESS);
    }

    public boolean isStopFailed() {
        return STOP_FAILED.equals(status);
    }

    public boolean isStopInProgress() {
        return STOP_IN_PROGRESS.equals(status);
    }

    public boolean isStartFailed() {
        return START_FAILED.equals(status);
    }

    public Boolean isCloudPlatformUsedWithTemplate() {
        return cloudPlatform().isWithTemplate();
    }

    public Set<Subnet> getAllowedSubnets() {
        return allowedSubnets;
    }

    public void setAllowedSubnets(Set<Subnet> allowedSubnets) {
        this.allowedSubnets = new HashSet<>(allowedSubnets);
    }

    public void addAllowedSubnets(Set<Subnet> allowedSubnets) {
        for (Subnet subnet : allowedSubnets) {
            addAllowedSubnet(subnet);
        }
    }

    public void addAllowedSubnet(Subnet subnet) {
        allowedSubnets.add(subnet);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public InstanceGroup getGatewayInstanceGroup() {
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                return instanceGroup;
            }
        }
        return null;
    }

    public int getGateWayNodeCount() {
        return getGatewayInstanceGroup().getNodeCount();
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public String getAmbariIp() {
        return cluster == null ? null : cluster.getAmbariIp();
    }

    public boolean isAvailable() {
        return AVAILABLE.equals(status);
    }

    public boolean isStopRequested() {
        return STOP_REQUESTED.equals(status);
    }

    public boolean isStopped() {
        return STOPPED.equals(status);
    }

    public boolean isDeleteCompleted() {
        return DELETE_COMPLETED.equals(status);
    }

    public boolean isDeleteInProgress() {
        return DELETE_IN_PROGRESS.equals(status);
    }

    public boolean isStartInProgress() {
        return START_IN_PROGRESS.equals(status) || START_REQUESTED.equals(status);
    }

    public boolean isRequested() {
        return REQUESTED.equals(status) || CREATE_IN_PROGRESS.equals(status);
    }

    public boolean isStackReadyForStop() {
        return AVAILABLE.equals(status) || STOP_REQUESTED.equals(status);
    }
}
