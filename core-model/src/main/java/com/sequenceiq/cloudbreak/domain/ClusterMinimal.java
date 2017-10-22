package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "Cluster", uniqueConstraints = @UniqueConstraint(columnNames = {"account", "name"}))
public class ClusterMinimal implements ProvisionEntity {

    @Id
    private Long id;

    @OneToOne
    private Stack stack;

    private String name;

    private String owner;

    private String ambariIp;

    private Boolean emailNeeded;

    private String emailTo;

    public Stack getStack() {
        return stack;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Boolean getEmailNeeded() {
        return emailNeeded;
    }

    public void setEmailNeeded(Boolean emailNeeded) {
        this.emailNeeded = emailNeeded;
    }
}
