package com.sequenceiq.remoteenvironment.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;

@Entity
@Table(name = "private_control_plane")
public class PrivateControlPlane implements AuthResource, AccountAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "private_control_plane_generator")
    @SequenceGenerator(name = "private_control_plane_generator", sequenceName = "private_control_plane_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String resourceCrn;

    @Column(nullable = false)
    private String accountId;

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PrivateControlPlane{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", accountId='" + accountId + '\'' +
                '}';
    }
}
