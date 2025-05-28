package com.sequenceiq.environment.environment.dto;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

public class EncryptionProfileDto implements AccountAwareResource {

    private Long id;

    private String name;

    private String description;

    private Set<TlsVersion> tlsVersions;

    private Set<String> cipherSuites;

    private String accountId;

    private String resourceCrn;

    private Long created;

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

    public Set<String> getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(Set<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public Set<TlsVersion> getTlsVersions() {
        return tlsVersions;
    }

    public void setTlsVersions(Set<TlsVersion> tlsVersions) {
        this.tlsVersions = tlsVersions;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "EncryptionProfileDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", tlsVersions=" + tlsVersions +
                ", cipherSuites=" + cipherSuites +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", created=" + created +
                '}';
    }

    public static final class Builder {
        private Long id;

        private String name;

        private String description;

        private Set<TlsVersion> tlsVersions;

        private Set<String> cipherSuites;

        private String accountId;

        private String resourceCrn;

        private Long created;

        private Builder() {

        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withTlsVersions(Set<TlsVersion> tlsVersions) {
            this.tlsVersions = tlsVersions;
            return this;
        }

        public Builder withCipherSuites(Set<String> cipherSuites) {
            this.cipherSuites = cipherSuites;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withCreated(Long created) {
            this.created = created;
            return this;
        }

        public EncryptionProfileDto build() {
            EncryptionProfileDto encryptionProfileDto = new EncryptionProfileDto();
            encryptionProfileDto.setId(id);
            encryptionProfileDto.setName(name);
            encryptionProfileDto.setDescription(description);
            encryptionProfileDto.setTlsVersions(tlsVersions);
            encryptionProfileDto.setCipherSuites(cipherSuites);
            encryptionProfileDto.setAccountId(accountId);
            encryptionProfileDto.setResourceCrn(resourceCrn);
            encryptionProfileDto.setCreated(created);

            return encryptionProfileDto;
        }
    }
}
