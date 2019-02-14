package com.sequenceiq.cloudbreak.clusterdefinition;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;

@Component
public class AmbariBlueprintProcessorFactory {

    public AmbariBlueprintTextProcessor get(String blueprintText) {
        return new AmbariBlueprintTextProcessor(blueprintText);
    }

}
