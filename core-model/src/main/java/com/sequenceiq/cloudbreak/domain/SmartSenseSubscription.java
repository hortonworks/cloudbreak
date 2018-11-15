package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "subscriptionId"}))
public class SmartSenseSubscription implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "smartsense_generator")
    @SequenceGenerator(name = "smartsense_generator", sequenceName = "smartsense_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String subscriptionId;

    @ManyToOne
    private Workspace workspace;

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public String toString() {
        return "SmartSenseSubscription{" + "id=" + id
                + ", subscriptionId='" + subscriptionId + '}';
    }
}
