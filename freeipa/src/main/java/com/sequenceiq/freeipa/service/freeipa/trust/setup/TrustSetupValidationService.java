package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.AD_DOMAIN;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.AD_IP;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SETUP_PILLAR;
import static com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker.IPA_SERVER_TRUST_AD_PACKAGE;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker;

@Service
public class TrustSetupValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrustSetupValidationService.class);

    private static final String AD_DNS_VALIDATION_STATE = "trustsetup/validation/ad_dns_validation";

    private static final int MAX_RETRY = 5;

    private static final int MAX_RETRY_ON_ERROR = 3;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private IpaTrustAdPackageAvailabilityChecker packageAvailabilityChecker;

    @Inject
    private StackService stackService;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public ValidationResult validateTrustSetup(Long stackId) {
        Optional<CrossRealmTrust> crossRealmTrust = crossRealmTrustService.getByStackIdIfExists(stackId);
        return crossRealmTrust
                .map(cr -> validateTrustSetup(stackId, cr))
                .orElse(ValidationResult.builder().error("No cross realm information is provided").build());
    }

    private ValidationResult validateTrustSetup(Long stackId, CrossRealmTrust crossRealmTrust) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        validatePackageAvailability(stackId, validationResultBuilder);
        validateDns(stack, crossRealmTrust, validationResultBuilder);
        return validationResultBuilder.build();
    }

    private void validatePackageAvailability(Long stackId, ValidationResultBuilder validationResultBuilder) {
        LOGGER.info("Validate for IPA Server image trust packages");
        if (!packageAvailabilityChecker.isPackageAvailable(stackId)) {
            LOGGER.warn("Missing package [{}] required for AD trust setup", IPA_SERVER_TRUST_AD_PACKAGE);
            validationResultBuilder.error(IPA_SERVER_TRUST_AD_PACKAGE
                    + " package is required for AD trust setup. Please upgrade to the latest image of FreeIPA.");
        }
    }

    private void validateDns(Stack stack, CrossRealmTrust crossRealmTrust, ValidationResultBuilder validationResultBuilder) {
        try {
            OrchestratorStateParams stateParams = saltStateParamsService.createStateParams(stack, AD_DNS_VALIDATION_STATE, true, MAX_RETRY, MAX_RETRY_ON_ERROR);
            stateParams.setStateParams(Map.of(FREEIPA, Map.of(TRUST_SETUP_PILLAR, Map.of(
                    AD_DOMAIN, crossRealmTrust.getFqdn(),
                    AD_IP, crossRealmTrust.getIp()))));
            hostOrchestrator.runOrchestratorState(stateParams);
        } catch (Exception ex) {
            LOGGER.error("Dns validation failed on AD: {}", crossRealmTrust.getFqdn(), ex);
            validationResultBuilder.error(ex.getMessage());
        }
    }
}
