package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account", "name"})
})
@NamedQueries({
        @NamedQuery(
                name = "FlexSubscription.findById",
                query = "SELECT f FROM FlexSubscription f WHERE f.id= :id"
        ),
        @NamedQuery(
                name = "FlexSubscription.findOneByName",
                query = "SELECT f FROM FlexSubscription f WHERE f.name= :name"
        ),
        @NamedQuery(
                name = "FlexSubscription.findOneByNameInAccount",
                query = "SELECT f FROM FlexSubscription f WHERE f.name= :name AND ((f.account= :account AND f.publicInAccount= true) OR f.owner= :owner)"
        ),
        @NamedQuery(
                name = "FlexSubscription.findByOwner",
                query = "SELECT f FROM FlexSubscription f WHERE f.owner= :owner"
        ),
        @NamedQuery(
                name = "FlexSubscription.findPublicInAccountForUser",
                query = "SELECT f FROM FlexSubscription f WHERE (f.account= :account AND f.publicInAccount= true) OR f.owner= :owner"
        ),
        @NamedQuery(
                name = "FlexSubscription.findAllInAccount",
                query = "SELECT f FROM FlexSubscription f WHERE f.account= :account"
        )
})
public class FlexSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "flexsubscription_generator")
    @SequenceGenerator(name = "flexsubscription_generator", sequenceName = "flexsubscription_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subscriptionId;

    @ManyToOne
    private SmartSenseSubscription smartSenseSubscription;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public SmartSenseSubscription getSmartSenseSubscription() {
        return smartSenseSubscription;
    }

    public void setSmartSenseSubscription(SmartSenseSubscription smartSenseSubscription) {
        this.smartSenseSubscription = smartSenseSubscription;
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

    @Override
    public String toString() {
        return "FlexSubscription{" + "id=" + id
                + ", name='" + name + '\''
                + ", subscriptionId='" + subscriptionId + '\''
                + ", smartSenseSubscription='" + smartSenseSubscription.toString() + '\''
                + ", owner='" + owner + '\''
                + ", account='" + account + '\''
                + ", publicInAccount=" + publicInAccount + '}';
    }
}
