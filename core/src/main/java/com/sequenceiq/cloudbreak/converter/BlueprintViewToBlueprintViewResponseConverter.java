package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.cloudbreak.domain.view.ClusterDefinitionView;

@Component
public class BlueprintViewToBlueprintViewResponseConverter extends CompactViewToCompactViewResponseConverter<ClusterDefinitionView, BlueprintViewResponse> {
    @Override
    public BlueprintViewResponse convert(ClusterDefinitionView entity) {
        BlueprintViewResponse blueprintJson = super.convert(entity);
        blueprintJson.setStackType(entity.getStackType());
        blueprintJson.setStackVersion(entity.getStackVersion());
        blueprintJson.setHostGroupCount(entity.getHostGroupCount());
        blueprintJson.setStatus(entity.getStatus());
        blueprintJson.setTags(entity.getTags().getMap());
        return blueprintJson;
    }

    @Override
    protected BlueprintViewResponse createTarget() {
        return new BlueprintViewResponse();
    }
}
