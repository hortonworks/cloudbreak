package com.sequenceiq.cloudbreak.converter.v4.database;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;

@Component
public class RDSConfigToDatabaseV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigToDatabaseV4ResponseConverter.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @Inject
    private WorkspaceToWorkspaceResourceV4ResponseConverter workspaceToWorkspaceResourceV4ResponseConverter;

    public DatabaseV4Response convert(RDSConfig source) {
        DatabaseV4Response response = new DatabaseV4Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setConnectionURL(source.getConnectionURL());
        response.setDatabaseEngine(source.getDatabaseEngine().name());
        response.setConnectionDriver(source.getConnectionDriver());
        response.setConnectionUserName(stringToSecretResponseConverter.convert(source.getConnectionUserNameSecret()));
        response.setConnectionPassword(stringToSecretResponseConverter.convert(source.getConnectionPasswordSecret()));
        response.setDatabaseEngineDisplayName(source.getDatabaseEngine().displayName());
        response.setCreationDate(source.getCreationDate());
        response.setClusterNames(clusterService.findNamesByRdsConfig(source.getId()));
        response.setType(source.getType());
        response.setWorkspace(workspaceToWorkspaceResourceV4ResponseConverter.convert(source.getWorkspace()));
        return response;
    }

    public DatabaseV4Response convert(RdsConfigWithoutCluster source, WorkspaceResourceV4Response workspaceResponse) {
        DatabaseV4Response response = new DatabaseV4Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setConnectionURL(source.getConnectionURL());
        response.setDatabaseEngine(source.getDatabaseEngine().name());
        response.setConnectionDriver(source.getConnectionDriver());
        response.setConnectionUserName(stringToSecretResponseConverter.convert(source.getConnectionUserNamePath()));
        response.setConnectionPassword(stringToSecretResponseConverter.convert(source.getConnectionPasswordPath()));
        response.setDatabaseEngineDisplayName(source.getDatabaseEngine().displayName());
        response.setCreationDate(source.getCreationDate());
        response.setClusterNames(clusterService.findNamesByRdsConfig(source.getId()));
        response.setType(source.getType());
        response.setWorkspace(workspaceResponse);
        return response;
    }
}
