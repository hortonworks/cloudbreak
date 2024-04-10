package com.sequenceiq.remoteenvironment.domain;


import java.util.Objects;

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

    @Column(nullable = false)
    private String url;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PrivateControlPlane{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", url='" + url + '\'' +
                ", accountId='" + accountId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrivateControlPlane that = (PrivateControlPlane) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(resourceCrn, that.resourceCrn)
                && Objects.equals(accountId, that.accountId)
                && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, resourceCrn, accountId, url);
    }
}
