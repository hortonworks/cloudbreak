package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@Component
class CustomTemplateUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTemplateUpgradeValidator.class);

    @Value("${cb.upgrade.requiredServices:}")
    private Set<String> requiredServices;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    boolean isValid(Blueprint blueprint) {
        if (isDatahubRuntimeUpgradeEnabledForCustomTemplate()) {
            LOGGER.debug("Skipping custom template validation because the entitlement is turned on.");
            return true;
        } else {
            Set<String> services = getServiceFromBlueprint(blueprint);
            LOGGER.debug("Validating custom template. Required services for upgrade: {}, available services: {}", requiredServices, services);
            return requiredServices.containsAll(services);
        }
    }

    private boolean isDatahubRuntimeUpgradeEnabledForCustomTemplate() {
        return entitlementService.datahubRuntimeUpgradeEnabledForCustomTemplate(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn())
                .getAccountId());
    }

    private Set<String> getServiceFromBlueprint(Blueprint blueprint) {
        return cmTemplateProcessorFactory.get(blueprint.getBlueprintText())
                .getAllComponents()
                .stream()
                .map(ServiceComponent::getService)
                .collect(Collectors.toSet());
    }
}
