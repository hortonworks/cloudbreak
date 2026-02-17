package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspathQuietly;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceConfig;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceDependencies;

@Service
public class CmTemplateGeneratorConfigurationResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateGeneratorConfigurationResolver.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${cb.blueprint.cm.services.file:cloudera-manager-template/service-definitions-minimal.json}")
    private String serviceDefinitionConfigurationPath;

    private Set<ServiceConfig> serviceConfigs = new HashSet<>();

    @PostConstruct
    public void prepareConfigs() {
        serviceConfigs = readServiceDefinitions();
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
}
