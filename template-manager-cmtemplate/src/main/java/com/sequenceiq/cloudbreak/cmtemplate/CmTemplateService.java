package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;

@Component
public class CmTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateService.class);

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public boolean isServiceTypePresent(String serviceType, String blueprint) {
        Set<String> serviceTypes = getServiceTypes(blueprint);
        LOGGER.debug("Searching for {} service type in the following set of service types {} which are extracted from blueprint: {}", serviceType, serviceTypes,
                blueprint);
        return serviceTypes.stream().anyMatch(serviceType::equals);
    }

    private Set<String> getServiceTypes(String blueprintText) {
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return getServiceTypesFromTemplate(cmTemplateProcessor);
    }

    private Set<String> getServiceTypesFromTemplate(CmTemplateProcessor cmTemplateProcessor) {
        return cmTemplateProcessor.getTemplate()
                .getServices()
                .stream()
                .map(ApiClusterTemplateService::getServiceType)
                .collect(Collectors.toSet());
    }
}
