package com.sequenceiq.cloudbreak.blueprint;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@Component
public class BlueprintProcessorFactory {

    public BlueprintTextProcessor get(String blueprintText) {
        return new BlueprintTextProcessor(blueprintText);
    }

}
