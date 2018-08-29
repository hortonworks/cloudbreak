package com.sequenceiq.cloudbreak.blueprint.knox;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class KnoxConfigProvider implements BlueprintComponentConfigProvider {

    private static final String KNOX_GATEWAY = "KNOX_GATEWAY";

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
