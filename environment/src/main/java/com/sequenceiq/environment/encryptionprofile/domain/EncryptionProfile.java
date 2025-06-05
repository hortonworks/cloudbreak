package com.sequenceiq.environment.encryptionprofile.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Where;

@Entity
@Where(clause = "archived = false")
@Table
public class EncryptionProfile extends EncryptionProfileBase {

    @Override
    public String toString() {
        return super.toString();
    }
}
