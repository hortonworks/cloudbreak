package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintToBlueprintV4ViewResponseConverter {

    public BlueprintV4ViewResponse convert(Blueprint source) {
        BlueprintV4ViewResponse blueprintV4ViewResponse = new BlueprintV4ViewResponse();
        blueprintV4ViewResponse.setName(source.getName());
        blueprintV4ViewResponse.setDescription(source.getDescription());
        blueprintV4ViewResponse.setCrn(source.getResourceCrn());
        blueprintV4ViewResponse.setId(source.getId());
        blueprintV4ViewResponse.setStackType(source.getStackType());
        blueprintV4ViewResponse.setStackVersion(source.getStackVersion());
        blueprintV4ViewResponse.setHostGroupCount(source.getHostGroupCount());
        blueprintV4ViewResponse.setStatus(source.getStatus());
        blueprintV4ViewResponse.setTags(source.getTags().getMap());
        blueprintV4ViewResponse.setCreated(source.getCreated());
        return blueprintV4ViewResponse;
    }
}
