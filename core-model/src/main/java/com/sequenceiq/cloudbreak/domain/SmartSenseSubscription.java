package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account", "subscriptionId"})
})
@NamedQueries({
        @NamedQuery(
                name = "SmartSenseSubscription.findById",
                query = "SELECT s FROM SmartSenseSubscription s WHERE s.id= :id"
        ),
        @NamedQuery(
                name = "SmartSenseSubscription.findBySubscriptionId",
                query = "SELECT s FROM SmartSenseSubscription s WHERE s.subscriptionId= :subscriptionId AND s.account= :account"
        ),
        @NamedQuery(
                name = "SmartSenseSubscription.findByOwner",
                query = "SELECT s FROM SmartSenseSubscription s WHERE s.owner= :owner"
        )
})
public class SmartSenseSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "smartsense_generator")
    @SequenceGenerator(name = "smartsense_generator", sequenceName = "smartsense_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String subscriptionId;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private boolean publicInAccount;

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

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }
}
