package com.sequenceiq.cdp.databus.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity
@Table(name = "account_databus_config", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "accountId"}))
public class AccountDatabusConfig implements Serializable, AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "accountdatabusconfig_generator")
    @SequenceGenerator(name = "accountdatabusconfig_generator", sequenceName = "accountdatabusconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String accountId;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret databusCredential = Secret.EMPTY;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatabusCredential() {
        return databusCredential.getRaw();
    }

    public void setDatabusCredential(String databusCredential) {
        this.databusCredential = new Secret(databusCredential);
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
