package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
        @UniqueConstraint(columnNames = { "stack_user", "name" })
})
@NamedQueries({
        @NamedQuery(
                name = "Stack.findAllStackForTemplate",
                query = "SELECT c FROM Stack c "
                        + "WHERE c.template.id= :id"),
        @NamedQuery(
                name = "Stack.findStackForCluster",
                query = "SELECT c FROM Stack c "
                        + "WHERE c.cluster.id= :id"),
        @NamedQuery(
                name = "Stack.findRequestedStacksWithCredential",
                query = "SELECT c FROM Stack c "
                        + "WHERE c.credential.id= :credentialId "
                        + "AND c.status= 'REQUESTED'")
})

public class Stack implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_table")
    private Long id;

    private Integer nodeCount;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String cfStackName;

    private String cfStackId;

    private boolean cfStackCompleted;

    private String ambariIp;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    private String hash;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InstanceMetaData> instanceMetaData = new HashSet<>();

    @OneToOne
    private Template template;

    @OneToOne
    private Credential credential;

    @OneToOne
    private Cluster cluster;

    @ManyToOne
    @JoinColumn(name = "stack_user")
    private User user;

    @Version
    private Long version;

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

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getCfStackId() {
        return cfStackId;
    }

    public void setCfStackId(String cfStackId) {
        this.cfStackId = cfStackId;
    }

    public String getCfStackName() {
        return cfStackName;
    }

    public void setCfStackName(String cfStackName) {
        this.cfStackName = cfStackName;
    }

    public boolean isCfStackCompleted() {
        return cfStackCompleted;
    }

    public void setCfStackCompleted(boolean cfStackCompleted) {
        this.cfStackCompleted = cfStackCompleted;
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

    public Set<InstanceMetaData> getInstanceMetaData() {
        return instanceMetaData;
    }

    public void setInstanceMetaData(Set<InstanceMetaData> instanceMetaData) {
        this.instanceMetaData = instanceMetaData;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
