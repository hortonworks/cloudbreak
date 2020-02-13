package com.sequenceiq.cloudbreak.template.filesystem;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.sequenceiq.cloudbreak.HandlebarTemplate;
import com.sequenceiq.cloudbreak.HandlebarUtils;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntries;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;
import com.sequenceiq.common.model.FileSystemType;

@Service
public class CloudStorageConfigDetails {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageConfigDetails.class);

    private final Handlebars handlebars = HandlebarUtils.handlebars();

    public Set<ConfigQueryEntry> queryParameters(BlueprintTextProcessor blueprintTextProcessor,
            ConfigQueryEntries configQueryEntries, FileSystemConfigQueryObject request) {

        Set<ConfigQueryEntry> filtered = new HashSet<>();
        Map<String, Set<String>> componentsByHostGroup = blueprintTextProcessor.getComponentsByHostGroup();
        boolean attachedCluster = request.isAttachedCluster();

        for (Map.Entry<String, Set<String>> serviceHostgroupEntry : componentsByHostGroup.entrySet()) {
            for (String service : serviceHostgroupEntry.getValue()) {
                Set<ConfigQueryEntry> collectedEntries = configQueryEntries.getEntries()
                        .stream()
                        .filter(configQueryEntry -> configQueryEntry.getRelatedServices().stream().
                                anyMatch(relatedService -> relatedService.equalsIgnoreCase(service)))
                        .filter(configQueryEntry -> {
                            if ((configQueryEntry.isRequiredForAttachedCluster() && attachedCluster) || !attachedCluster) {
                                return true;
                            }
                            return false;
                        })
                        .filter(configQueryEntry -> configQueryEntry.getSupportedStorages().contains(request.getFileSystemType().toUpperCase()))
                        .collect(Collectors.toSet());
                filtered.addAll(collectedEntries);
            }
        }

        Set<ConfigQueryEntry> collectedEntries = configQueryEntries.getEntries()
                .stream()
                .filter(configQueryEntry -> blueprintDoesNotContainActual(configQueryEntry.getRelatedMissingServices(), componentsByHostGroup))
                .collect(Collectors.toSet());
        filtered.addAll(collectedEntries);
        String fileSystemTypeRequest = request.getFileSystemType();
        FileSystemType fileSystemType = FileSystemType.valueOf(fileSystemTypeRequest);
        String protocol = fileSystemType.getProtocol();
        Map<String, Object> templateObject = getTemplateObject(request, protocol);
        for (ConfigQueryEntry configQueryEntry : filtered) {
            try {
                boolean secure = request.isSecure();
                configQueryEntry.setProtocol(secure ? protocol + "s" : protocol);
                configQueryEntry.setSecure(secure);
                configQueryEntry.setDefaultPath(generateConfigWithParameters(configQueryEntry.getDefaultPath(), fileSystemType, templateObject));
            } catch (IOException e) {
            }
        }
        filtered = filtered.stream().sorted(Comparator.comparing(ConfigQueryEntry::getPropertyName)).collect(Collectors.toCollection(LinkedHashSet::new));
        return filtered;
    }

    private boolean blueprintDoesNotContainActual(Set<String> relatedMissingServices, Map<String, Set<String>> componentsByHostGroup) {
        boolean contains = false;
        if (!relatedMissingServices.isEmpty()) {
            for (Map.Entry<String, Set<String>> stringSetEntry : componentsByHostGroup.entrySet()) {
                for (String service : stringSetEntry.getValue()) {
                    for (String relatedMissingService : relatedMissingServices) {
                        if (relatedMissingService.equalsIgnoreCase(service)) {
                            contains = true;
                            break;
                        }
                    }

                }
            }
        } else {
            contains = true;
        }
        return !contains;
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
        templateObject.put("storageName", trimStoragePostfixIfNeeded(fileSystemConfigQueryObject.getStorageName(),
                fileSystemConfigQueryObject.getFileSystemType()));
        templateObject.put("blueprintText", fileSystemConfigQueryObject.getBlueprintText());
        templateObject.put("accountName", fileSystemConfigQueryObject.getAccountName().orElse("default-account-name"));
        templateObject.put("protocol", fileSystemConfigQueryObject.isSecure() ? protocol + "s" : protocol);
        return templateObject;
    }

    private String trimStoragePostfixIfNeeded(String storageName, String fileSystemTypeNm) {
        FileSystemType fileSystemType = FileSystemType.valueOf(fileSystemTypeNm);
        String postfix = fileSystemType.getPostFix();
        return storageName.replaceAll(postfix, "");
    }
}
