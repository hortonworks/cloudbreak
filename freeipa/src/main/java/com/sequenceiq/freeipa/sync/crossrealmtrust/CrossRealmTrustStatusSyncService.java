package com.sequenceiq.freeipa.sync.crossrealmtrust;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation.TrustStatusValidationService;

@Service
public class CrossRealmTrustStatusSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrossRealmTrustStatusSyncService.class);

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private TrustStatusValidationService trustStatusValidationService;

    public void syncCrossRealmTrustStatus(Stack stack, CrossRealmTrust crossRealmTrust) {
        TrustStatus trustStatus;
        ValidationResult validationResult = trustStatusValidationService.validateTrustStatus(stack, crossRealmTrust);
        if (validationResult.hasError()) {
            LOGGER.warn("Cross realm trust status validation failed: {}", validationResult.getFormattedErrors());
            trustStatus = TrustStatus.TRUST_BROKEN;
        } else {
            LOGGER.info("Cross realm trust status validation passed");
            trustStatus = TrustStatus.TRUST_ACTIVE;
        }
        crossRealmTrustService.updateTrustStateByStackId(stack.getId(), trustStatus);
    }

}
