package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
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

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.aspect.vault.VaultValue;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class RDSConfig implements ProvisionEntity, EnvironmentAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "rdsconfig_generator")
    @SequenceGenerator(name = "rdsconfig_generator", sequenceName = "rdsconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String connectionURL;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DatabaseVendor databaseEngine;

    @Column(nullable = false)
    private String connectionDriver;

    @VaultValue
    @Column(nullable = false)
    private String connectionUserName;

    @VaultValue
    @Column(nullable = false)
    private String connectionPassword;

    private Long creationDate;

    @Column(nullable = false)
    private String stackVersion;

    private String owner;

    private String account;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @ManyToMany(mappedBy = "rdsConfigs")
    private Set<Cluster> clusters;

    @Column(nullable = false)
    private String type;

    @Column
    private String connectorJarUrl;

    @ManyToOne
    private Workspace workspace;

    @ManyToMany(cascade = {CascadeType.MERGE}, fetch = FetchType.EAGER)
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
        return connectionUserName;
    }

    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName = connectionUserName;
    }

    public String getConnectionPassword() {
        return connectionPassword;
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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
        return WorkspaceResource.RDS;
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
}
