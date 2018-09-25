package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;

@Component
public class BlueprintViewToBlueprintViewResponseConverter extends AbstractConversionServiceAwareConverter<BlueprintView, BlueprintViewResponse> {
    @Override
    public BlueprintViewResponse convert(BlueprintView entity) {
        BlueprintViewResponse blueprintJson = new BlueprintViewResponse();
        blueprintJson.setId(entity.getId());
        blueprintJson.setName(entity.getName());
        blueprintJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        blueprintJson.setStackType(entity.getStackType());
        blueprintJson.setStackVersion(entity.getStackVersion());
        blueprintJson.setHostGroupCount(entity.getHostGroupCount());
        blueprintJson.setStatus(entity.getStatus());
        blueprintJson.setTags(entity.getTags().getMap());
        return blueprintJson;
    }
}
