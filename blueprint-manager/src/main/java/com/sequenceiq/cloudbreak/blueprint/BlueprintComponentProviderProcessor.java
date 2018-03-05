package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AzureFileSystemConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintComponentProviderProcessor {

    @Inject
    private AzureFileSystemConfigProvider azureFileSystemConfigProvider;

    @Resource
    private Map<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>> fileSystemConfigurators;

    @Inject
    private List<BlueprintComponentConfigProvider> blueprintComponentConfigProviders;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    public String process(BlueprintPreparationObject source, String blueprint) throws IOException {
        for (BlueprintComponentConfigProvider provider : blueprintComponentConfigProviders) {
            if (blueprintProcessor.componentsExistsInBlueprint(provider.components(), blueprint) || provider.additionalCriteria(source, blueprint)) {
                blueprint = blueprintProcessor.addConfigEntries(blueprint, provider.getConfigurationEntries(source, blueprint), false);
                blueprint = blueprintProcessor.addSettingsEntries(blueprint, provider.getSettingsEntries(source, blueprint), false);

                Map<HostgroupEntry, List<BlueprintConfigurationEntry>> hostgroupConfigs = provider.getHostgroupConfigurationEntries(source, blueprint);
                blueprint = provider.customTextManipulation(source, blueprint);
            }
        }
        if (source.getCluster().getFileSystem() != null) {
            blueprint = extendBlueprintWithFsConfig(blueprint, source.getCluster().getFileSystem(), source.getStack());
        }
        if (!source.getOrchestratorType().containerOrchestrator() && source.getStackRepoDetailsHdpVersion().isPresent()) {
            blueprint = blueprintProcessor.modifyHdpVersion(blueprint, source.getStackRepoDetailsHdpVersion().get());
        }
        return blueprint;
    }

    private String extendBlueprintWithFsConfig(String blueprintText, FileSystem fs, Stack stack) throws IOException {
        FileSystemType fileSystemType = FileSystemType.valueOf(fs.getType());
        FileSystemConfigurator<FileSystemConfiguration> fsConfigurator = fileSystemConfigurators.get(fileSystemType);
        String json = JsonUtil.writeValueAsString(fs.getProperties());
        FileSystemConfiguration fsConfiguration = JsonUtil.readValue(json, fileSystemType.getClazz());
        fsConfiguration = decorateFsConfigurationProperties(fsConfiguration, stack);
        Map<String, String> resourceProperties = fsConfigurator.createResources(fsConfiguration);
        List<BlueprintConfigurationEntry> bpConfigEntries = fsConfigurator.getFsProperties(fsConfiguration, resourceProperties);
        if (fs.isDefaultFs()) {
            bpConfigEntries.addAll(fsConfigurator.getDefaultFsProperties(fsConfiguration));
        }
        return blueprintProcessor.addConfigEntries(blueprintText, bpConfigEntries, true);
    }

    private FileSystemConfiguration decorateFsConfigurationProperties(FileSystemConfiguration fsConfiguration, Stack stack) {
        fsConfiguration.addProperty(FileSystemConfiguration.STORAGE_CONTAINER, "cloudbreak" + stack.getId());

        if (CloudConstants.AZURE.equals(stack.getPlatformVariant())) {
            fsConfiguration = azureFileSystemConfigProvider.decorateFileSystemConfiguration(stack, fsConfiguration);
        }
        return fsConfiguration;
    }
}
