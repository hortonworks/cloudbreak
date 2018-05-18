package com.sequenceiq.cloudbreak.blueprint.knox;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.template.views.HostgroupView;

@Component
public class KnoxConfigProvider implements BlueprintComponentConfigProvider {

    private static final String KNOX_GATEWAY = "KNOX_GATEWAY";

    @Override
    public BlueprintTextProcessor customTextManipulation(BlueprintPreparationObject source, BlueprintTextProcessor blueprintProcessor) {
        Set<String> hostGroupNames = source.getHostgroupViews().stream().map(getHostGroupNameMapper()).filter("master"::equals).collect(Collectors.toSet());
        return blueprintProcessor.addComponentToHostgroups(KNOX_GATEWAY, hostGroupNames::contains);
    }

    @Override
    public boolean specialCondition(BlueprintPreparationObject source, String blueprintText) {
        return source.getGeneralClusterConfigs().isGatewayInstanceMetadataPresented();
    }

    private Function<HostgroupView, String> getHostGroupNameMapper() {
        return HostgroupView::getName;
    }
}
