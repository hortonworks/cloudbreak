package com.sequenceiq.cloudbreak.blueprint;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.templateprocessor.HostgroupEntry;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateConfigurationEntry;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessorFactory;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateTextProcessor;
import com.sequenceiq.cloudbreak.templateprocessor.configuration.HostgroupConfigurations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class BlueprintComponentProviderProcessor {

    @Resource
    private Map<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>> fileSystemConfigurators;

    @Inject
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Inject
    private List<BlueprintComponentConfigProvider> blueprintComponentConfigProviders;

    @Inject
    private TemplateProcessorFactory blueprintProcessorFactory;

    public String process(PreparationObject source, String blueprintText) throws IOException {
        TemplateTextProcessor blueprintProcessor = blueprintProcessorFactory.get(blueprintText);
        for (BlueprintComponentConfigProvider provider : blueprintComponentConfigProviders) {
            if (blueprintProcessor.componentsExistsInBlueprint(provider.components()) || provider.additionalCriteria(source, blueprintText)) {
                blueprintProcessor.addConfigEntries(provider.getConfigurationEntries(source, blueprintProcessor.asText()), false);
                blueprintProcessor.addSettingsEntries(provider.getSettingsEntries(source, blueprintProcessor.asText()), false);

                Map<HostgroupEntry, List<TemplateConfigurationEntry>> hostgroupConfigs = provider.getHostgroupConfigurationEntries(source, blueprintText);
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

    private String extendBlueprintWithFsConfig(TemplateTextProcessor blueprintProcessor, FileSystemConfiguration fsConfiguration, boolean defaultFs) {
        FileSystemType fileSystemType = FileSystemType.fromClass(fsConfiguration.getClass());
        FileSystemConfigurator<FileSystemConfiguration> fsConfigurator = fileSystemConfigurators.get(fileSystemType);
        Map<String, String> resourceProperties = fsConfigurator.createResources(fsConfiguration);
        List<TemplateConfigurationEntry> bpConfigEntries = fsConfigurator.getFsProperties(fsConfiguration, resourceProperties);
        if (defaultFs) {
            bpConfigEntries.addAll(fsConfigurator.getDefaultFsProperties(fsConfiguration));
        }
        return blueprintProcessor.addConfigEntries(bpConfigEntries, true).asText();
    }
}
