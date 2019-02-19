package com.sequenceiq.cloudbreak.converter.v4.clusterdefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;

@Component
public class ClusterDefinitionToClusterDefinitionV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<ClusterDefinition, ClusterDefinitionV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionToClusterDefinitionV4ResponseConverter.class);

    @Override
    public ClusterDefinitionV4Response convert(ClusterDefinition entity) {
        ClusterDefinitionV4Response blueprintJson = new ClusterDefinitionV4Response();
        blueprintJson.setId(entity.getId());
        blueprintJson.setName(entity.getName());
        blueprintJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        blueprintJson.setHostGroupCount(entity.getHostGroupCount());
        blueprintJson.setStatus(entity.getStatus());
        blueprintJson.setTags(entity.getTags().getMap());
        blueprintJson.setClusterDefinition(entity.getClusterDefinitionText());
        return blueprintJson;
    }

}
