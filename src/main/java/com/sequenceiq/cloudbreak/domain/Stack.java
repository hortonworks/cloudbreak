package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
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
                        + "AND c.status= 0")
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

    private Status status;

    private String cfStackName;

    private String cfStackId;

    private String ambariIp;

    @OneToOne
    private Template template;

    @OneToOne
    private Credential credential;

    @OneToOne
    private Cluster cluster;

    @ManyToOne
    private User user;

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

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

}
