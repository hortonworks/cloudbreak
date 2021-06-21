package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@Component
class CustomTemplateUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTemplateUpgradeValidator.class);

    @Value("${cb.upgrade.permittedServicesForUpgrade}")
    private Set<String> permittedServicesForUpgrade;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    BlueprintValidationResult isValid(Blueprint blueprint) {
        if (isDatahubRuntimeUpgradeEnabledForCustomTemplate()) {
            LOGGER.debug("Skipping custom template validation because the entitlement is turned on.");
            return new BlueprintValidationResult(true);
        } else {
            Set<String> services = getServicesFromBlueprint(blueprint);
            LOGGER.debug("Validating custom template. Permitted services for upgrade: {}, available services: {}", permittedServicesForUpgrade, services);
            Set<String> notUpgradePermittedServices = getNotUpgradePermittedServices(services);
            return new BlueprintValidationResult(notUpgradePermittedServices.isEmpty(), createReason(notUpgradePermittedServices));
        }
    }

    private Set<String> getNotUpgradePermittedServices(Set<String> services) {
        return services.stream()
                .filter(service -> !permittedServicesForUpgrade.contains(service))
                .collect(Collectors.toSet());
    }

    private boolean isDatahubRuntimeUpgradeEnabledForCustomTemplate() {
        return entitlementService.datahubRuntimeUpgradeEnabledForCustomTemplate(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()).getAccountId());
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
