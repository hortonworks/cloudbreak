package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AzureFileSystemConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.blueprint.template.BlueprintTemplateProcessor;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Component
public class CentralBlueprintUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralBlueprintUpdater.class);

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Inject
    private BlueprintTemplateProcessor blueprintTemplateProcessor;

    @Inject
    private AzureFileSystemConfigProvider azureFileSystemConfigProvider;

    @Resource
    private Map<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>> fileSystemConfigurators;

    @Inject
    private List<BlueprintComponentConfigProvider> blueprintComponentConfigProviders;

    public String getBlueprintText(BlueprintPreparationObject source) throws CloudbreakServiceException, HttpResponseException {
        String blueprintText = source.getCluster().getBlueprint().getBlueprintText();
        try {
            blueprintText = updateBlueprintConfiguration(source, blueprintText);
        } catch (IOException e) {
            throw new CloudbreakServiceException(e);
        }
        return blueprintText;
    }

    private String updateBlueprintConfiguration(BlueprintPreparationObject source, String blueprintText)
            throws IOException {
        blueprintText = blueprintTemplateProcessor.process(blueprintText, source.getCluster(), source.getRdsConfigs(), source.getAmbariDatabase());

        for (BlueprintComponentConfigProvider provider : blueprintComponentConfigProviders) {
            if (blueprintProcessor.componentsExistsInBlueprint(provider.components(), blueprintText) || provider.additionalCriteria(source, blueprintText)) {
                blueprintText = provider.configure(source, blueprintText);
            }
        }

        if (source.getCluster().getFileSystem() != null) {
            blueprintText = extendBlueprintWithFsConfig(blueprintText, source.getCluster().getFileSystem(), source.getStack());
        }
        if (!source.getOrchestratorType().containerOrchestrator()) {
            if (source.getStackRepoDetails() != null && source.getStackRepoDetails().getHdpVersion() != null) {
                blueprintText = blueprintProcessor.modifyHdpVersion(blueprintText, source.getStackRepoDetails().getHdpVersion());
            }
        }

        return blueprintText;
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
