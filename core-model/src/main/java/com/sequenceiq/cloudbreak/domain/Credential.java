package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
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

import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" })
})
@NamedQueries({
        @NamedQuery(
                name = "Credential.findByTopology",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.topology.id= :topologyId"),
        @NamedQuery(
                name = "Credential.findForUser",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.owner= :user"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findPublicInAccountForUser",
                query = "SELECT c FROM Credential c "
                        + "WHERE ((c.account= :account AND c.publicInAccount= true) OR c.owner= :user)"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findOneByName",
                query = "SELECT b FROM Credential b "
                        + "WHERE b.name= :name AND b.account= :account"
                        + " AND b.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findAllInAccount",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.account= :account"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findByIdInAccount",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.id= :id AND c.account= :account"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findByNameInAccount",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.name= :name AND ((c.publicInAccount=true and c.account= :account) OR c.owner= :owner)"
                        + " AND c.archived IS FALSE"),
        @NamedQuery(
                name = "Credential.findByNameInUser",
                query = "SELECT c FROM Credential c "
                        + "WHERE c.owner= :owner and c.name= :name"
                        + " AND c.archived IS FALSE")
})

public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "credential_generator")
    @SequenceGenerator(name = "credential_generator", sequenceName = "credential_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String owner;
    private String account;

    private String loginUserName;

    private boolean publicInAccount;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    @Column(columnDefinition = "boolean default false")
    private boolean archived;

    @Column(nullable = false)
    private String cloudPlatform;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    @ManyToOne
    private Topology topology;

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

    public boolean isArchived() {
        return archived;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    public void setLoginUserName(String loginUserName) {
        this.loginUserName = loginUserName;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean passwordAuthenticationRequired() {
        return publicKey.startsWith("Basic:");
    }

    public String getLoginPassword() {
        return publicKey.replaceAll("Basic:", "").trim();
    }

    public String cloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public Topology getTopology() {
        return topology;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }
}
