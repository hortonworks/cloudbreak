package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class RDSConfigToRDSConfigResponseConverter extends AbstractConversionServiceAwareConverter<RDSConfig, RDSConfigResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigToRDSConfigResponseConverter.class);

    @Override
    public RDSConfigResponse convert(RDSConfig source) {
        RDSConfigResponse json = new RDSConfigResponse();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setConnectionURL(source.getConnectionURL());
        json.setDatabaseEngine(source.getDatabaseEngine().name());
        json.setConnectionDriver(source.getConnectionDriver());
        json.setDatabaseEngineDisplayName(source.getDatabaseEngine().displayName());
        json.setConnectorJarUrl(source.getConnectorJarUrl());
        json.setCreationDate(source.getCreationDate());
        if (source.getClusters() != null) {
            json.setClusterNames(source.getClusters().stream().map(Cluster::getName).collect(Collectors.toSet()));
        } else {
            json.setClusterNames(new HashSet<>());
        }
        json.setStackVersion(source.getStackVersion());
        json.setType(source.getType());
        json.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        json.setEnvironments(source.getEnvironments().stream()
                .map(CompactView::getName).collect(Collectors.toSet()));
        return json;
    }
}
