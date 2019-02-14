package com.sequenceiq.cloudbreak.converter.v4.blueprints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;

@Component
public class BlueprintToBlueprintV4ResponseConverter extends AbstractConversionServiceAwareConverter<ClusterDefinition, BlueprintV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintV4ResponseConverter.class);

    @Override
    public BlueprintV4Response convert(ClusterDefinition entity) {
        BlueprintV4Response blueprintJson = new BlueprintV4Response();
        blueprintJson.setId(entity.getId());
        blueprintJson.setName(entity.getName());
        blueprintJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        blueprintJson.setHostGroupCount(entity.getHostGroupCount());
        blueprintJson.setStatus(entity.getStatus());
        blueprintJson.setTags(entity.getTags().getMap());
        blueprintJson.setAmbariBlueprint(entity.getClusterDefinitionText());
        return blueprintJson;
    }

}
