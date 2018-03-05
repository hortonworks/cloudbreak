package com.sequenceiq.cloudbreak.blueprint;

import org.springframework.stereotype.Component;

@Component
public class BlueprintProcessorFactory {

    public BlueprintTextProcessor get(String blueprintText) {
        return new BlueprintTextProcessor(blueprintText);
    }

}
