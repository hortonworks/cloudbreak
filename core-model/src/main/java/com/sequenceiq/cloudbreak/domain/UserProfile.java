package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.domain.json.EncryptedJsonToString;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Entity
@Table(name = "UserProfile", uniqueConstraints = @UniqueConstraint(columnNames = {"account", "owner"}))
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "userprofile_generator")
    @SequenceGenerator(name = "userprofile_generator", sequenceName = "userprofile_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Credential credential;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String account;

    @Convert(converter = EncryptedJsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json uiProperties;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Json getUiProperties() {
        return uiProperties;
    }

    public void setUiProperties(Json uiProperties) {
        this.uiProperties = uiProperties;
    }
}
