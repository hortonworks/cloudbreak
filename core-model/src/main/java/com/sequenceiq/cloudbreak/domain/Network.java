package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
        @UniqueConstraint(columnNames = { "account", "name" }),
})
@NamedQueries({
        @NamedQuery(
                name = "Network.findOneById",
                query = "SELECT r FROM Network r "
                        + "WHERE r.id= :id"),
        @NamedQuery(
                name = "Network.findOneByName",
                query = "SELECT r FROM Network r "
                        + "WHERE r.name= :name "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Network.findByNameForUser",
                query = "SELECT r FROM Network r "
                        + "WHERE r.name= :name "
                        + "AND r.owner= :owner "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Network.findByNameInAccount",
                query = "SELECT r FROM Network r "
                        + "WHERE r.name= :name "
                        + "AND r.account= :account "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Network.findByName",
                query = "SELECT r FROM Network r "
                        + "WHERE r.name= :name "),
        @NamedQuery(
                name = "Network.findForUser",
                query = "SELECT r FROM Network r "
                        + "WHERE r.owner= :owner "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Network.findPublicInAccountForUser",
                query = "SELECT r FROM Network r "
                        + "WHERE ((r.account= :account AND r.publicInAccount= true) OR r.owner= :owner) "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Network.findAllInAccount",
                query = "SELECT r FROM Network r "
                        + "WHERE r.account= :account "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Network.findAllDefaultInAccount",
                query = "SELECT r FROM Network r "
                        + "WHERE r.account= :account "
                        + "AND (r.status = 'DEFAULT_DELETED' OR r.status = 'DEFAULT') ")
})
public abstract class Network {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "network_generator")
    @SequenceGenerator(name = "network_generator", sequenceName = "network_table")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subnetCIDR;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String owner;
    private String account;

    private boolean publicInAccount;

    @Enumerated(EnumType.STRING)
    private NetworkStatus status;

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

    public String getSubnetCIDR() {
        return subnetCIDR;
    }

    public void setSubnetCIDR(String subnetCIDR) {
        this.subnetCIDR = subnetCIDR;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public NetworkStatus getStatus() {
        return status;
    }

    public void setStatus(NetworkStatus status) {
        this.status = status;
    }

    public abstract CloudPlatform cloudPlatform();
}
