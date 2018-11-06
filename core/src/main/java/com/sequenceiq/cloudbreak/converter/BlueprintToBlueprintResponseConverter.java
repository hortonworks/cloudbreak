package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.VaultService;

@Component
public class BlueprintToBlueprintResponseConverter extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintResponseConverter.class);

    @Inject
    private VaultService vaultService;

    @Override
    public BlueprintResponse convert(Blueprint entity) {
        BlueprintResponse blueprintJson = new BlueprintResponse();
        blueprintJson.setId(entity.getId());
        blueprintJson.setName(entity.getName());
        blueprintJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        blueprintJson.setHostGroupCount(entity.getHostGroupCount());
        blueprintJson.setStatus(entity.getStatus());
        blueprintJson.setTags(entity.getTags().getMap());
        blueprintJson.setAmbariBlueprint(vaultService.resolveSingleValue(entity.getBlueprintText()));
        return blueprintJson;
    }

}
