package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" }),
})
@NamedQueries({
        @NamedQuery(
                name = "SecurityGroup.findById",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE r.id= :id"),
        @NamedQuery(
                name = "SecurityGroup.findOneById",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE r.id= :id"),
        @NamedQuery(
                name = "SecurityGroup.findOneByName",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE r.name= :name "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "SecurityGroup.findByNameForUser",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE r.name= :name "
                        + "AND r.owner= :owner "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "SecurityGroup.findByNameInAccount",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE r.name= :name "
                        + "AND r.account= :account "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "SecurityGroup.findByName",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE r.name= :name "),
        @NamedQuery(
                name = "SecurityGroup.findForUser",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE r.owner= :owner "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "SecurityGroup.findPublicInAccountForUser",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE ((r.account= :account AND r.publicInAccount= true) OR r.owner= :owner) "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "SecurityGroup.findAllInAccount",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE r.account= :account "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "SecurityGroup.findAllDefaultInAccount",
                query = "SELECT r FROM SecurityGroup r "
                        + "LEFT JOIN FETCH r.securityRules "
                        + "WHERE r.account= :account "
                        + "AND (r.status = 'DEFAULT_DELETED' OR r.status = 'DEFAULT') ")
})
public class SecurityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "security_group_sequence_generator")
    @SequenceGenerator(name = "security_group_sequence_generator", sequenceName = "security_group_seq")
    private Long id;

    @Column(nullable = false)
    private String name;

    private String owner;

    private String account;

    private boolean publicInAccount;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @OneToMany(mappedBy = "securityGroup", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<SecurityRule> securityRules = new HashSet<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Set<SecurityRule> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(Set<SecurityRule> securityRules) {
        this.securityRules = securityRules;
    }
}
