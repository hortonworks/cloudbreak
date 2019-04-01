package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.sequenceiq.cloudbreak.domain.converter.EncryptionConverter;

@Entity
public class SecretKey {

    @Id
    private Long id;

    @Convert(converter = EncryptionConverter.class)
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
