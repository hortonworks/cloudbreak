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

import com.sequenceiq.cloudbreak.domain.organization.Organization;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"account", "subscriptionId"}))
public class SmartSenseSubscription implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "smartsense_generator")
    @SequenceGenerator(name = "smartsense_generator", sequenceName = "smartsense_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String subscriptionId;

    private String owner;

    private String account;

    @ManyToOne
    private Organization organization;

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
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

    @Override
    public String toString() {
        return "SmartSenseSubscription{" + "id=" + id
                + ", subscriptionId='" + subscriptionId + '\''
                + ", owner='" + owner + '\''
                + ", account='" + account + '}';
    }
}
