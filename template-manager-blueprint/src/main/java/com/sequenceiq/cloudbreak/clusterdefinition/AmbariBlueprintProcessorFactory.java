package com.sequenceiq.cloudbreak.clusterdefinition;

import org.springframework.stereotype.Component;

@Component
public class AmbariBlueprintProcessorFactory {

    public AmbariBlueprintTextProcessor get(String blueprintText) {
        return new AmbariBlueprintTextProcessor(blueprintText);
    }
}
