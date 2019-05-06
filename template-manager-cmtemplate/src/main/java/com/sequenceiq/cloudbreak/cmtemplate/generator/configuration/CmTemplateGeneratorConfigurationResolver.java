package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspathQuietly;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.StackVersion;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceConfig;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceDependecies;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.versionmatrix.ServiceList;

@Service
public class CmTemplateGeneratorConfigurationResolver implements ResourceLoaderAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateGeneratorConfigurationResolver.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${cb.blueprint.cm.version.files:cloudera-manager-template/cdh}")
    private String cdhConfigurationsPath;

    @Value("${cb.blueprint.cm.services.file:cloudera-manager-template/service-definitions-minimal.json}")
    private String serviceDefinitionConfigurationPath;

    private ResourceLoader resourceLoader;

    private Map<StackVersion, Set<String>> cdhConfigurationsMap = new HashMap<>();

    private Set<ServiceConfig> serviceInformations = new HashSet<>();

    @PostConstruct
    public void prepareConfigs() {
        cdhConfigurationsMap = readAllFilesFromParameterDir();
        serviceInformations = readServiceDefinitions();
    }

    public Map<StackVersion, Set<String>> cdhConfigurations() {
        return cdhConfigurationsMap;
    }

    public Set<ServiceConfig> serviceInformations() {
        return serviceInformations;
    }

    private Set<ServiceConfig> readServiceDefinitions() {
        Set<ServiceConfig> serviceConfigs = new HashSet<>();
        String content = readFileFromClasspathQuietly(serviceDefinitionConfigurationPath);
        try {
            ServiceDependecies serviceDependecies = MAPPER.readValue(content, ServiceDependecies.class);
            serviceConfigs = serviceDependecies.getServices();
        } catch (IOException ex) {
            String message = String.format("Could not read files from the definiated folder which was: %s", cdhConfigurationsPath);
            LOGGER.error(message, ex);
        }
        return serviceConfigs;
    }

    private Map<StackVersion, Set<String>> readAllFilesFromParameterDir() {
        Map<StackVersion, Set<String>> collectedFiles = new HashMap<>();
        try {
            List<Resource> files = getFiles(cdhConfigurationsPath);
            for (Resource serviceEntry : files) {
                String[] serviceAndPath = serviceEntry.getURL().getPath().split(cdhConfigurationsPath);
                String cdhVersion = serviceAndPath[1].split("/")[1].replace(".json", "");
                String insideFolder = String.format("%s%s", cdhConfigurationsPath, serviceAndPath[1]);
                LOGGER.debug("The the entry url is: {} file url is : {} for version: {}", serviceEntry, insideFolder, cdhVersion);
                String content = readFileFromClasspathQuietly(insideFolder);
                ServiceList serviceList = MAPPER.readValue(content, ServiceList.class);
                StackVersion stackVersion = new StackVersion();
                stackVersion.setStackType(serviceList.getStackType());
                stackVersion.setVersion(serviceList.getVersion());
                collectedFiles.put(stackVersion, serviceList.getServices());
            }
        } catch (IOException ex) {
            String message = String.format("Could not read files from the definiated folder which was: %s", cdhConfigurationsPath);
            LOGGER.error(message, ex);
        }
        return collectedFiles;
    }

    private List<Resource> getFiles(String configDir) throws IOException {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        List<Resource> jsonFiles = Arrays.stream(patternResolver
                .getResources("classpath:" + configDir + "/*.json"))
                .collect(toList());
        List<Resource> resources = new ArrayList<>();
        resources.addAll(jsonFiles);
        return resources;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
