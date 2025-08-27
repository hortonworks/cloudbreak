package com.sequenceiq.cloudbreak.domain;

import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Where;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.DatabaseVendorConverter;
import com.sequenceiq.cloudbreak.domain.converter.RdsSslModeConverter;
import com.sequenceiq.cloudbreak.domain.converter.ResourceStatusConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
@Where(clause = "archived = false")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class RDSConfig implements ProvisionEntity, WorkspaceAwareResource, ArchivableResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "rdsconfig_generator")
    @SequenceGenerator(name = "rdsconfig_generator", sequenceName = "rdsconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String connectionURL;

    @Convert(converter = RdsSslModeConverter.class)
    private RdsSslMode sslMode;

    @Column(nullable = false)
    @Convert(converter = DatabaseVendorConverter.class)
    private DatabaseVendor databaseEngine;

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
    private String stackVersion;

    @Column(nullable = false)
    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    @Where(clause = "status != 'DELETE_COMPLETED'")
    @ManyToMany(mappedBy = "rdsConfigs")
    private Set<Cluster> clusters;

    @Column(nullable = false)
    private String type;

    @Column
    private String connectorJarUrl;

    @ManyToOne
    private Workspace workspace;

    private boolean archived;

    private Long deletionTimestamp = -1L;

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

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public RdsSslMode getSslMode() {
        return sslMode;
    }

    public void setSslMode(RdsSslMode sslMode) {
        this.sslMode = sslMode;
    }

    public DatabaseVendor getDatabaseEngine() {
        return databaseEngine;
    }

    public void setDatabaseEngine(DatabaseVendor databaseEngine) {
        this.databaseEngine = databaseEngine;
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

    public Secret getConnectionPasswordSecretObject() {
        return connectionPassword;
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = new Secret(connectionPassword);
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Set<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(Set<Cluster> clusters) {
        this.clusters = clusters;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public String getConnectorJarUrl() {
        return connectorJarUrl;
    }

    public void setConnectorJarUrl(String connectorJarUrl) {
        this.connectorJarUrl = connectorJarUrl;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RDSConfig that = (RDSConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public void setDeletionTimestamp(Long timestampMillisecs) {
        deletionTimestamp = timestampMillisecs;
    }

    @Override
    public void setArchived(boolean archived) {
        this.archived = true;
    }

    @Override
    public void unsetRelationsToEntitiesToBeDeleted() {

    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public boolean isArchived() {
        return archived;
    }

    @Override
    public String toString() {
        return "RDSConfig{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", connectionURL='" + connectionURL + '\'' +
                ", sslMode=" + sslMode +
                ", databaseEngine=" + databaseEngine +
                ", connectionDriver='" + connectionDriver + '\'' +
                ", connectionUserName=" + connectionUserName +
                ", connectionPassword=" + connectionPassword +
                ", creationDate=" + creationDate +
                ", stackVersion='" + stackVersion + '\'' +
                ", status=" + status +
                ", clusters=" + clusters +
                ", type='" + type + '\'' +
                ", connectorJarUrl='" + connectorJarUrl + '\'' +
                ", workspace=" + workspace +
                ", archived=" + archived +
                ", deletionTimestamp=" + deletionTimestamp +
                '}';
    }
}
