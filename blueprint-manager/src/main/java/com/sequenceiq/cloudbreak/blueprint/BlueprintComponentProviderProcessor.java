package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurator;

@Component
public class BlueprintComponentProviderProcessor {

    @Resource
    private Map<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>> fileSystemConfigurators;

    @Inject
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Inject
    private List<BlueprintComponentConfigProvider> blueprintComponentConfigProviders;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    public String process(BlueprintPreparationObject source, String blueprintText) throws IOException {
        BlueprintTextProcessor blueprintProcessor = blueprintProcessorFactory.get(blueprintText);
        for (BlueprintComponentConfigProvider provider : blueprintComponentConfigProviders) {
            if (blueprintProcessor.componentsExistsInBlueprint(provider.components()) || provider.additionalCriteria(source, blueprintText)) {
                blueprintProcessor.addConfigEntries(provider.getConfigurationEntries(source, blueprintProcessor.asText()), false);
                blueprintProcessor.addSettingsEntries(provider.getSettingsEntries(source, blueprintProcessor.asText()), false);

                Map<HostgroupEntry, List<BlueprintConfigurationEntry>> hostgroupConfigs = provider.getHostgroupConfigurationEntries(source, blueprintText);
                blueprintProcessor.extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromConfigEntryMap(hostgroupConfigs), false);
                provider.customTextManipulation(source, blueprintProcessor);
            }
        }
        if (source.getFileSystemConfigurationView().isPresent()) {
            extendBlueprintWithFsConfig(blueprintProcessor,
                    source.getFileSystemConfigurationView().get().getFileSystemConfiguration(),
                    source.getFileSystemConfigurationView().get().isDefaultFs());
        }
        if (!source.getGeneralClusterConfigs().getOrchestratorType().containerOrchestrator() && source.getStackRepoDetailsHdpVersion().isPresent()) {
            blueprintProcessor.modifyHdpVersion(source.getStackRepoDetailsHdpVersion().get());
        }
        return blueprintProcessor.asText();
    }

    private String extendBlueprintWithFsConfig(BlueprintTextProcessor blueprintProcessor, FileSystemConfiguration fsConfiguration, boolean defaultFs) {
        FileSystemType fileSystemType = FileSystemType.fromClass(fsConfiguration.getClass());
        FileSystemConfigurator<FileSystemConfiguration> fsConfigurator = fileSystemConfigurators.get(fileSystemType);
        Map<String, String> resourceProperties = fsConfigurator.createResources(fsConfiguration);
        List<BlueprintConfigurationEntry> bpConfigEntries = fsConfigurator.getFsProperties(fsConfiguration, resourceProperties);
        if (defaultFs) {
            bpConfigEntries.addAll(fsConfigurator.getDefaultFsProperties(fsConfiguration));
        }
        return blueprintProcessor.addConfigEntries(bpConfigEntries, true).asText();
    }
}
