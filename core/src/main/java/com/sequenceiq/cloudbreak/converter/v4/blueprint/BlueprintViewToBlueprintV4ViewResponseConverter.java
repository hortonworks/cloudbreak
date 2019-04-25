package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.converter.CompactViewToCompactViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;

@Component
public class BlueprintViewToBlueprintV4ViewResponseConverter
        extends CompactViewToCompactViewResponseConverter<BlueprintView, BlueprintV4ViewResponse> {
    @Override
    public BlueprintV4ViewResponse convert(BlueprintView entity) {
        BlueprintV4ViewResponse blueprintV4ViewResponse = super.convert(entity);
        blueprintV4ViewResponse.setStackType(entity.getStackType());
        blueprintV4ViewResponse.setStackVersion(entity.getStackVersion());
        blueprintV4ViewResponse.setHostGroupCount(entity.getHostGroupCount());
        blueprintV4ViewResponse.setStatus(entity.getStatus());
        blueprintV4ViewResponse.setTags(entity.getTags().getMap());
        return blueprintV4ViewResponse;
    }

    @Override
    protected BlueprintV4ViewResponse createTarget() {
        return new BlueprintV4ViewResponse();
    }
}
