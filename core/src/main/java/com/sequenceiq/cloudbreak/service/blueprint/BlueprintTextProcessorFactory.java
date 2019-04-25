package com.sequenceiq.cloudbreak.service.blueprint;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.ClusterManagerType;

@Component
public class BlueprintTextProcessorFactory {
    public BlueprintTextProcessor createBlueprintTextProcessor(String blueprintText) {
        return BlueprintTextProcessorUtil.getClusterManagerType(blueprintText) == ClusterManagerType.CLOUDERA_MANAGER
                ? new CmTemplateProcessor(blueprintText)
                : new AmbariBlueprintTextProcessor(blueprintText);
    }
}
