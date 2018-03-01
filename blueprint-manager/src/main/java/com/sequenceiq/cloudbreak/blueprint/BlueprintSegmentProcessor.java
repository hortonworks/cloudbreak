package com.sequenceiq.cloudbreak.blueprint;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspathQuietly;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.template.BlueprintTemplateProcessor;
import com.sequenceiq.cloudbreak.blueprint.templates.RelatedServices;
import com.sequenceiq.cloudbreak.blueprint.templates.ServiceName;
import com.sequenceiq.cloudbreak.blueprint.templates.TemplateFiles;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintSegmentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintSegmentProcessor.class);

    private static final String SERVICES_JSON = "services.json";

    @Inject
    private BlueprintTemplateProcessor blueprintTemplateProcessor;

    @Inject
    private BlueprintSegmentReader blueprintSegmentReader;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    public String process(BlueprintPreparationObject source, String blueprintText) {
        Map<String, Object> customProperties = new HashMap<>();
        AtomicReference<String> resultBlueprint = new AtomicReference<>(blueprintText);

        collectContents(blueprintSegmentReader.collectAllConfigFile(), resultBlueprint.get(), file -> {
            String configContent = prepareContent(file, source, customProperties);
            customProperties.put(getCustomPropertyName(file), configContent);
        });

        collectContents(blueprintSegmentReader.collectAllServiceFile(), resultBlueprint.get(), file -> {
            String serviceContent = prepareContent(file, source, customProperties);
            resultBlueprint.set(blueprintProcessor.addConfigEntryStringToBlueprint(serviceContent, resultBlueprint.get()));
        });
        return resultBlueprint.get();
    }

    private String getCustomPropertyName(String file) {
        return file.split("\\.")[0].replaceAll("[^A-Za-z0-9 ]", "_");
    }

    private void collectContents(Map<ServiceName, TemplateFiles> map, String blueprintText, Consumer<String> function) {
        map.forEach((ServiceName key, TemplateFiles value) -> {
            if (shouldGenerateTemplates(value, blueprintText)) {
                for (String serviceFilePath : collectAllFileWithoutRelatedServiceFile(value)) {
                    function.accept(serviceFilePath);
                }
            }
        });
    }

    private List<String> collectAllFileWithoutRelatedServiceFile(TemplateFiles value) {
        return value.getFiles().stream().filter(item -> !item.endsWith(SERVICES_JSON)).collect(Collectors.toList());
    }

    private String prepareContent(final String filePath, BlueprintPreparationObject source, Map<String, Object> configs) {
        String result;
        String content = readFileFromClasspathQuietly(filePath);
        try {
            result = blueprintTemplateProcessor.process(content, source, configs);
        } catch (IOException e) {
            LOGGER.error("Could not open {} file to generate result based on template.", filePath);
            result = content;
        }
        return result;
    }

    private boolean shouldGenerateTemplates(TemplateFiles templateFiles, String blueprintText) {
        boolean shouldGenerate;
        Optional<String> requiredServices = templateFiles.getFiles().stream().filter(item -> item.endsWith(SERVICES_JSON)).findFirst();
        if (!requiredServices.isPresent()) {
            shouldGenerate = true;
        } else {
            try {
                RelatedServices relatedServices = JsonUtil.readValue(readFileFromClasspathQuietly(requiredServices.get()), RelatedServices.class);
                shouldGenerate = blueprintProcessor.componentsExistsInBlueprint(relatedServices.getServices(), blueprintText);
            } catch (IOException e) {
                LOGGER.error("Could not open {} file to check related service list.", requiredServices.get());
                shouldGenerate = false;
            }
        }
        return shouldGenerate;
    }

}
