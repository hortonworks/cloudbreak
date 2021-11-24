package com.sequenceiq.cloudbreak.service.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class FreeipaServiceTest {

    private static final String ENV_CRN =
            "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    @Mock
    private FreeipaClientService freeipaClientService;

    @InjectMocks
    private FreeipaService underTest;

    @ParameterizedTest
    @EnumSource(value = Status.class)
    void testCheckFreeipaRunningWhenFreeIpaStoppedThenReturnsFalse(Status status) {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(status);
        freeipa.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);

        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(freeipa);

        boolean freeipaRunning = underTest.checkFreeipaRunning(ENV_CRN);
        assertFalse(freeipaRunning);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class)
    void testCheckFreeipaRunningWhenFreeIpaUnknownThenThrowsException(Status status) {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(status);
        freeipa.setAvailabilityStatus(AvailabilityStatus.UNKNOWN);

        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(freeipa);

        CloudbreakServiceException exception = Assertions.assertThrows(CloudbreakServiceException.class, () -> underTest.checkFreeipaRunning(ENV_CRN));
        assertEquals("Freeipa availability cannot be determined currently.", exception.getMessage());
    }

    @Test
    void testCheckFreeipaRunningWhenFreeIpaIsNullThenThrowsException() {
        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(null);

        CloudbreakServiceException exception = Assertions.assertThrows(CloudbreakServiceException.class, () -> underTest.checkFreeipaRunning(ENV_CRN));
        assertEquals("Freeipa availability cannot be determined currently.", exception.getMessage());
    }

    @Test
    void testCheckFreeipaRunningWhenFreeIpaStatusIsNullThenThrowsException() {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(freeipa);

        CloudbreakServiceException exception = Assertions.assertThrows(CloudbreakServiceException.class, () -> underTest.checkFreeipaRunning(ENV_CRN));
        assertEquals("Freeipa availability cannot be determined currently.", exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class)
    void testCheckFreeipaRunningWhenFreeIpaAvailableThenPass(Status status) {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(status);
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(freeipa);

        Assertions.assertDoesNotThrow(() -> underTest.checkFreeipaRunning(ENV_CRN));
    }
}