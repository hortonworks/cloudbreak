package com.sequenceiq.cloudbreak.blueprint.filesystem;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.template.HandlebarTemplate;
import com.sequenceiq.cloudbreak.template.HandlebarUtils;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntries;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class FileSystemConfigQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemConfigQueryService.class);

    private final Handlebars handlebars = HandlebarUtils.handlebars();

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    private ConfigQueryEntries configQueryEntries;

    @PostConstruct
    public void init() {
        String configDefinitions = cloudbreakResourceReaderService.resourceDefinition("cloud-storage-location-specification");
        try {
            configQueryEntries = JsonUtil.readValue(configDefinitions, ConfigQueryEntries.class);
        } catch (IOException e) {
            LOGGER.warn("Cannot initialize configQueryEntries", e);
            configQueryEntries = new ConfigQueryEntries();
        }
    }

    public Set<ConfigQueryEntry> queryParameters(FileSystemConfigQueryObject request) {
        Set<ConfigQueryEntry> filtered = new HashSet<>();

        BlueprintTextProcessor blueprintTextProcessor = blueprintProcessorFactory.get(request.getBlueprintText());
        Map<String, Set<String>> componentsByHostGroup = blueprintTextProcessor.getComponentsByHostGroup();
        for (Map.Entry<String, Set<String>> serviceHostgroupEntry : componentsByHostGroup.entrySet()) {
            for (String service : serviceHostgroupEntry.getValue()) {
                Set<ConfigQueryEntry> collectedEntries = configQueryEntries.getEntries()
                        .stream()
                        .filter(configQueryEntry -> configQueryEntry.getRelatedService().equalsIgnoreCase(service))
                        .filter(configQueryEntry -> configQueryEntry.getSupportedStorages().contains(request.getFileSystemType().toUpperCase()))
                        .map(configQueryEntry -> configQueryEntry.copy())
                        .collect(Collectors.toSet());
                filtered.addAll(collectedEntries);
            }
        }
        List<ConfigQueryEntry> collectedEntries = configQueryEntries.getEntries()
                .stream()
                .filter(configQueryEntry -> configQueryEntry.isRequiredForAttachedCluster() && request.isAttachedCluster())
                .collect(Collectors.toList());
        filtered.addAll(collectedEntries);
        String fileSystemTypeRequest = request.getFileSystemType();
        FileSystemType fileSystemType = FileSystemType.valueOf(fileSystemTypeRequest);
        Map<String, String> templateObject = getTemplateObject(request, fileSystemType.getProtocol());
        for (ConfigQueryEntry configQueryEntry : filtered) {
            try {
                configQueryEntry.setProtocol(fileSystemType.getProtocol());
                configQueryEntry.setDefaultPath(generateConfigWithParameters(configQueryEntry.getDefaultPath(), fileSystemType, templateObject));
            } catch (IOException e) {
                configQueryEntry.setDefaultPath(configQueryEntry.getDefaultPath());
            }
        }
        filtered = filtered.stream().sorted(Comparator.comparing(ConfigQueryEntry::getPropertyName)).collect(Collectors.toSet());
        return filtered;
    }

    private String generateConfigWithParameters(String sourceTemplate, FileSystemType fileSystemType, Map<String, String> templateObject) throws IOException {
        String defaultPath = fileSystemType.getDefaultPath();
        Template defaultPathtemplate = handlebars.compileInline(defaultPath, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        templateObject.put("defaultPath", defaultPathtemplate.apply(templateObject));
        Template template = handlebars.compileInline(sourceTemplate, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        return template.apply(templateObject);
    }

    private Map<String, String> getTemplateObject(FileSystemConfigQueryObject fileSystemConfigQueryObject, String protocol) {
        Map<String, String> templateObject = new HashMap<>();
        templateObject.put("clusterName", fileSystemConfigQueryObject.getClusterName());
        templateObject.put("storageName", fileSystemConfigQueryObject.getStorageName());
        templateObject.put("blueprintText", fileSystemConfigQueryObject.getBlueprintText());
        templateObject.put("accountName", fileSystemConfigQueryObject.getAccountName().orElse("default-account-name"));
        templateObject.put("protocol", protocol);
        return templateObject;
    }

}
