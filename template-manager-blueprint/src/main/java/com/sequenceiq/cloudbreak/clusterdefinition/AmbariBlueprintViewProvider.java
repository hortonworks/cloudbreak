package com.sequenceiq.cloudbreak.clusterdefinition;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterdefinition.utils.StackInfoService;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.template.model.ClusterDefinitionStackInfo;
import com.sequenceiq.cloudbreak.template.views.ClusterDefinitionView;

@Component
public class AmbariBlueprintViewProvider {

    @Inject
    private StackInfoService stackInfoService;

    public ClusterDefinitionView getBlueprintView(@Nonnull ClusterDefinition clusterDefinition) {
        String clusterDefinitionText = clusterDefinition.getClusterDefinitionText();
        ClusterDefinitionStackInfo clusterDefinitionStackInfo = stackInfoService.clusterDefinitionStackInfo(clusterDefinitionText);
        return new ClusterDefinitionView(clusterDefinitionText, clusterDefinitionStackInfo.getVersion(), clusterDefinitionStackInfo.getType());
    }

}
