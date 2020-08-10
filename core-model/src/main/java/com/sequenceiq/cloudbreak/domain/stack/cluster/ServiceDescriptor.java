package com.sequenceiq.cloudbreak.domain.stack.cluster;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.common.api.util.ProvisionEntity;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

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
    @Column(columnDefinition = "TEXT")
    private Json blueprintParams;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret blueprintSecretParams = Secret.EMPTY;

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

    public Json getBlueprintParams() {
        return blueprintParams;
    }

    public void setBlueprintParam(Json blueprintParam) {
        blueprintParams = blueprintParam;
    }

    public Json getBlueprintSecretParams() {
        return new Json(blueprintSecretParams.getRaw());
    }

    public void setBlueprintSecretParams(Json blueprintSecretParams) {
        this.blueprintSecretParams = new Secret(blueprintSecretParams.getValue());
    }

    public Json getComponentsHosts() {
        return componentsHosts;
    }

    public void setComponentsHosts(Json componentsHosts) {
        this.componentsHosts = componentsHosts;
    }
}
