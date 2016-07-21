package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RDSConfigResponse;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class RDSConfigToJsonConverter extends AbstractConversionServiceAwareConverter<RDSConfig, RDSConfigResponse> {
    @Override
    public RDSConfigResponse convert(RDSConfig source) {
        RDSConfigResponse json = new RDSConfigResponse();
        json.setName(source.getName());
        json.setConnectionURL(source.getConnectionURL());
        json.setConnectionUserName(source.getConnectionUserName());
        json.setConnectionPassword(source.getConnectionPassword());
        json.setDatabaseType(source.getDatabaseType());
        json.setId(source.getId().toString());
        json.setPublicInAccount(source.isPublicInAccount());
        json.setCreationDate(source.getCreationDate());
        json.setClusterNames(source.getClusters().stream().map(cluster -> cluster.getName()).collect(Collectors.toSet()));
        json.setHdpVersion(source.getHdpVersion());
        return json;
    }
}
