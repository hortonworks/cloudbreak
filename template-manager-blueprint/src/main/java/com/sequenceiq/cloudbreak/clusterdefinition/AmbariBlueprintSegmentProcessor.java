package com.sequenceiq.cloudbreak.clusterdefinition;

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

import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.template.model.RelatedServices;
import com.sequenceiq.cloudbreak.template.model.ServiceName;
import com.sequenceiq.cloudbreak.template.model.TemplateFiles;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class AmbariBlueprintSegmentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariBlueprintSegmentProcessor.class);

    private static final String SERVICES_JSON = "services.json";

    @Inject
    private TemplateProcessor templateProcessor;

    @Inject
    private AmbariBlueprintSegmentReader ambariBlueprintSegmentReader;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    public String process(String blueprintText, TemplatePreparationObject source) {
        Map<String, Object> customProperties = new HashMap<>();
        AtomicReference<String> resultBlueprint = new AtomicReference<>(blueprintText);

        Map<ServiceName, TemplateFiles> configurationValueMap = ambariBlueprintSegmentReader.collectAllConfigFile();
        LOGGER.debug("The collected entries are for configurationValueMap : {}", configurationValueMap);
        collectContents(configurationValueMap, resultBlueprint.get(), file -> {
            LOGGER.debug("The actual file is: {}", file);
            String configContent = prepareContent(file, source, customProperties);
            LOGGER.debug("The generated content is: {}", configContent);
            customProperties.put(getCustomPropertyName(file), configContent);
        });

        Map<ServiceName, TemplateFiles> configMap = ambariBlueprintSegmentReader.collectAllServiceFile();
        LOGGER.debug("The collected entries are for configMap: {}", configMap);
        collectContents(configMap, resultBlueprint.get(), file -> {
            LOGGER.debug("The actual file is: {}", file);
            String serviceContent = prepareContent(file, source, customProperties);
            LOGGER.debug("The generated content is: {}", serviceContent);
            resultBlueprint.set(ambariBlueprintProcessorFactory.get(resultBlueprint.get()).addConfigEntryStringToBlueprint(serviceContent, false).asText());
        });

        Map<ServiceName, TemplateFiles> settingsMap = ambariBlueprintSegmentReader.collectAllSettingsFile();
        LOGGER.debug("The collected entries are for settingsMap: {}", settingsMap);
        collectContents(settingsMap, resultBlueprint.get(), file -> {
            LOGGER.debug("The actual file is: {}", file);
            String serviceContent = prepareContent(file, source, customProperties);
            LOGGER.debug("The generated content is: {}", serviceContent);
            resultBlueprint.set(ambariBlueprintProcessorFactory.get(resultBlueprint.get()).addSettingsEntryStringToBlueprint(serviceContent, false).asText());

        });

        Map<ServiceName, TemplateFiles> kerberosDescriptorMap = ambariBlueprintSegmentReader.collectAllKerberosDescriptorFile();
        LOGGER.debug("The collected entries are for kerberosDescriptorMap: {}", kerberosDescriptorMap);
        collectContents(kerberosDescriptorMap, resultBlueprint.get(), file -> {
            LOGGER.debug("The actual file is: {}", file);
            String kerberosDescriptorServiceContent = prepareContent(file, source, customProperties);
            LOGGER.debug("The generated content is: {}", kerberosDescriptorServiceContent);
            resultBlueprint.set(ambariBlueprintProcessorFactory.get(resultBlueprint.get())
                    .addKerberosDescriptorEntryStringToBlueprint(kerberosDescriptorServiceContent, false).asText());
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

    private String prepareContent(final String filePath, TemplatePreparationObject source, Map<String, Object> configs) {
        String result;
        String content = readFileFromClasspathQuietly(filePath);
        try {
            result = templateProcessor.process(content, source, configs);
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
            LOGGER.debug("Service file is not presented in {} list.", templateFiles.getFiles());
            shouldGenerate = true;
        } else {
            try {
                LOGGER.debug("Service file is presented in {} list.", templateFiles.getFiles());
                RelatedServices relatedServices = JsonUtil.readValue(readFileFromClasspathQuietly(requiredServices.get()), RelatedServices.class);
                LOGGER.debug("Related services are {}.", relatedServices.getServices());
                if (relatedServices.getServices().isEmpty()) {
                    LOGGER.debug("Related services are empty in list {}.", templateFiles.getFiles());
                    shouldGenerate = true;
                } else {
                    LOGGER.debug("Related services list is not empty checking the blueprint that components {} are exist.", relatedServices.getServices());
                    shouldGenerate = ambariBlueprintProcessorFactory.get(blueprintText).isComponentsExistsInBlueprint(relatedServices.getServices());
                    LOGGER.debug("The mechanism should generate configurations [{}] for {} services.", shouldGenerate, relatedServices.getServices());
                }
            } catch (IOException e) {
                LOGGER.error("Could not open {} file to check related service list and the template files were {}.",
                        requiredServices.get(), templateFiles.getFiles());
                shouldGenerate = false;
            }
        }
        return shouldGenerate;
    }

}
