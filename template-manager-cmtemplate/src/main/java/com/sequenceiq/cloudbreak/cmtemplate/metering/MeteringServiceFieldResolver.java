package com.sequenceiq.cloudbreak.cmtemplate.metering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class MeteringServiceFieldResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringServiceFieldResolver.class);

    private static final String DEFAULT_SERVICE_TYPE = "DATAHUB";

    private String meteringServiceFieldMapperLocation;

    private Map<String, List<MeteringServiceFieldMapperDefinition>> meteringServiceFieldMaps = new HashMap<>();

    public MeteringServiceFieldResolver(
            @Value("${cb.cm.metering.service.field.mapper.location:definitions/cm-metering-service-field-mapping.json}")
            String meteringServiceFieldMapperLocation) {
        this.meteringServiceFieldMapperLocation = meteringServiceFieldMapperLocation;
    }

    /**.
     * Check that which service type should be used based on the CM template and a service type mapping definition file
     * At the moment it only do checks against services. In case of more specific requirements, this can be extended with
     * roles and configuration checks as well.
     * @param cmTemplateProcessor template processor that contains role and configuration data
     * @return resolved service type for the template
     */
    public String resolveServiceType(CmTemplateProcessor cmTemplateProcessor) {
        return resolveServiceField(cmTemplateProcessor, "type", "types", DEFAULT_SERVICE_TYPE);
    }

    /**.
     * Check that which service feature should be used based on the CM template and a service feature mapping definition file
     * At the moment it only do checks against services. In case of more specific requirements, this can be extended with
     * roles and configuration checks as well.
     * @param cmTemplateProcessor template processor that contains role and configuration data
     * @return resolved service type for the template
     */
    public String resolveServiceFeature(CmTemplateProcessor cmTemplateProcessor) {
        return resolveServiceField(cmTemplateProcessor, "feature", "features", null);
    }

    private String resolveServiceField(CmTemplateProcessor cmTemplateProcessor, String field, String jsonField, String defaultValue) {
        LOGGER.debug("Service {} needs to be calculated as no provided application tag for it", field);
        Set<ServiceComponent> serviceComponents = cmTemplateProcessor.getAllComponents();
        List<MeteringServiceFieldMapperDefinition> serviceFieldMapperDefinitions = meteringServiceFieldMaps.getOrDefault(jsonField, new ArrayList<>());
        String result = serviceFieldMapperDefinitions
                .stream()
                .filter(serviceTypeMapperDefinition ->
                        serviceTypeMapperDefinition
                                .getRelatedServices()
                                .stream()
                                .allMatch(relatedService -> containsService(serviceComponents, relatedService))
                )
                .findFirst()
                .map(MeteringServiceFieldMapperDefinition::getName)
                .orElse(defaultValue);
        LOGGER.debug("The resolved service {}: {}", field, result);
        return result;
    }

    private boolean containsService(Set<ServiceComponent> serviceComponents, String service) {
        return serviceComponents.stream().map(ServiceComponent::getService).anyMatch(service::equals);
    }

    @PostConstruct
    public void init() {
        try {
            this.meteringServiceFieldMaps = loadMeteringServiceDefinitions();
        } catch (IOException e) {
            LOGGER.warn("Static service type mapping definitions cannot be loaded.", e);
        }
    }

    private Map<String, List<MeteringServiceFieldMapperDefinition>> loadMeteringServiceDefinitions() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(meteringServiceFieldMapperLocation);
        if (classPathResource.exists()) {
            String json = FileReaderUtils.readFileFromClasspath(meteringServiceFieldMapperLocation);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<>() {
            });
        } else {
            LOGGER.debug("{} was not loaded successfully as thr definition file does not exist. " +
                    "Metering service field mapper definitions won't be used", meteringServiceFieldMapperLocation);
            return new HashMap<>();
        }
    }
}
