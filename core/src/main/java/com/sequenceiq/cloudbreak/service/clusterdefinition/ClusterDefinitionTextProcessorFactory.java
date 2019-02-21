package com.sequenceiq.cloudbreak.service.clusterdefinition;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.ClusterDefinitionTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.ClusterManagerType;

@Component
public class ClusterDefinitionTextProcessorFactory {
    public ClusterDefinitionTextProcessor createClusterDefinitionTextProcessor(String clusterDefinitionText) {
        if (ClusterDefinitionTextProcessorUtil.getClusterManagerType(clusterDefinitionText) == ClusterManagerType.CLOUDERA_MANAGER) {
            return new CmTemplateProcessor(clusterDefinitionText);
        } else {
            return new AmbariBlueprintTextProcessor(clusterDefinitionText);
        }
    }
}
