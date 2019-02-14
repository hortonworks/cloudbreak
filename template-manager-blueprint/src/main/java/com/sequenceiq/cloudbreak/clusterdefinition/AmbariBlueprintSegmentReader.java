package com.sequenceiq.cloudbreak.clusterdefinition;

import static com.sequenceiq.cloudbreak.template.model.ServiceName.serviceName;
import static com.sequenceiq.cloudbreak.template.model.TemplateFiles.templateFiles;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.model.ServiceName;
import com.sequenceiq.cloudbreak.template.model.TemplateFiles;

@Component
public class AmbariBlueprintSegmentReader implements ResourceLoaderAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariBlueprintSegmentReader.class);

    @Value("${cb.blueprint.template.path:blueprints/configurations}")
    private String blueprintTemplatePath;

    @Value("${cb.blueprint.basic.path:blueprints/basics}")
    private String basicTemplatePath;

    @Value("${cb.blueprint.settings.path:blueprints/settings}")
    private String settingsTemplatePath;

    @Value("${cb.blueprint.security.kerberos_descriptor.path:blueprints/security/kerberos_descriptor}")
    private String kerberosDescriptorTemplatePath;

    private ResourceLoader resourceLoader;

    public Map<ServiceName, TemplateFiles> collectAllServiceFile() {
        return readAllFilesFromParameterDir(blueprintTemplatePath);
    }

    public Map<ServiceName, TemplateFiles> collectAllConfigFile() {
        return readAllFilesFromParameterDir(basicTemplatePath);
    }

    public Map<ServiceName, TemplateFiles> collectAllSettingsFile() {
        return readAllFilesFromParameterDir(settingsTemplatePath);
    }

    public Map<ServiceName, TemplateFiles> collectAllKerberosDescriptorFile() {
        return readAllFilesFromParameterDir(kerberosDescriptorTemplatePath);
    }

    private Map<ServiceName, TemplateFiles> readAllFilesFromParameterDir(String dir) {
        Map<ServiceName, TemplateFiles> collectedFiles = new HashMap<>();
        try {
            List<Resource> files = getFiles(dir);
            for (final Resource serviceEntry : files) {
                String[] serviceAndPath = serviceEntry.getURL().getPath().split(dir);
                String simpleServiceName = serviceAndPath[1].split("/")[1];
                String insideFolder = String.format("%s%s", dir, serviceAndPath[1]);
                LOGGER.debug("The the entry url is: {} file url is : {} for service: {}", serviceEntry, insideFolder, simpleServiceName);
                ServiceName serviceName = serviceName(simpleServiceName);
                if (!collectedFiles.keySet().contains(serviceName)) {
                    collectedFiles.put(serviceName, templateFiles(Lists.newArrayList()));
                }
                collectedFiles.get(serviceName).getFiles().add(insideFolder);
            }
        } catch (IOException ex) {
            String message = String.format("Could not read files from the definiated folder which was: %s", dir);
            LOGGER.error(message, ex);
            throw new ClusterDefinitionProcessingException(message, ex);
        }

        return collectedFiles;
    }

    private List<Resource> getFiles(String configDir) throws IOException {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();

        List<Resource> handleBarFiles = Arrays.stream(patternResolver.getResources("classpath:" + configDir + "/*/*.handlebars"))
                .collect(toList());
        List<Resource> jsonFiles = Arrays.stream(patternResolver.getResources("classpath:" + configDir + "/*/*.json"))
                .collect(toList());

        List<Resource> resources = new ArrayList<>();
        resources.addAll(handleBarFiles);
        resources.addAll(jsonFiles);

        return resources;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
