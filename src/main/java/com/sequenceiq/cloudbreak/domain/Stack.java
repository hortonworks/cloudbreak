package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE c.id= :id"),
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
                        + "WHERE s.owner= :user"
                        + "AND status <> 'DELETE_COMPLETED' "),
        @NamedQuery(
                name = "Stack.findPublicInAccountForUser",
                query = "SELECT s FROM Stack s "
                        + "LEFT JOIN FETCH s.resources "
                        + "LEFT JOIN FETCH s.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE (s.account= :account AND s.publicInAccount= true) "
                        + "OR s.owner= :user"
                        + "AND status <> 'DELETE_COMPLETED'"),
        @NamedQuery(
                name = "Stack.findAllInAccount",
                query = "SELECT s FROM Stack s "
                        + "LEFT JOIN FETCH s.resources "
                        + "LEFT JOIN FETCH s.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE s.account= :account "
                        + "AND status <> 'DELETE_COMPLETED' "),
        @NamedQuery(
                name = "Stack.findByAmbari",
                query = "SELECT s from Stack s "
                        + "LEFT JOIN FETCH s.resources "
                        + "LEFT JOIN FETCH s.instanceGroups ig "
                        + "LEFT JOIN FETCH ig.instanceMetaData "
                        + "WHERE s.ambariIp= :ambariIp"),
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
                        + "WHERE t.credential.id= :credentialId ")
})
public class Stack implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_table")
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String owner;
    private String account;

    private boolean publicInAccount;
    private String region;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;

    private boolean stackCompleted;
    private String image;

    private String ambariIp;
    private String userName;
    private String password;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    private String hash;

    private boolean metadataReady;

    @OneToOne
    private Credential credential;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Cluster cluster;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Resource> resources = new HashSet<>();

    @Version
    private Long version;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InstanceGroup> instanceGroups = new HashSet<>();

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

    public boolean isStackCompleted() {
        return stackCompleted;
    }

    public void setStackCompleted(boolean stackCompleted) {
        this.stackCompleted = stackCompleted;
    }

    public String getAmbariIp() {
        return ambariIp;
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

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
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

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public boolean isMetadataReady() {
        return metadataReady;
    }

    public void setMetadataReady(boolean metadataReady) {
        this.metadataReady = metadataReady;
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

    public Set<InstanceMetaData> getAllInstanceMetaData() {
        Set<InstanceMetaData> instanceMetaDatas = new HashSet<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
            instanceMetaDatas.addAll(instanceGroup.getInstanceMetaData());
        }
        return instanceMetaDatas;
    }

    public List<InstanceGroup> getInstanceGroupsAsList() {
        return new ArrayList<>(instanceGroups);
    }

    public CloudPlatform cloudPlatform() {
        return credential.cloudPlatform();
    }

}
