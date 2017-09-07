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

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"account", "name"}))
public class FlexSubscription implements ProvisionEntity {

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

    @Column
    //CHECKSTYLE:OFF
    private boolean isDefault;
    //CHECKSTYLE:ON

    @Column
    private boolean usedForController;

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

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isUsedForController() {
        return usedForController;
    }

    public void setUsedForController(boolean usedForController) {
        this.usedForController = usedForController;
    }

    @Override
    public String toString() {
        return "FlexSubscription{" + "id=" + id
                + ", name='" + name + '\''
                + ", subscriptionId='" + subscriptionId + '\''
                + ", smartSenseSubscription='" + smartSenseSubscription + '\''
                + ", owner='" + owner + '\''
                + ", account='" + account + '\''
                + ", publicInAccount=" + publicInAccount + '}';
    }
}
