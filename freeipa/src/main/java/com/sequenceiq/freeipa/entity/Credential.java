package com.sequenceiq.freeipa.entity;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.secret.domain.Secret;
import com.sequenceiq.secret.domain.SecretToString;

@Entity
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "credential_generator")
    @SequenceGenerator(name = "credential_generator", sequenceName = "credential_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret attributes = Secret.EMPTY;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAttributes() {
        return attributes.getRaw();
    }

    public void setAttributes(Secret attributes) {
        this.attributes = attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = new Secret(attributes);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
