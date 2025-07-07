package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker;

@ExtendWith(MockitoExtension.class)
class TrustSetupValidationServiceTest {

    @Mock
    private IpaTrustAdPackageAvailabilityChecker packageAvailabilityChecker;

    @InjectMocks
    private TrustSetupValidationService underTest;

    @Test
    void testMissingPackage() {
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.validateTrustSetup(4L));
        assertEquals("ipa-server-trust-ad package is required for AD trust setup. Please upgrade to the latest image of FreeIPA.",
                exception.getMessage());
    }
}