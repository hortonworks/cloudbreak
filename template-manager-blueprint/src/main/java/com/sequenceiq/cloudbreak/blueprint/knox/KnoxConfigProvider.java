package com.sequenceiq.cloudbreak.blueprint.knox;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.template.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class KnoxConfigProvider implements BlueprintComponentConfigProvider {

    private static final String KNOX_GATEWAY = "KNOX_GATEWAY";

    // TODO-MASTER-REPAIR knox should be enabled
    @Override
    public BlueprintTextProcessor customTextManipulation(TemplatePreparationObject source, BlueprintTextProcessor blueprintProcessor) {
        Set<String> hostGroupNames = source.getHostgroupViews()
                .stream()
                .filter(hostgroupView -> InstanceGroupType.isGateway(hostgroupView.getInstanceGroupType()))
                .map(HostgroupView::getName)
                .collect(Collectors.toSet());
        return blueprintProcessor.addComponentToHostgroups(KNOX_GATEWAY, hostGroupNames::contains);
    }

    @Override
    public boolean specialCondition(TemplatePreparationObject source, String blueprintText) {
        return source.getGeneralClusterConfigs().isGatewayInstanceMetadataPresented();
    }
}
