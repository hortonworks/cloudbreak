package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspathQuietly;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.StackVersion;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceConfig;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceDependencies;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.versionmatrix.CdhService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.versionmatrix.ServiceList;

@Service
public class CmTemplateGeneratorConfigurationResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateGeneratorConfigurationResolver.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${cb.blueprint.cm.version.files:cloudera-manager-template/cdh}")
    private String cdhConfigurationsPath;

    @Value("${cb.blueprint.cm.services.file:cloudera-manager-template/service-definitions-minimal.json}")
    private String serviceDefinitionConfigurationPath;

    private Map<StackVersion, Set<CdhService>> cdhConfigurationsMap = new HashMap<>();

    private Set<ServiceConfig> serviceConfigs = new HashSet<>();

    @PostConstruct
    public void prepareConfigs() {
        cdhConfigurationsMap = readAllFilesFromParameterDir();
        serviceConfigs = readServiceDefinitions();
    }

    public Map<StackVersion, Set<CdhService>> cdhConfigurations() {
        return cdhConfigurationsMap;
    }

    public Set<ServiceConfig> serviceConfigs() {
        return serviceConfigs;
    }

    private Set<ServiceConfig> readServiceDefinitions() {
        Set<ServiceConfig> serviceConfigs = new HashSet<>();
        String content = readFileFromClasspathQuietly(serviceDefinitionConfigurationPath);
        try {
            ServiceDependencies serviceDependencies = MAPPER.readValue(content, ServiceDependencies.class);
            serviceConfigs = serviceDependencies.getServices();
        } catch (IOException ex) {
            String message = String.format("Could not read service definitions from: %s", serviceDefinitionConfigurationPath);
            LOGGER.error(message, ex);
        }
        return serviceConfigs;
    }

    private Map<StackVersion, Set<CdhService>> readAllFilesFromParameterDir() {
        Map<StackVersion, Set<CdhService>> collectedFiles = new HashMap<>();
        try {
            List<Resource> files = getFiles(cdhConfigurationsPath);
            for (Resource serviceEntry : files) {
                String[] serviceAndPath = serviceEntry.getURL().getPath().split(cdhConfigurationsPath);
                String cdhVersion = serviceAndPath[1].split("/")[1].replace(".json", "");
                String insideFolder = String.format("%s%s", cdhConfigurationsPath, serviceAndPath[1]);
                LOGGER.debug("The entry url: {} file url: {} for version: {}", serviceEntry, insideFolder, cdhVersion);
                String content = readFileFromClasspathQuietly(insideFolder);
                ServiceList serviceList = MAPPER.readValue(content, ServiceList.class);
                StackVersion stackVersion = new StackVersion();
                stackVersion.setStackType(serviceList.getStackType());
                stackVersion.setVersion(serviceList.getVersion());
                collectedFiles.put(stackVersion, serviceList.getServices());
            }
        } catch (IOException ex) {
            String message = String.format("Could not read files from folder: %s", cdhConfigurationsPath);
            LOGGER.error(message, ex);
        }
        return collectedFiles;
    }

    private List<Resource> getFiles(String configDir) throws IOException {
        return Arrays.asList(new PathMatchingResourcePatternResolver()
                .getResources("classpath:" + configDir + "/*.json"));
    }
}
