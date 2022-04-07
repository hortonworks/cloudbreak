package com.sequenceiq.consumption.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;

@Entity
@Table
public class Consumption implements AuthResource, AccountAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "consumption_generator")
    @SequenceGenerator(name = "consumption_generator", sequenceName = "consumption_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    public Consumption() {

    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getResourceName() {
        return name;
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

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    @Override
    public String toString() {
        return "Consumption{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }
}
