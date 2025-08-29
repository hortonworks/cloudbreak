package com.sequenceiq.redbeams.domain;

import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Where;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.DatabaseVendorConverter;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.archive.ArchivableResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.converter.CrnConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.converter.ResourceStatusConverter;

@Entity
@Where(clause = "archived = false")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "deletionTimestamp", "environmentId"}))
public class DatabaseServerConfig implements ArchivableResource, AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "databaseserverconfig_generator")
    @SequenceGenerator(name = "databaseserverconfig_generator", sequenceName = "databaseserverconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Convert(converter = CrnConverter.class)
    @Column(nullable = false)
    private Crn resourceCrn;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column
    private String host;

    @Column
    private Integer port;

    @Column(nullable = false)
    @Convert(converter = DatabaseVendorConverter.class)
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

    @Column(nullable = false)
    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus resourceStatus;

    @OneToOne
    private DBStack dbStack;

    @Column
    private boolean archived;

    @Column
    private Long deletionTimestamp = -1L;

    @Column(nullable = false)
    private String environmentId;

    @OneToMany(mappedBy = "server", fetch = FetchType.EAGER)
    private Set<DatabaseConfig> databases;

    @Column
    private String clusterCrn;

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

    public Crn getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(Crn resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
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

    public String getConnectionUserName() {
        return connectionUserName.getRaw();
    }

    public String getConnectionUserNameSecret() {
        return connectionUserName.getSecret();
    }

    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName = new Secret(connectionUserName);
    }

    public String getConnectionPassword() {
        return connectionPassword.getRaw();
    }

    public String getConnectionPasswordSecret() {
        return connectionPassword.getSecret();
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = new Secret(connectionPassword);
    }

    public void setConnectionPasswordSecret(Secret connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public ResourceStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(ResourceStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    @Override
    public void setDeletionTimestamp(Long timestampMillisecs) {
        deletionTimestamp = timestampMillisecs;
    }

    public boolean isArchived() {
        return archived;
    }

    @Override
    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public void unsetRelationsToEntitiesToBeDeleted() {
        dbStack = null;
        if (databases != null) {
            databases.clear();
        }
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public Set<DatabaseConfig> getDatabases() {
        return databases;
    }

    @VisibleForTesting
    public void setDatabases(Set<DatabaseConfig> databases) {
        this.databases = databases;
    }

    public Optional<DBStack> getDbStack() {
        return Optional.ofNullable(dbStack);
    }

    public void setDbStack(DBStack dbStack) {
        this.dbStack = dbStack;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public void setClusterCrn(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    /**
     * Creates a database based on this database server config.
     *
     * @param databaseName         the name of the database
     * @param type                 the type of the database
     * @param databaseDescription  the description of the database
     * @param status               the resource status of the database
     * @param userName             the username for the user associated with the database
     * @param password             the password for the user associated with the database
     * @return a DatabaseConfig
     * @throws IllegalStateException if this database server config lacks a host or port
     */
    public DatabaseConfig createDatabaseConfig(String databaseName, String type, Optional<String> databaseDescription, ResourceStatus status,
        String userName, String password) {
        if (host == null) {
            throw new IllegalStateException("Database server config has no host");
        }
        if (port == null) {
            throw new IllegalStateException("Database server config has no port");
        }

        DatabaseConfig databaseConfig = new DatabaseConfig();

        databaseConfig.setDatabaseVendor(databaseVendor);
        databaseConfig.setName(databaseName);
        databaseConfig.setDescription(databaseDescription.orElse(description));
        databaseConfig.setConnectionURL(new DatabaseCommon().getJdbcConnectionUrl(databaseVendor.jdbcUrlDriverId(),
            host, port, Optional.of(databaseName)));
        databaseConfig.setConnectionDriver(connectionDriver);
        databaseConfig.setConnectionUserName(userName);
        databaseConfig.setConnectionPassword(password);
        databaseConfig.setStatus(status);
        databaseConfig.setType(type);
        databaseConfig.setEnvironmentId(environmentId);
        databaseConfig.setServer(this);

        return databaseConfig;
    }
}
