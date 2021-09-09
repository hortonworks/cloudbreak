package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;

@Component
public class BlueprintViewToBlueprintV4ViewResponseConverter {

    public BlueprintV4ViewResponse convert(BlueprintView entity) {
        BlueprintV4ViewResponse blueprintV4ViewResponse = new BlueprintV4ViewResponse();
        blueprintV4ViewResponse.setName(entity.getName());
        blueprintV4ViewResponse.setDescription(entity.getDescription());
        blueprintV4ViewResponse.setId(entity.getId());
        blueprintV4ViewResponse.setStackType(entity.getStackType());
        blueprintV4ViewResponse.setStackVersion(entity.getStackVersion());
        blueprintV4ViewResponse.setHostGroupCount(entity.getHostGroupCount());
        blueprintV4ViewResponse.setStatus(entity.getStatus());
        blueprintV4ViewResponse.setTags(entity.getTags().getMap());
        blueprintV4ViewResponse.setCrn(entity.getResourceCrn());
        blueprintV4ViewResponse.setCreated(entity.getCreated());
        return blueprintV4ViewResponse;
    }
}
