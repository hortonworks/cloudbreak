package com.sequenceiq.cloudbreak.clusterdefinition.filesystem;

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
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
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
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    private ConfigQueryEntries configQueryEntries;

    @PostConstruct
    public void init() {
        String configDefinitions = cloudbreakResourceReaderService.resourceDefinition("cloud-storage-location-specification");
        try {
            configQueryEntries = JsonUtil.readValue(configDefinitions, ConfigQueryEntries.class);
        } catch (IOException e) {
            LOGGER.error("Cannot initialize configQueryEntries", e);
            configQueryEntries = new ConfigQueryEntries();
        }
    }

    public Set<ConfigQueryEntry> queryParameters(FileSystemConfigQueryObject request) {
        Set<ConfigQueryEntry> filtered = new HashSet<>();

        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = ambariBlueprintProcessorFactory.get(request.getBlueprintText());
        Map<String, Set<String>> componentsByHostGroup = ambariBlueprintTextProcessor.getComponentsByHostGroup();
        for (Map.Entry<String, Set<String>> serviceHostgroupEntry : componentsByHostGroup.entrySet()) {
            for (String service : serviceHostgroupEntry.getValue()) {
                Set<ConfigQueryEntry> collectedEntries = configQueryEntries.getEntries()
                        .stream()
                        .filter(configQueryEntry -> configQueryEntry.getRelatedServices().stream().
                                anyMatch(relatedService -> relatedService.equalsIgnoreCase(service)))
                        .filter(configQueryEntry -> configQueryEntry.getSupportedStorages().contains(request.getFileSystemType().toUpperCase()))
                        .collect(Collectors.toSet());
                filtered.addAll(collectedEntries);
            }
        }
        boolean attachedCluster = request.isAttachedCluster();
        List<ConfigQueryEntry> collectedEntries = configQueryEntries.getEntries()
                .stream()
                .filter(configQueryEntry -> configQueryEntry.isRequiredForAttachedCluster() && attachedCluster)
                .collect(Collectors.toList());
        filtered.addAll(collectedEntries);
        String fileSystemTypeRequest = request.getFileSystemType();
        FileSystemType fileSystemType = FileSystemType.valueOf(fileSystemTypeRequest);
        Map<String, Object> templateObject = getTemplateObject(request, fileSystemType.getProtocol());
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

    private String generateConfigWithParameters(String sourceTemplate, FileSystemType fileSystemType, Map<String, Object> templateObject) throws IOException {
        String defaultPath = fileSystemType.getDefaultPath();
        Template defaultPathTemplate = handlebars.compileInline(defaultPath, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        templateObject.put("defaultPath", defaultPathTemplate.apply(templateObject));
        Template template = handlebars.compileInline(sourceTemplate, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        return template.apply(templateObject);
    }

    private Map<String, Object> getTemplateObject(FileSystemConfigQueryObject fileSystemConfigQueryObject, String protocol) {
        Map<String, Object> templateObject = new HashMap<>();
        templateObject.put("clusterName", fileSystemConfigQueryObject.getClusterName());
        templateObject.put("attachedCluster", fileSystemConfigQueryObject.isAttachedCluster());
        templateObject.put("datalakeCluster", fileSystemConfigQueryObject.isDatalakeCluster());
        templateObject.put("storageName", fileSystemConfigQueryObject.getStorageName());
        templateObject.put("blueprintText", fileSystemConfigQueryObject.getBlueprintText());
        templateObject.put("accountName", fileSystemConfigQueryObject.getAccountName().orElse("default-account-name"));
        templateObject.put("protocol", protocol);
        return templateObject;
    }

}
