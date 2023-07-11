package com.sequenceiq.cloudbreak.rotation.entity;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.rotation.SecretType;

@Entity
public class MultiClusterSecretRotation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "multiclustersecretrotation_generator")
    @SequenceGenerator(name = "multiclustersecretrotation_generator", sequenceName = "multiclustersecretrotation_id_seq", allocationSize = 1)
    private Long id;

    private String resourceCrn;

    @Convert(converter = RotationEnumConverter.class)
    private SecretType secretType;

    @Convert(converter = MultiClusterRotationResourceTypeConverter.class)
    private MultiClusterRotationResourceType resourceType;

    public MultiClusterSecretRotation() {
    }

    public MultiClusterSecretRotation(String resourceCrn, SecretType secretType, MultiClusterRotationResourceType resourceType) {
        this.resourceCrn = resourceCrn;
        this.secretType = secretType;
        this.resourceType = resourceType;
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

    public MultiClusterRotationResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(MultiClusterRotationResourceType resourceType) {
        this.resourceType = resourceType;
    }
}
