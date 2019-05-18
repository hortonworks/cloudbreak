package com.sequenceiq.freeipa.entity;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.secret.domain.Secret;
import com.sequenceiq.secret.domain.SecretToString;

@Entity
public class FreeIpa {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "freeipa_generator")
    @SequenceGenerator(name = "freeipa_generator", sequenceName = "freeipa_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Stack stack;

    private String hostname;

    private String domain;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret adminPassword = Secret.EMPTY;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAdminPassword() {
        return adminPassword.getRaw();
    }

    public void setAdminPassword(Secret adminPassword) {
        this.adminPassword = adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = new Secret(adminPassword);
    }
}
