package com.sequenceiq.redbeams.domain.stack;

import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.DatabaseVendorConverter;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity
@Table
public class DatabaseServer implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "databaseserver_generator")
    @SequenceGenerator(name = "databaseserver_generator", sequenceName = "databaseserver_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String instanceType;

    @Column(nullable = false)
    @Convert(converter = DatabaseVendorConverter.class)
    private DatabaseVendor databaseVendor;

    @Column
    private String connectionDriver;

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

    private Integer port;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DatabaseServer that = (DatabaseServer) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(instanceType, that.instanceType) &&
                databaseVendor == that.databaseVendor &&
                Objects.equals(connectionDriver, that.connectionDriver) &&
                Objects.equals(storageSize, that.storageSize) &&
                Objects.equals(rootUserName, that.rootUserName) &&
                Objects.equals(rootPassword, that.rootPassword) &&
                Objects.equals(securityGroup, that.securityGroup) &&
                Objects.equals(port, that.port) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountId, name, description, instanceType, databaseVendor, connectionDriver, storageSize, rootUserName, rootPassword,
                securityGroup, port, attributes);
    }
}