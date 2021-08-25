package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@Component
public class CustomTemplateUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTemplateUpgradeValidator.class);

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private PermittedServicesForUpgradeService permittedServicesForUpgradeService;

    BlueprintValidationResult isValid(Blueprint blueprint) {
        Set<String> services = getServicesFromBlueprint(blueprint);
        String blueprintVersion = blueprint.getStackVersion();
        LOGGER.debug("Validating custom template. Permitted services for upgrade with minimum required blueprint version: {}, available services: {}",
                permittedServicesForUpgradeService.toString(), services);
        Set<String> notUpgradePermittedServices = getNotUpgradePermittedServices(services, blueprintVersion);
        return new BlueprintValidationResult(notUpgradePermittedServices.isEmpty(), createReason(notUpgradePermittedServices));
    }

    private Set<String> getNotUpgradePermittedServices(Set<String> services, String blueprintVersion) {
        return services.stream()
                .filter(service -> !permittedServicesForUpgradeService.isAllowedForUpgrade(service, blueprintVersion))
                .collect(Collectors.toSet());
    }

    private Set<String> getServicesFromBlueprint(Blueprint blueprint) {
        return cmTemplateProcessorFactory.get(blueprint.getBlueprintText())
                .getAllComponents()
                .stream()
                .map(ServiceComponent::getService)
                .collect(Collectors.toSet());
    }

    private String createReason(Set<String> notUpgradeableServices) {
        if (notUpgradeableServices.isEmpty()) {
            return null;
        } else {
            String reasonMessage = String.format("The following services are not eligible for upgrade in the cluster template: %s", notUpgradeableServices);
            LOGGER.debug(reasonMessage);
            return reasonMessage;
        }
    }
}
