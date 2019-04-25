package com.sequenceiq.cloudbreak.blueprint;

import org.springframework.stereotype.Component;

@Component
public class AmbariBlueprintProcessorFactory {

    public AmbariBlueprintTextProcessor get(String blueprintText) {
        return new AmbariBlueprintTextProcessor(blueprintText);
    }
}
