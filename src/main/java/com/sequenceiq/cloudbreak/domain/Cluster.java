package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "Cluster", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "cluster_user", "name" })
})
@NamedQuery(
        name = "Cluster.findAllClusterByBlueprint",
        query = "SELECT c FROM Cluster c "
                + "WHERE c.blueprint.id= :id")
public class Cluster implements ProvisionEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Blueprint blueprint;

    @ManyToOne
    @JoinColumn(name = "cluster_user")
    private User user;

    @Column(nullable = false)
    private String name;

    private String description;

    private Status status;

    private Long creationStarted;

    private Long creationFinished;

    private String statusReason;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

}
