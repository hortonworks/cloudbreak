package com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation;

import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_FQDN;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_REALM;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY_ON_ERROR;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SETUP_PILLAR;

import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class TrustStatusValidationService {

    protected static final String TRUST_STATUS_VALIDATION_STATE = "trustsetup/validation/trust_status_validation";

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustStatusValidationService.class);

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private StackService stackService;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public ValidationResult validateTrustStatus(Stack stack, CrossRealmTrust crossRealmTrust) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        validateTrustStatus(stack, crossRealmTrust, validationResultBuilder);
        return validationResultBuilder.build();
    }

    private void validateTrustStatus(Stack stack, CrossRealmTrust crossRealmTrust, ValidationResult.ValidationResultBuilder validationResultBuilder) {
        try {
            OrchestratorStateParams stateParams =
                    saltStateParamsService.createStateParams(stack, TRUST_STATUS_VALIDATION_STATE, true, MAX_RETRY, MAX_RETRY_ON_ERROR);
            stateParams.setStateParams(Map.of(FREEIPA, Map.of(TRUST_SETUP_PILLAR, Map.of(
                    KDC_FQDN, crossRealmTrust.getKdcFqdn(),
                    KDC_REALM, crossRealmTrust.getKdcRealm().toUpperCase(Locale.ROOT)))));
            hostOrchestrator.runOrchestratorState(stateParams);
        } catch (Exception ex) {
            LOGGER.warn("Trust status validation failed for AD: {}", crossRealmTrust.getKdcFqdn());
            validationResultBuilder.warning(ex.getMessage());
        }
    }
}
