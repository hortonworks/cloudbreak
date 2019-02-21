package com.sequenceiq.cloudbreak.service.clusterdefinition;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.processor.ClusterManagerType;

public class ClusterDefinitionTextProcessorUtil {
    private ClusterDefinitionTextProcessorUtil() {
    }

    public static ClusterManagerType getClusterManagerType(String clusterDefinitionText) {
        try {
            new CmTemplateProcessor(clusterDefinitionText);
            return ClusterManagerType.CLOUDERA_MANAGER;
        } catch (ClusterDefinitionProcessingException ex) {
            return ClusterManagerType.AMBARI;
        }
    }
}
