package com.sequenceiq.redbeams.domain.stack;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity
@Table
public class DatabaseServer implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "databaseserver_generator")
    @SequenceGenerator(name = "databaseserver_generator", sequenceName = "databaseserver_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String instanceType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DatabaseVendor databaseVendor;

    private Long storageSize;

    @Convert(converter = SecretToString.class)
    @SecretValue
    @Column(nullable = false)
    private Secret rootUserName = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    @Column(nullable = false)
    private Secret rootPassword = Secret.EMPTY;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "securitygroup_id", referencedColumnName = "id")
    private SecurityGroup securityGroup;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public DatabaseVendor getDatabaseVendor() {
        return databaseVendor;
    }

    public void setDatabaseVendor(DatabaseVendor databaseVendor) {
        this.databaseVendor = databaseVendor;
    }

    public Long getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(Long storageSize) {
        this.storageSize = storageSize;
    }

    public String getRootUserName() {
        return rootUserName.getRaw();
    }

    public String getRootUserNameSecret() {
        return rootUserName.getSecret();
    }

    public void setRootUserName(String rootUserName) {
        this.rootUserName = new Secret(rootUserName);
    }

    public String getRootPassword() {
        return rootPassword.getRaw();
    }

    public String getRootPasswordSecret() {
        return rootPassword.getSecret();
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = new Secret(rootPassword);
    }

    public SecurityGroup getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getAccountId() {
        return null;
    }
}
