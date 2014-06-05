package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Cluster {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Blueprint blueprint;

    @ManyToOne
    private User user;

    private String name;

    private Status status;

    private Long creationStarted;

    private Long creationFinished;

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

}
