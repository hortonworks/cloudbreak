package com.sequenceiq.redbeams.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.converter.database.CrnConverter;
import com.sequenceiq.secret.domain.Secret;
import com.sequenceiq.secret.domain.SecretToString;

@Entity
public class DatabaseConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "databaseconfig_generator")
    @SequenceGenerator(name = "databaseconfig_generator", sequenceName = "databaseconfig_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = CrnConverter.class)
    @Column(nullable = false)
    private Crn crn;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "connectionurl")
    private String connectionURL;

    @Column(nullable = false, name = "database_vendor")
    @Enumerated(EnumType.STRING)
    private DatabaseVendor databaseVendor;

    @Column(nullable = false)
    private String connectionDriver;

    @Convert(converter = SecretToString.class)
    @SecretValue
    @Column(nullable = false)
    private Secret connectionUserName = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    @Column(nullable = false)
    private Secret connectionPassword = Secret.EMPTY;

    private Long creationDate;

    @Column(nullable = false, name = "resourcestatus")
    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @Column(nullable = false)
    private String type;

    @Column
    private String connectorJarUrl;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    @Column(nullable = false, name = "environment_id")
    private String environmentId;

    public Long getId() {
        return id;
    }

    public Crn getCrn() {
        return crn;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public DatabaseVendor getDatabaseVendor() {
        return databaseVendor;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public Secret getConnectionUserName() {
        return connectionUserName;
    }

    public Secret getConnectionPassword() {
        return connectionPassword;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getConnectorJarUrl() {
        return connectorJarUrl;
    }

    public boolean isArchived() {
        return archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCrn(Crn crn) {
        this.crn = crn;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public void setDatabaseVendor(DatabaseVendor databaseVendor) {
        this.databaseVendor = databaseVendor;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName = new Secret(connectionUserName);
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = new Secret(connectionPassword);
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setConnectorJarUrl(String connectorJarUrl) {
        this.connectorJarUrl = connectorJarUrl;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setDeletionTimestamp(Long deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

    public void setEnvironmentId(String environment) {
        this.environmentId = environment;
    }
}
