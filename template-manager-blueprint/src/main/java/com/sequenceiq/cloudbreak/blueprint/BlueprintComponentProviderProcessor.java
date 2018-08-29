package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupEntry;

@Component
public class BlueprintComponentProviderProcessor {

    @Inject
    private List<BlueprintComponentConfigProvider> blueprintComponentConfigProviders;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    public String process(TemplatePreparationObject source, String blueprintText) throws IOException {
        BlueprintTextProcessor blueprintProcessor = blueprintProcessorFactory.get(blueprintText);
        for (BlueprintComponentConfigProvider provider : blueprintComponentConfigProviders) {
            if (blueprintProcessor.componentsExistsInBlueprint(provider.components()) || provider.specialCondition(source, blueprintText)) {
                blueprintProcessor.addConfigEntries(provider.getConfigurationEntries(source, blueprintProcessor.asText()), false);
                blueprintProcessor.addSettingsEntries(provider.getSettingsEntries(source, blueprintProcessor.asText()), false);

                Map<HostgroupEntry, List<BlueprintConfigurationEntry>> hostgroupConfigs = provider.getHostgroupConfigurationEntries(source, blueprintText);
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
