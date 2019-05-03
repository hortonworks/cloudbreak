package com.sequenceiq.freeipa.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class StackAuthentication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stackauthentication_generator")
    @SequenceGenerator(name = "stackauthentication_generator", sequenceName = "stackauthentication_id_seq", allocationSize = 1)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String publicKey;

    private String publicKeyId;

    private String loginUserName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public boolean passwordAuthenticationRequired() {
        return publicKey != null && publicKey.startsWith("Basic:");
    }

    public String getLoginPassword() {
        return publicKey.replaceAll("Basic:", "").trim();
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    public void setLoginUserName(String loginUserName) {
        this.loginUserName = loginUserName;
    }
}