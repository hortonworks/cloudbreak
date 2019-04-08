package com.sequenceiq.cloudbreak.service.clusterdefinition;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.ClusterDefinitionTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.ClusterManagerType;

@Component
public class ClusterDefinitionTextProcessorFactory {
    public ClusterDefinitionTextProcessor createClusterDefinitionTextProcessor(String clusterDefinitionText) {
        return ClusterDefinitionTextProcessorUtil.getClusterManagerType(clusterDefinitionText) == ClusterManagerType.CLOUDERA_MANAGER
                ? new CmTemplateProcessor(clusterDefinitionText)
                : new AmbariBlueprintTextProcessor(clusterDefinitionText);
    }
}
