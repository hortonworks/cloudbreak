package com.sequenceiq.cloudbreak.cmtemplate.cloudstorage;

import java.io.IOException;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.template.filesystem.CloudStorageConfigDetails;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntries;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;

@Service
public class CmCloudStorageConfigDetails {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmCloudStorageConfigDetails.class);

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private CloudStorageConfigDetails cloudStorageConfigDetails;

    private ConfigQueryEntries configQueryEntries;

    @PostConstruct
    public void init() {
        String configDefinitions = cloudbreakResourceReaderService.resourceDefinition("cm-cloud-storage-location-specification");
        try {
            configQueryEntries = JsonUtil.readValue(configDefinitions, ConfigQueryEntries.class);
        } catch (IOException e) {
            LOGGER.error("Cannot initialize configQueryEntries", e);
            configQueryEntries = new ConfigQueryEntries();
        }
    }

    public Set<ConfigQueryEntry> queryParameters(FileSystemConfigQueryObject request) {
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(request.getBlueprintText());
        return cloudStorageConfigDetails.queryParameters(cmTemplateProcessor, configQueryEntries, request);
    }

}
