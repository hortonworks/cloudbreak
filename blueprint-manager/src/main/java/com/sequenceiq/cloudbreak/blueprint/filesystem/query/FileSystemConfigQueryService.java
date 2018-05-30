package com.sequenceiq.cloudbreak.blueprint.filesystem.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.HandlebarTemplate;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.blueprint.template.HandlebarUtils;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
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

    public List<ConfigQueryEntry> queryParameters(FileSystemConfigQueryObject request) {
        List<ConfigQueryEntry> filtered = new ArrayList<>();

        BlueprintTextProcessor blueprintTextProcessor = blueprintProcessorFactory.get(request.getBlueprintText());
        Map<String, Set<String>> componentsByHostGroup = blueprintTextProcessor.getComponentsByHostGroup();
        for (Map.Entry<String, Set<String>> serviceHostgroupEntry : componentsByHostGroup.entrySet()) {
            for (String service : serviceHostgroupEntry.getValue()) {
                List<ConfigQueryEntry> collectedEntries = configQueryEntries.getEntries()
                        .stream()
                        .filter(configQueryEntry -> configQueryEntry.getRelatedService().equalsIgnoreCase(service))
                        .filter(configQueryEntry -> configQueryEntry.getSupportedStorages().contains(request.getFileSystemType().toUpperCase()))
                        .map(configQueryEntry -> configQueryEntry.copy())
                        .collect(Collectors.toList());
                filtered.addAll(collectedEntries);
            }
        }
        String fileSystemType = request.getFileSystemType();
        String protocol = FileSystemType.valueOf(fileSystemType).getProtocol();
        for (ConfigQueryEntry configQueryEntry : filtered) {
            try {
                configQueryEntry.setProtocol(protocol);
                configQueryEntry.setDefaultPath(generateConfigWithParameters(request, protocol, configQueryEntry.getDefaultPath()));
            } catch (IOException e) {
                configQueryEntry.setDefaultPath(configQueryEntry.getDefaultPath());
            }
        }
        filtered = filtered.stream().sorted(Comparator.comparing(ConfigQueryEntry::getPropertyName)).collect(Collectors.toList());
        return filtered;
    }

    private String generateConfigWithParameters(FileSystemConfigQueryObject fileSystemConfigQueryObject, String protocol, String sourceTemplate)
            throws IOException {
        Template template = handlebars.compileInline(sourceTemplate, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        Map<String, String> templateObject = new HashMap<>();
        templateObject.put("clusterName", fileSystemConfigQueryObject.getClusterName());
        templateObject.put("storageName", fileSystemConfigQueryObject.getStorageName());
        templateObject.put("blueprintText", fileSystemConfigQueryObject.getBlueprintText());
        templateObject.put("protocol", protocol);
        return template.apply(templateObject);
    }
}
