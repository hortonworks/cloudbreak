package com.sequenceiq.cloudbreak.converter.v4.database;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
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
        DatabaseV4Response json = new DatabaseV4Response();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setConnectionURL(source.getConnectionURL());
        json.setDatabaseEngine(source.getDatabaseEngine().name());
        json.setConnectionDriver(source.getConnectionDriver());
        json.setConnectionUserName(stringToSecretResponseConverter.convert(source.getConnectionUserNameSecret()));
        json.setConnectionPassword(stringToSecretResponseConverter.convert(source.getConnectionPasswordSecret()));
        json.setDatabaseEngineDisplayName(source.getDatabaseEngine().displayName());
        json.setCreationDate(source.getCreationDate());
        json.setClusterNames(clusterService.findNamesByRdsConfig(source.getId()));
        json.setType(source.getType());
        json.setWorkspace(workspaceToWorkspaceResourceV4ResponseConverter.convert(source.getWorkspace()));
        return json;
    }
}
