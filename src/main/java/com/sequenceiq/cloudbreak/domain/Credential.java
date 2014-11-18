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
        @UniqueConstraint(columnNames = { "account", "name" })
})
@NamedQueries({
        @NamedQuery(
                name = "Credential.findForUser",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.owner= :user"),
        @NamedQuery(
                name = "Credential.findPublicsInAccount",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.account= :account "
                        + "AND c.publicInAccount= true"),
        @NamedQuery(
                name = "Credential.findAllInAccount",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.account= :account ")
})
public abstract class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credential_generator")
    @SequenceGenerator(name = "credential_generator", sequenceName = "credential_table")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private CloudPlatform cloudPlatform;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String owner;
    private String account;

    private boolean publicInAccount;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    public Credential() {

    }

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

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public abstract CloudPlatform cloudPlatform();

}
