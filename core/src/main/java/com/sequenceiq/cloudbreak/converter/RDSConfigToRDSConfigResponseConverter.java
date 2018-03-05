package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class RDSConfigToRDSConfigResponseConverter extends AbstractConversionServiceAwareConverter<RDSConfig, RDSConfigResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigToRDSConfigResponseConverter.class);

    @Override
    public RDSConfigResponse convert(RDSConfig source) {
        RDSConfigResponse json = new RDSConfigResponse();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setConnectionURL(source.getConnectionURL());
        json.setDatabaseEngine(source.getDatabaseEngine());
        json.setConnectionDriver(source.getConnectionDriver());
        json.setPublicInAccount(source.isPublicInAccount());
        json.setCreationDate(source.getCreationDate());
        if (source.getClusters() != null) {
            json.setClusterNames(source.getClusters().stream().map(Cluster::getName).collect(Collectors.toSet()));
        } else {
            json.setClusterNames(new HashSet<>());
        }
        json.setStackVersion(source.getStackVersion());
        json.setType(source.getType());
        return json;
    }
}
