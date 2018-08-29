package com.sequenceiq.cloudbreak.blueprint.hadoop;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.ConfigService;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Service
public class HadoopConfigurationService implements BlueprintComponentConfigProvider {

    @Inject
    private ConfigService configService;

    @Override
    public BlueprintTextProcessor customTextManipulation(TemplatePreparationObject source, BlueprintTextProcessor blueprintProcessor) {
        Map<String, Map<String, Map<String, String>>> hostGroupConfig =
                configService.getHostGroupConfiguration(blueprintProcessor, source.getHostgroupViews());
        HostgroupConfigurations hostgroupConfigurations = HostgroupConfigurations.fromMap(hostGroupConfig);
        blueprintProcessor.extendBlueprintHostGroupConfiguration(hostgroupConfigurations, false);

        Map<String, Map<String, String>> globalConfig = getGlobalConfiguration(blueprintProcessor, source.getHostgroupViews());
        SiteConfigurations siteConfigurations = SiteConfigurations.fromMap(globalConfig);
        blueprintProcessor.extendBlueprintGlobalConfiguration(siteConfigurations, false);

        return blueprintProcessor;
    }

    @Override
    public boolean specialCondition(TemplatePreparationObject source, String blueprintProcessor) {
        return !source.getBlueprintView().isHdf();
    }

    private Map<String, Map<String, String>> getGlobalConfiguration(BlueprintTextProcessor blueprintProcessor, Collection<HostgroupView> hostGroups) {
        Map<String, Map<String, String>> config = configService.getComponentsByHostGroup(blueprintProcessor, hostGroups);
        configService.collectBlueprintConfigIfNeed(config);
        return config;
    }
}
