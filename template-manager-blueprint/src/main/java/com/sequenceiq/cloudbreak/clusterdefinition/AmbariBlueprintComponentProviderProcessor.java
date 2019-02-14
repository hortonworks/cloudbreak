package com.sequenceiq.cloudbreak.clusterdefinition;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.ClusterDefinitionComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.ClusterDefinitionConfigurationEntry;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupEntry;

@Component
public class AmbariBlueprintComponentProviderProcessor {

    @Inject
    private List<ClusterDefinitionComponentConfigProvider> clusterDefinitionComponentConfigProviders;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    public String process(TemplatePreparationObject source, String blueprintText) throws IOException {
        AmbariBlueprintTextProcessor blueprintProcessor = ambariBlueprintProcessorFactory.get(blueprintText);
        for (ClusterDefinitionComponentConfigProvider provider : clusterDefinitionComponentConfigProviders) {
            if (blueprintProcessor.isComponentsExistsInBlueprint(provider.components()) || provider.specialCondition(source, blueprintText)) {
                blueprintProcessor.addConfigEntries(provider.getConfigurationEntries(source, blueprintProcessor.asText()), false);
                blueprintProcessor.addSettingsEntries(provider.getSettingsEntries(source, blueprintProcessor.asText()), false);

                Map<HostgroupEntry, List<ClusterDefinitionConfigurationEntry>> hostgroupConfigs = provider.getHostgroupConfigurationEntries(source,
                        blueprintText);
                blueprintProcessor.extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromConfigEntryMap(hostgroupConfigs), false);
                provider.customTextManipulation(source, blueprintProcessor);
            }
        }
        if (!source.getGeneralClusterConfigs().getOrchestratorType().containerOrchestrator() && source.getStackRepoDetailsHdpVersion().isPresent()) {
            blueprintProcessor.modifyHdpVersion(source.getStackRepoDetailsHdpVersion().get());
        }
        return blueprintProcessor.asText();
    }
}
