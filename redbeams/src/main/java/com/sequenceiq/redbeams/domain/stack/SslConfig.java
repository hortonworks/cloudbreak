package com.sequenceiq.redbeams.domain.stack;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.repository.converter.SslCertificateTypeConverter;

@Entity
@Table
public class SslConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sslconfig_generator")
    @SequenceGenerator(name = "sslconfig_generator", sequenceName = "sslconfig_id_seq", allocationSize = 1)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "sslcertificate_value")
    private Set<String> sslCertificates = new HashSet<>();

    @Convert(converter = SslCertificateTypeConverter.class)
    @Column(nullable = false)
    private SslCertificateType sslCertificateType = SslCertificateType.NONE;

    private Integer sslCertificateActiveVersion;

    private String sslCertificateActiveCloudProviderIdentifier;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<String> getSslCertificates() {
        return sslCertificates;
    }

    public void setSslCertificates(Set<String> sslCertificates) {
        this.sslCertificates = sslCertificates;
    }

    public SslCertificateType getSslCertificateType() {
        return sslCertificateType;
    }

    public void setSslCertificateType(SslCertificateType sslCertificateType) {
        this.sslCertificateType = sslCertificateType;
    }

    public Integer getSslCertificateActiveVersion() {
        return sslCertificateActiveVersion;
    }

    public void setSslCertificateActiveVersion(Integer sslCertificateActiveVersion) {
        this.sslCertificateActiveVersion = sslCertificateActiveVersion;
    }

    public String getSslCertificateActiveCloudProviderIdentifier() {
        return sslCertificateActiveCloudProviderIdentifier;
    }

    public void setSslCertificateActiveCloudProviderIdentifier(String sslCertificateActiveCloudProviderIdentifier) {
        this.sslCertificateActiveCloudProviderIdentifier = sslCertificateActiveCloudProviderIdentifier;
    }

    @Override
    public String toString() {
        return "SslConfig{" +
                "id=" + id +
                ", sslCertificates=" + sslCertificates +
                ", sslCertificateType=" + sslCertificateType +
                ", sslCertificateActiveVersion=" + sslCertificateActiveVersion +
                ", sslCertificateActiveCloudProviderIdentifier='" + sslCertificateActiveCloudProviderIdentifier + '\'' +
                '}';
    }

}
