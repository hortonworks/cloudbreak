package com.sequenceiq.cloudbreak.cmtemplate.servicetype;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class ServiceTypeResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTypeResolver.class);

    private static final String DEFAULT_SERVICE_TYPE = "DATAHUB";

    private final String serviceTypeMapperLocation;

    private List<ServiceTypeMapperDefinition> serviceTypeMapperDefinitions;

    public ServiceTypeResolver(@Value("${cb.cm.service.type.mapper.location:definitions/cm-service-type-mapping.json}")
            String serviceTypeMapperLocation) {
        this.serviceTypeMapperLocation = serviceTypeMapperLocation;
    }

    /**.
     * Check that which service type should be used based on the CM template and a service type mapping definition file
     * At the moment it only do checks against services. In case of more specific requirements, this can be extended with
     * roles and configuration checks as well.
     * @param cmTemplateProcessor template processor that contains role and configuration data
     * @return resolved service type for the template
     */
    public String resolveServiceType(CmTemplateProcessor cmTemplateProcessor) {
        LOGGER.debug("Service type needs to be calculated as no provided application tag for it");
        Set<ServiceComponent> serviceComponents = cmTemplateProcessor.getAllComponents();
        String result = this.serviceTypeMapperDefinitions
                .stream()
                .filter(serviceTypeMapperDefinition ->
                    serviceTypeMapperDefinition
                            .getRelatedServices()
                            .stream()
                            .allMatch(relatedService -> containsService(serviceComponents, relatedService))
                )
                .findFirst()
                .map(ServiceTypeMapperDefinition::getType)
                .orElse(DEFAULT_SERVICE_TYPE);
        LOGGER.debug("The resolved service type: {}", result);
        return result;
    }

    private boolean containsService(Set<ServiceComponent> serviceComponents, String service) {
        return serviceComponents.stream().map(ServiceComponent::getService).anyMatch(service::equals);
    }

    @PostConstruct
    public void init() {
        try {
            this.serviceTypeMapperDefinitions = loadServiceTypeDefinitions();
        } catch (IOException e) {
            LOGGER.warn("Static service type mapping definitions cannot be loaded.", e);
        }
    }

    private List<ServiceTypeMapperDefinition> loadServiceTypeDefinitions() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(serviceTypeMapperLocation);
        if (classPathResource.exists()) {
            String json = FileReaderUtils.readFileFromClasspath(serviceTypeMapperLocation);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<>() {
            });
        } else {
            LOGGER.debug("{} was not loaded successfully as thr definition file does not exist. " +
                    "Service type mapper definitions won't be used", serviceTypeMapperLocation);
            return new ArrayList<>();
        }
    }

}
