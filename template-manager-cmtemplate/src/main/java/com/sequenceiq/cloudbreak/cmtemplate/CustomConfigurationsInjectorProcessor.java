package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class CustomConfigurationsInjectorProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomConfigurationsInjectorProcessor.class);

    public void process(CmTemplateProcessor processor, TemplatePreparationObject source) {
        source.getCustomConfigurationsView().ifPresent(
                customConfigsView -> {
                    Map<String, List<ApiClusterTemplateConfig>> serviceNameMappedToCustomServiceConfigs =
                            processor.getCustomServiceConfigsMap(customConfigsView.getConfigurations());
                    Map<String, List<ApiClusterTemplateRoleConfigGroup>> serviceNameMappedToCustomRoleConfigs =
                            processor.getCustomRoleConfigsMap(customConfigsView.getConfigurations());
                    List<ApiClusterTemplateService> services = Optional.ofNullable(processor.getTemplate().getServices()).orElse(List.of());
                    processServiceConfigs(serviceNameMappedToCustomServiceConfigs, processor, services);
                    processRoleConfigs(serviceNameMappedToCustomRoleConfigs, processor, services);
                }
        );
    }

    private void processServiceConfigs(Map<String, List<ApiClusterTemplateConfig>> serviceNameMappedToCustomServiceConfigs, CmTemplateProcessor processor,
            List<ApiClusterTemplateService> services) {
        serviceNameMappedToCustomServiceConfigs.forEach((String serviceName, List<ApiClusterTemplateConfig> serviceConfigs) -> {
            Optional<ApiClusterTemplateService> serviceOpt = services.stream()
                    .filter(service -> service.getServiceType().equalsIgnoreCase(serviceName))
                    .findFirst();
            if (serviceOpt.isEmpty()) {
                LOGGER.info("Service with name " + serviceName + " does not exist for the current template");
            } else {
                processor.mergeCustomServiceConfigs(serviceOpt.get(), serviceConfigs);
            }
        });
    }

    private void processRoleConfigs(Map<String, List<ApiClusterTemplateRoleConfigGroup>> serviceNameMappedToCustomRoleConfigs, CmTemplateProcessor processor,
            List<ApiClusterTemplateService> services) {
        serviceNameMappedToCustomRoleConfigs.forEach((String serviceName, List<ApiClusterTemplateRoleConfigGroup> roleConfigs) -> {
            Optional<ApiClusterTemplateService> serviceOpt = services.stream()
                    .filter(service -> service.getServiceType().equalsIgnoreCase(serviceName))
                    .findFirst();
            if (serviceOpt.isEmpty()) {
                LOGGER.info("Service with name " + serviceName + " does not exist for the current template");
            } else {
                processor.mergeCustomRoleConfigs(serviceOpt.get(), roleConfigs);
            }
        });
    }
}
