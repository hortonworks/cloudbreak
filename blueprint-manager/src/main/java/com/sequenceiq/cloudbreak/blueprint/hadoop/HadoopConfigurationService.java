package com.sequenceiq.cloudbreak.blueprint.hadoop;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.ConfigService;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateTextProcessor;
import com.sequenceiq.cloudbreak.templateprocessor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.templateprocessor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.templateprocessor.template.views.HostgroupView;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

@Service
public class HadoopConfigurationService implements BlueprintComponentConfigProvider {

    @Inject
    private ConfigService configService;

    @Override
    public TemplateTextProcessor customTextManipulation(PreparationObject source, TemplateTextProcessor blueprintProcessor) {
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
    public boolean additionalCriteria(PreparationObject source, String blueprintProcessor) {
        return !source.getBlueprintView().isHdf();
    }

    private Map<String, Map<String, String>> getGlobalConfiguration(TemplateTextProcessor blueprintProcessor, Collection<HostgroupView> hostGroups) {
        Map<String, Map<String, String>> config = configService.getComponentsByHostGroup(blueprintProcessor, hostGroups);
        configService.collectBlueprintConfigIfNeed(config);
        return config;
    }
}
