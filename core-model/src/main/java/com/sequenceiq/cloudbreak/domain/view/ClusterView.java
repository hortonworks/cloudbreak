package com.sequenceiq.cloudbreak.domain.view;

import static com.sequenceiq.cloudbreak.api.model.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "Cluster")
public class ClusterView implements ProvisionEntity {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private StackView stack;

    private String name;

    private String owner;

    private String ambariIp;

    private Boolean emailNeeded;

    private String emailTo;

    @Enumerated(EnumType.STRING)
    private Status status;

    public Long getId() {
        return id;
    }

    public StackView getStackView() {
        return stack;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public Boolean getEmailNeeded() {
        return emailNeeded;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isRequested() {
        return REQUESTED.equals(status);
    }

    public boolean isStartRequested() {
        return START_REQUESTED.equals(status);
    }
}
