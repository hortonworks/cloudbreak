package com.sequenceiq.cloudbreak.service.blueprint;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@Component
public class BlueprintTextProcessorFactory {
    public BlueprintTextProcessor createBlueprintTextProcessor(String blueprintText) {
        return new CmTemplateProcessor(blueprintText);
    }
}
