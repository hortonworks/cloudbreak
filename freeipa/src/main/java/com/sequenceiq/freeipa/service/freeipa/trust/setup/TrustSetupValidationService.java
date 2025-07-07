package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker.IPA_SERVER_TRUST_AD_PACKAGE;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker;

@Service
public class TrustSetupValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustSetupValidationService.class);

    @Inject
    private IpaTrustAdPackageAvailabilityChecker packageAvailabilityChecker;

    public void validateTrustSetup(Long stackId) {
        validatePackageAvailability(stackId);
    }

    private void validatePackageAvailability(Long stackId) {
        if (!packageAvailabilityChecker.isPackageAvailable(stackId)) {
            LOGGER.warn("Missing package [{}] required for AD trust setup", IPA_SERVER_TRUST_AD_PACKAGE);
            throw new CloudbreakServiceException(IPA_SERVER_TRUST_AD_PACKAGE
                    + " package is required for AD trust setup. Please upgrade to the latest image of FreeIPA.");
        }
    }
}
