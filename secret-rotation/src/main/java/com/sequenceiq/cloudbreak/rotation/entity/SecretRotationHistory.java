package com.sequenceiq.cloudbreak.rotation.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.rotation.SecretType;

@Entity
public class SecretRotationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "secretrotationhistory_generator")
    @SequenceGenerator(name = "secretrotationhistory_generator", sequenceName = "secretrotationhistory_id_seq", allocationSize = 1)
    private Long id;

    private String resourceCrn;

    @Convert(converter = RotationEnumConverter.class)
    private SecretType secretType;

    private Long lastUpdated;

    public SecretRotationHistory() {
    }

    public SecretRotationHistory(String resourceCrn, SecretType secretType, Long lastUpdated) {
        this.resourceCrn = resourceCrn;
        this.secretType = secretType;
        this.lastUpdated = lastUpdated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public SecretType getSecretType() {
        return secretType;
    }

    public void setSecretType(SecretType secretType) {
        this.secretType = secretType;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "SecretRotationHistory{" +
                "resourceCrn='" + resourceCrn + '\'' +
                ", secretType=" + secretType +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
