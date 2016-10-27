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

import com.sequenceiq.cloudbreak.common.type.ResourceStatus;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" }),
})
@NamedQueries({
        @NamedQuery(
                name = "ConstraintTemplate.findForUser",
                query = "SELECT t FROM ConstraintTemplate t "
                        + "WHERE t.owner= :user AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "ConstraintTemplate.findPublicInAccountForUser",
                query = "SELECT t FROM ConstraintTemplate t "
                        + "WHERE ((t.account= :account AND t.publicInAccount= true) "
                        + "OR t.owner= :user) AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "ConstraintTemplate.findAllInAccount",
                query = "SELECT t FROM ConstraintTemplate t "
                        + "WHERE t.account= :account AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "ConstraintTemplate.findOneByName",
                query = "SELECT t FROM ConstraintTemplate t "
                        + "WHERE t.name= :name and t.account= :account AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "ConstraintTemplate.findByIdInAccount",
                query = "SELECT t FROM ConstraintTemplate t "
                        + "WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "ConstraintTemplate.findByNameInAccount",
                query = "SELECT t FROM ConstraintTemplate t "
                        + "WHERE t.name= :name and ((t.account= :account and t.publicInAccount=true) or t.owner= :owner) "
                        + "AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "ConstraintTemplate.findByNameInUser",
                query = "SELECT t FROM ConstraintTemplate t "
                        + "WHERE t.owner= :owner and t.name= :name AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "ConstraintTemplate.findAllDefaultInAccount",
                query = "SELECT t FROM ConstraintTemplate t "
                        + "WHERE t.account= :account "
                        + "AND (t.status = 'DEFAULT_DELETED' OR t.status = 'DEFAULT') ")
})
public class ConstraintTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "constraint_template_generator")
    @SequenceGenerator(name = "constraint_template_generator", sequenceName = "constrainttemplate_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private boolean publicInAccount;

    @Column(nullable = false)
    private Double cpu;

    @Column(nullable = false)
    private Double memory;

    @Column(nullable = false)
    private Double disk;

    @Column(nullable = false)
    private String orchestratorType;

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

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

    public Double getCpu() {
        return cpu;
    }

    public void setCpu(Double cpu) {
        this.cpu = cpu;
    }

    public Double getMemory() {
        return memory;
    }

    public void setMemory(Double memory) {
        this.memory = memory;
    }

    public Double getDisk() {
        return disk;
    }

    public void setDisk(Double disk) {
        this.disk = disk;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String getOrchestratorType() {
        return orchestratorType;
    }

    public void setOrchestratorType(String orchestratorType) {
        this.orchestratorType = orchestratorType;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
