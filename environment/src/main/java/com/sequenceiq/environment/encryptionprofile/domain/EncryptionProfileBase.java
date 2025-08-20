package com.sequenceiq.environment.encryptionprofile.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.common.database.StringListToStringConverter;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.encryptionprofile.converter.ResourceStatusConverter;
import com.sequenceiq.environment.encryptionprofile.converter.TlsVersionConverter;

@MappedSuperclass
public class EncryptionProfileBase implements Serializable, AuthResource, AccountAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "encryptionprofile_generator")
    @SequenceGenerator(name = "encryptionprofile_generator", sequenceName = "encryptionprofile_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = TlsVersionConverter.class)
    @Column(name = "tls_versions", nullable = false)
    private Set<TlsVersion> tlsVersions;

    @Convert(converter = StringListToStringConverter.class)
    @Column(name = "cipher_suites", length = 1000000, columnDefinition = "TEXT")
    private List<String> cipherSuites;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    @Convert(converter = ResourceStatusConverter.class)
    @Column(name = "resourcestatus", nullable = false)
    private ResourceStatus resourceStatus;

    @Column(columnDefinition = "boolean default false")
    private boolean archived;

    private Long created = System.currentTimeMillis();

    public void setId(Long id) {
        this.id = id;
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

    public Set<TlsVersion> getTlsVersions() {
        return tlsVersions;
    }

    public void setTlsVersions(Set<TlsVersion> tlsVersions) {
        this.tlsVersions = tlsVersions;
    }

    public List<String> getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(List<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public ResourceStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(ResourceStatus status) {
        this.resourceStatus = status;
    }

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

    @Override
    public String toString() {
        return "EncryptionProfileBase{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", tlsVersions='" + tlsVersions + '\'' +
                ", cipherSuites='" + cipherSuites + '\'' +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", created='" + created + '\'' +
                ", resourceStatus='" + resourceStatus +
                '}';
    }
}
