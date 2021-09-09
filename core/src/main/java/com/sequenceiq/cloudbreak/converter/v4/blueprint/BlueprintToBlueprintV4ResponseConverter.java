package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintToBlueprintV4ResponseConverter {

    public BlueprintV4Response convert(Blueprint entity) {
        BlueprintV4Response blueprintJson = new BlueprintV4Response();
        blueprintJson.setName(entity.getName());
        blueprintJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        blueprintJson.setHostGroupCount(entity.getHostGroupCount());
        blueprintJson.setStatus(entity.getStatus());
        blueprintJson.setTags(entity.getTags().getMap());
        blueprintJson.setBlueprint(entity.getBlueprintText());
        blueprintJson.setCrn(entity.getResourceCrn());
        blueprintJson.setCreated(entity.getCreated());
        return blueprintJson;
    }

}
