package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Where;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@Entity
@Where(clause = "archived = false")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class RDSConfig implements ProvisionEntity, EnvironmentAwareResource, ArchivableResource {

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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
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
    @Enumerated(EnumType.STRING)
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

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "env_rds", joinColumns = @JoinColumn(name = "rdsid"), inverseJoinColumns = @JoinColumn(name = "envid"))
    private Set<EnvironmentView> environments = new HashSet<>();

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
    public Set<EnvironmentView> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<EnvironmentView> environments) {
        this.environments = environments;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.DATABASE;
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
}
