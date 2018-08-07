package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.blueprint.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.blueprint.filesystem.query.FileSystemConfigQueryService;
import com.sequenceiq.cloudbreak.blueprint.template.BlueprintTemplateProcessor;
import com.sequenceiq.cloudbreak.blueprint.template.HandleBarModelKey;
import com.sequenceiq.cloudbreak.blueprint.template.TemplateParameterFilter;

@Component
public class CentralBlueprintParameterQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralBlueprintParameterQueryService.class);

    @Inject
    private BlueprintTemplateProcessor blueprintTemplateProcessor;

    @Inject
    private TemplateParameterFilter templateParameterFilter;

    @Inject
    private FileSystemConfigQueryService fileSystemConfigQueryService;

    public Set<String> queryDatalakeParameters(String sourceTemplate) throws BlueprintProcessingException {
        Set<String> blueprintParameters;
        try {
            blueprintParameters = templateParameterFilter.queryForDatalakeParameters(
                    HandleBarModelKey.DATALAKE,
                    blueprintTemplateProcessor.queryParameters(sourceTemplate));
        } catch (IOException e) {
            String message = String.format("Unable to query blueprint parameters from blueprint which was: %s", sourceTemplate);
            LOGGER.warn(message);
            throw new BlueprintProcessingException(message, e);
        }
        return blueprintParameters;
    }

    public Set<String> queryCustomParameters(String sourceTemplate) throws BlueprintProcessingException {
        Set<String> blueprintParameters;
        try {
            blueprintParameters = templateParameterFilter.queryForCustomParameters(blueprintTemplateProcessor.queryParameters(sourceTemplate));
        } catch (IOException e) {
            String message = String.format("Unable to query blueprint parameters from blueprint which was: %s", sourceTemplate);
            LOGGER.warn(message);
            throw new BlueprintProcessingException(message, e);
        }
        return blueprintParameters;
    }

    public Set<ConfigQueryEntry> queryFileSystemParameters(FileSystemConfigQueryObject fileSystemConfigQueryObject) throws BlueprintProcessingException {
        return fileSystemConfigQueryService.queryParameters(fileSystemConfigQueryObject);
    }
}
