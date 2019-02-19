package com.sequenceiq.cloudbreak.clusterdefinition.hadoop;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.template.ClusterDefinitionComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.clusterdefinition.ConfigService;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Service
public class HadoopConfigurationService implements ClusterDefinitionComponentConfigProvider {

    @Inject
    private ConfigService configService;

    @Override
    public AmbariBlueprintTextProcessor customTextManipulation(TemplatePreparationObject source, AmbariBlueprintTextProcessor blueprintProcessor) {
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
    public boolean specialCondition(TemplatePreparationObject source, String clusterDefinitionText) {
        return !source.getClusterDefinitionView().isHdf();
    }

    private Map<String, Map<String, String>> getGlobalConfiguration(AmbariBlueprintTextProcessor blueprintProcessor, Collection<HostgroupView> hostGroups) {
        Map<String, Map<String, String>> config = configService.getComponentsByHostGroup(blueprintProcessor, hostGroups);
        configService.collectBlueprintConfigIfNeed(config);
        return config;
    }
}
