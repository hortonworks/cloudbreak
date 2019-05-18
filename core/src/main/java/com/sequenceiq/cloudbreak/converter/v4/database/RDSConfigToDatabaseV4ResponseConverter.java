package com.sequenceiq.cloudbreak.converter.v4.database;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.CompactView;
import com.sequenceiq.secret.model.SecretResponse;

@Component
public class RDSConfigToDatabaseV4ResponseConverter extends AbstractConversionServiceAwareConverter<RDSConfig, DatabaseV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigToDatabaseV4ResponseConverter.class);

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
        json.setConnectorJarUrl(source.getConnectorJarUrl());
        json.setCreationDate(source.getCreationDate());
        if (source.getClusters() != null) {
            json.setClusterNames(source.getClusters().stream().map(Cluster::getName).collect(Collectors.toSet()));
        } else {
            json.setClusterNames(new HashSet<>());
        }
        json.setType(source.getType());
        json.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        json.setEnvironments(source.getEnvironments().stream()
                .map(CompactView::getName).collect(Collectors.toSet()));
        return json;
    }
}
