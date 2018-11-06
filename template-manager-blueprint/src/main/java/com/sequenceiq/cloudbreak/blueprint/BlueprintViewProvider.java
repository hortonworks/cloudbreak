package com.sequenceiq.cloudbreak.blueprint;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@Component
public class BlueprintViewProvider {

    @Inject
    private StackInfoService stackInfoService;

    @Inject
    private VaultService vaultService;

    public BlueprintView getBlueprintView(@Nonnull Blueprint blueprint) {
        String blueprintText = vaultService.resolveSingleValue(blueprint.getBlueprintText());
        BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprintText);
        return new BlueprintView(blueprintText, blueprintStackInfo.getVersion(), blueprintStackInfo.getType());
    }

}
