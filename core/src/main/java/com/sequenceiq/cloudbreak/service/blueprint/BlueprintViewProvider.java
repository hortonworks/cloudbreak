package com.sequenceiq.cloudbreak.service.blueprint;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.utils.StackInfoService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@Component
public class BlueprintViewProvider {

    @Inject
    private StackInfoService stackInfoService;

    @Inject
    private BlueprintTextProcessorFactory blueprintTextProcessorFactory;

    public BlueprintView getBlueprintView(@Nonnull Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintJsonText();
        BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprintText);
        BlueprintTextProcessor processor = blueprintTextProcessorFactory.createBlueprintTextProcessor(blueprintText);
        return new BlueprintView(blueprintText, blueprintStackInfo.getVersion(), blueprintStackInfo.getType(), blueprint.getHybridOption(), processor);
    }

}
