package com.sequenceiq.datalake.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import javax.ws.rs.ServiceUnavailableException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class FreeipaServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    @Mock
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @InjectMocks
    private FreeipaService underTest;

    @Test
    void testCheckFreeipaRunningWhenFreeIpaStoppedThenThrowsException() {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(Status.STOPPED);
        freeipa.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);

        when(underTest.describe(ENV_CRN)).thenReturn(freeipa);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.checkFreeipaRunning(ENV_CRN));
        assertEquals("Freeipa should be in Available state but currently is " + Status.STOPPED, exception.getMessage());
    }

    @Test
    void testCheckFreeipaRunningWhenFreeIpaIsNullThenThrowsException() {
        when(underTest.describe(ENV_CRN)).thenReturn(null);

        ServiceUnavailableException exception = Assertions.assertThrows(ServiceUnavailableException.class, () -> underTest.checkFreeipaRunning(ENV_CRN));
        assertEquals("Freeipa availability cannot be determined currently.", exception.getMessage());
    }

    @Test
    void testCheckFreeipaRunningWhenFreeIpaStatusIsNullThenThrowsException() {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        when(underTest.describe(ENV_CRN)).thenReturn(freeipa);

        ServiceUnavailableException exception = Assertions.assertThrows(ServiceUnavailableException.class, () -> underTest.checkFreeipaRunning(ENV_CRN));
        assertEquals("Freeipa availability cannot be determined currently.", exception.getMessage());
    }

    @Test
    void testCheckFreeipaRunningWhenFreeIpaAvailableThenPass() {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(Status.AVAILABLE);
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        when(underTest.describe(ENV_CRN)).thenReturn(freeipa);

        Assertions.assertDoesNotThrow(() -> underTest.checkFreeipaRunning(ENV_CRN));
    }
}