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
        @UniqueConstraint(columnNames = { "account", "name" })
})
@NamedQueries({
        @NamedQuery(
                name = "Credential.findForUser",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.owner= :user"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findPublicInAccountForUser",
                query = "SELECT c FROM Credential c "
                        + "WHERE (c.account= :account AND c.publicInAccount= true) "
                        + "OR c.owner= :user"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findOneByName",
                query = "SELECT b FROM Credential b "
                        + "WHERE b.name= :name and b.account= :account"
                        + " AND b.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findAllInAccount",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.account= :account"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findByIdInAccount",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.id= :id and c.account= :account"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findByNameInAccount",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.name= :name and ((c.publicInAccount=true and c.account= :account) or c.owner= :owner)"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findByNameInUser",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.owner= :owner and c.name= :name"
                        + " AND c.archived IS FALSE")
})

public abstract class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credential_generator")
    @SequenceGenerator(name = "credential_generator", sequenceName = "credential_table")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String owner;
    private String account;

    private boolean publicInAccount;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    @Column(columnDefinition = "boolean default false")
    private boolean archived;

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

    public abstract CloudPlatform cloudPlatform();

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
