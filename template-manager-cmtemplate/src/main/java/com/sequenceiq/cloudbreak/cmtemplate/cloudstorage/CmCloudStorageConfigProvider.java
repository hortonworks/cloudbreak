package com.sequenceiq.cloudbreak.cmtemplate.cloudstorage;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.template.filesystem.CloudStorageConfigDetails;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntries;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;

@Component
public class CmCloudStorageConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmCloudStorageConfigProvider.class);

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private final CloudbreakResourceReaderService cloudbreakResourceReaderService;

    private final CloudStorageConfigDetails cloudStorageConfigDetails;

    private final ConfigQueryEntries configQueryEntries;

    public CmCloudStorageConfigProvider(CloudbreakResourceReaderService cloudbreakResourceReaderService, CloudStorageConfigDetails cloudStorageConfigDetails) {
        this.cloudbreakResourceReaderService = cloudbreakResourceReaderService;
        this.cloudStorageConfigDetails = cloudStorageConfigDetails;
        String configDefinitions = cloudbreakResourceReaderService.resourceDefinition("cm-cloud-storage-location-specification");
        configQueryEntries = parseConfigQueryEntries(configDefinitions);
    }

    private ConfigQueryEntries parseConfigQueryEntries(String configDefinitions) {
        try {
            return JsonUtil.readValue(configDefinitions, ConfigQueryEntries.class);
        } catch (IOException e) {
            LOGGER.error("Cannot initialize configQueryEntries", e);
            return new ConfigQueryEntries();
        }
    }

    public Set<ConfigQueryEntry> queryParameters(FileSystemConfigQueryObject request) {
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(request.getBlueprintText());
        return cloudStorageConfigDetails.queryParameters(cmTemplateProcessor, configQueryEntries, request);
    }

    public Set<ConfigQueryEntry> queryParameters(Set<ConfigQueryEntry> entries, FileSystemConfigQueryObject request) {
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(request.getBlueprintText());
        return cloudStorageConfigDetails.queryParameters(cmTemplateProcessor, new ConfigQueryEntries(entries), request);
    }

    public ConfigQueryEntries getConfigQueryEntries() {
        return configQueryEntries;
    }
}
