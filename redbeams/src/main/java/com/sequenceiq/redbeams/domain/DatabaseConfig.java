package com.sequenceiq.redbeams.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.DatabaseVendorConverter;
import com.sequenceiq.redbeams.repository.converter.ResourceStatusConverter;
import org.hibernate.annotations.Where;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.archive.ArchivableResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.converter.CrnConverter;

@Entity
@Where(clause = "archived = false")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "deletionTimestamp", "environment_id"}))
public class DatabaseConfig implements ArchivableResource, AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "databaseconfig_generator")
    @SequenceGenerator(name = "databaseconfig_generator", sequenceName = "databaseconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Convert(converter = CrnConverter.class)
    @Column(name = "crn", nullable = false)
    private Crn resourceCrn;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "connectionurl")
    private String connectionURL;

    @Column(nullable = false, name = "database_vendor")
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

    @Column(nullable = false, name = "resourcestatus")
    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    @Column(nullable = false)
    private String type;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    @Column(nullable = false, name = "environment_id")
    private String environmentId;

    @ManyToOne(optional = true)
    @JoinColumn(name = "server_id", nullable = true)
    private DatabaseServerConfig server;

    public Long getId() {
        return id;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public Crn getResourceCrn() {
        return resourceCrn;
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

    public boolean isArchived() {
        return archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public DatabaseServerConfig getServer() {
        return server;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setResourceCrn(Crn resourceCrn) {
        this.resourceCrn = resourceCrn;
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

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public void unsetRelationsToEntitiesToBeDeleted() {
        server = null;
    }

    public void setDeletionTimestamp(Long deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

    public void setEnvironmentId(String environment) {
        this.environmentId = environment;
    }

    public void setServer(DatabaseServerConfig server) {
        this.server = server;
    }
}
