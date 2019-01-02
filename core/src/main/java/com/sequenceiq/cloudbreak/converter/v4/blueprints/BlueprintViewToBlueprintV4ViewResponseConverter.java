package com.sequenceiq.cloudbreak.converter.v4.blueprints;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.converter.CompactViewToCompactViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;

@Component
public class BlueprintViewToBlueprintV4ViewResponseConverter extends CompactViewToCompactViewResponseConverter<BlueprintView, BlueprintV4ViewResponse> {
    @Override
    public BlueprintV4ViewResponse convert(BlueprintView entity) {
        BlueprintV4ViewResponse blueprintJson = super.convert(entity);
        blueprintJson.setStackType(entity.getStackType());
        blueprintJson.setStackVersion(entity.getStackVersion());
        blueprintJson.setHostGroupCount(entity.getHostGroupCount());
        blueprintJson.setStatus(entity.getStatus());
        blueprintJson.setTags(entity.getTags().getMap());
        return blueprintJson;
    }

    @Override
    protected BlueprintV4ViewResponse createTarget() {
        return new BlueprintV4ViewResponse();
    }
}
