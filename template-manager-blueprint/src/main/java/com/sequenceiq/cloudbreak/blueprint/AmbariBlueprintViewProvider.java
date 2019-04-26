package com.sequenceiq.cloudbreak.blueprint;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@Component
public class AmbariBlueprintViewProvider {

    @Inject
    private StackInfoService stackInfoService;

    public BlueprintView getBlueprintView(@Nonnull Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprintText);
        BlueprintTextProcessor processor = new AmbariBlueprintTextProcessor(blueprintText);
        return new BlueprintView(blueprintText, blueprintStackInfo.getVersion(), blueprintStackInfo.getType(), processor);
    }

}
