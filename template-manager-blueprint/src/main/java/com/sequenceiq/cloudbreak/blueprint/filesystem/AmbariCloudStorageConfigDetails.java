package com.sequenceiq.cloudbreak.blueprint.filesystem;

import java.io.IOException;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.template.filesystem.CloudStorageConfigDetails;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntries;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;

@Service
public class AmbariCloudStorageConfigDetails {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariCloudStorageConfigDetails.class);

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private CloudStorageConfigDetails cloudStorageConfigDetails;

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
        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = ambariBlueprintProcessorFactory.get(request.getBlueprintText());
        return cloudStorageConfigDetails.queryParameters(ambariBlueprintTextProcessor, configQueryEntries, request);
    }
}
