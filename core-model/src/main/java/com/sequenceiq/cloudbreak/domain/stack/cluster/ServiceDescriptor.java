package com.sequenceiq.cloudbreak.domain.stack.cluster;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Secret;
import com.sequenceiq.cloudbreak.domain.SecretToString;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

@Entity
public class ServiceDescriptor implements ProvisionEntity, WorkspaceAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "servicedescriptor_generator")
    @SequenceGenerator(name = "servicedescriptor_generator", sequenceName = "servicedescriptor_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private Workspace workspace;

    @ManyToOne
    private DatalakeResources datalakeResources;

    private String serviceName;

    @Convert(converter = JsonToString.class)
    @Column(name = "blueprintParams", columnDefinition = "TEXT")
    private Json clusterDefinitionParams;

    @Convert(converter = SecretToString.class)
    @SecretValue
    @Column(name = "blueprintSecretParams")
    private Secret clusterDefinitionSecretParams = Secret.EMPTY;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json componentsHosts;

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public String getName() {
        return "servicedescriptor-" + id;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.SERVICE_DESCRIPTOR;
    }

    public DatalakeResources getDatalakeResources() {
        return datalakeResources;
    }

    public void setDatalakeResources(DatalakeResources datalakeResources) {
        this.datalakeResources = datalakeResources;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Json getClusterDefinitionParams() {
        return clusterDefinitionParams;
    }

    public void setClusterDefinitionParam(Json clusterDefinitionParam) {
        this.clusterDefinitionParams = clusterDefinitionParam;
    }

    public Json getClusterDefinitionSecretParams() {
        return new Json(clusterDefinitionSecretParams.getRaw());
    }

    public void setClusterDefinitionSecretParams(Json clusterDefinitionSecretParams) {
        this.clusterDefinitionSecretParams = new Secret(clusterDefinitionSecretParams.getValue());
    }

    public Json getComponentsHosts() {
        return componentsHosts;
    }

    public void setComponentsHosts(Json componentsHosts) {
        this.componentsHosts = componentsHosts;
    }
}
