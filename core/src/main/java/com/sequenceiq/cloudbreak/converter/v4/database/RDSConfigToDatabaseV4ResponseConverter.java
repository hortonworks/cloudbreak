package com.sequenceiq.cloudbreak.converter.v4.database;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

@Component
public class RDSConfigToDatabaseV4ResponseConverter extends AbstractConversionServiceAwareConverter<RDSConfig, DatabaseV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigToDatabaseV4ResponseConverter.class);

    @Inject
    private ClusterService clusterService;

    @Override
    public DatabaseV4Response convert(RDSConfig source) {
        DatabaseV4Response json = new DatabaseV4Response();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setConnectionURL(source.getConnectionURL());
        json.setDatabaseEngine(source.getDatabaseEngine().name());
        json.setConnectionDriver(source.getConnectionDriver());
        json.setConnectionUserName(getConversionService().convert(source.getConnectionUserNameSecret(), SecretResponse.class));
        json.setConnectionPassword(getConversionService().convert(source.getConnectionPasswordSecret(), SecretResponse.class));
        json.setDatabaseEngineDisplayName(source.getDatabaseEngine().displayName());
        json.setCreationDate(source.getCreationDate());
        json.setClusterNames(clusterService.findNamesByRdsConfig(source.getId()));
        json.setType(source.getType());
        json.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        return json;
    }
}
