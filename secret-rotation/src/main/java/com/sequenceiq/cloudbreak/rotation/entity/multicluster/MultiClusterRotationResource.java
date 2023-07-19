package com.sequenceiq.cloudbreak.rotation.entity.multicluster;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.RotationEnumConverter;

@Entity
public class MultiClusterRotationResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "multiclusterrotationresource_generator")
    @SequenceGenerator(name = "multiclusterrotationresource_generator", sequenceName = "multiclusterrotationresource_id_seq", allocationSize = 1)
    private Long id;

    private String resourceCrn;

    @Convert(converter = RotationEnumConverter.class)
    private MultiSecretType secretType;

    @Convert(converter = MultiClusterRotationResourceTypeConverter.class)
    private MultiClusterRotationResourceType type;

    public MultiClusterRotationResource() {
    }

    public MultiClusterRotationResource(String resourceCrn, MultiSecretType secretType, MultiClusterRotationResourceType type) {
        this.resourceCrn = resourceCrn;
        this.secretType = secretType;
        this.type = type;
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

    public MultiSecretType getSecretType() {
        return secretType;
    }

    public void setSecretType(MultiSecretType secretType) {
        this.secretType = secretType;
    }

    public MultiClusterRotationResourceType getType() {
        return type;
    }

    public void setType(MultiClusterRotationResourceType type) {
        this.type = type;
    }
}
