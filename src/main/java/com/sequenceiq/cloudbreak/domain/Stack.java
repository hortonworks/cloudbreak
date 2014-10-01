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
                        + "WHERE c.id= :id"),
        @NamedQuery(
                name = "Stack.findAllStackForTemplate",
                query = "SELECT c FROM Stack c "
                        + "WHERE c.template.id= :id"),
        @NamedQuery(
                name = "Stack.findStackForCluster",
                query = "SELECT c FROM Stack c "
                        + "WHERE c.cluster.id= :id"),
        @NamedQuery(
                name = "Stack.findStackWithListsForCluster",
                query = "SELECT c FROM Stack c "
                        + "LEFT JOIN FETCH c.resources "
                        + "LEFT JOIN FETCH c.instanceMetaData "
                        + "WHERE c.cluster.id= :id"),
        @NamedQuery(
                name = "Stack.findRequestedStacksWithCredential",
                query = "SELECT c FROM Stack c "
                        + "WHERE c.credential.id= :credentialId "
                        + "AND c.status= 'REQUESTED'"),
        @NamedQuery(
                name = "Stack.findOneWithLists",
                query = "SELECT c FROM Stack c "
                        + "LEFT JOIN FETCH c.resources "
                        + "LEFT JOIN FETCH c.instanceMetaData "
                        + "WHERE c.id= :id"),
        @NamedQuery(
                name = "Stack.findByStackResourceName",
                query = "SELECT c FROM Stack c inner join c.resources res "
                        + "WHERE res.resourceName = :stackName AND res.resourceType = 'CLOUDFORMATION_STACK'"),
        @NamedQuery(
                name = "Stack.findForUser",
                query = "SELECT s FROM Stack s "
                        + "WHERE s.owner= :user"),
        @NamedQuery(
                name = "Stack.findPublicsInAccount",
                query = "SELECT s FROM Stack s "
                        + "WHERE s.account= :account "
                        + "AND s.publicInAccount= true"),
        @NamedQuery(
                name = "Stack.findAllInAccount",
                query = "SELECT s FROM Stack s "
                        + "WHERE s.account= :account ")
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

    private Integer nodeCount;

    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;

    private boolean stackCompleted;

    private String ambariIp;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    private String hash;

    private boolean metadataReady;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InstanceMetaData> instanceMetaData = new HashSet<>();

    @OneToOne
    private Template template;

    @OneToOne
    private Credential credential;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Cluster cluster;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Resource> resources = new HashSet<>();

    @Version
    private Long version;

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

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
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

    public Set<InstanceMetaData> getInstanceMetaData() {
        return instanceMetaData;
    }

    public void setInstanceMetaData(Set<InstanceMetaData> instanceMetaData) {
        this.instanceMetaData = instanceMetaData;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
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

    public Integer getMultiplier() {
        return template.getMultiplier();
    }

}
