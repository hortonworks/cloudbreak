package com.sequenceiq.cloudbreak.blueprint;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.blueprint.templates.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintViewProvider {

    @Inject
    private StackInfoService stackInfoService;

    public BlueprintView getBlueprintView(@Nonnull Blueprint blueprint) {
        BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprint.getBlueprintText());
        return new BlueprintView(blueprint.getBlueprintText(), blueprintStackInfo.getVersion(), blueprintStackInfo.getType());
    }

}
