package com.sequenceiq.environment.environment.sync;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
public class EnvironmentSyncServiceTest {

    private final FreeIpaService freeIpaService = mock(FreeIpaService.class);

    private final EnvironmentSyncService underTest = new EnvironmentSyncService(freeIpaService);

    @ParameterizedTest(name = "{0}")
    @MethodSource("getStatusByFreeipaParams")
    void testGetStatusByFreeipa(String testName, DescribeFreeIpaResponse freeIpaResponse, EnvironmentStatus expected) {
        Environment environment = new Environment();
        environment.setResourceCrn("crn");

        when(freeIpaService.describe(environment.getResourceCrn())).thenReturn(Optional.of(freeIpaResponse));

        EnvironmentStatus actual = underTest.getStatusByFreeipa(environment);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testGetStatusByFreeipaWhenFreeipaAttachedButNotFound() {
        Environment environment = new Environment();
        environment.setResourceCrn("crn");
        environment.setCreateFreeIpa(true);

        when(freeIpaService.describe(environment.getResourceCrn())).thenReturn(Optional.empty());

        EnvironmentStatus actual = underTest.getStatusByFreeipa(environment);
        Assertions.assertEquals(EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE, actual);
    }

    @Test
    void testGetStatusByFreeipaWhenFreeipaNotAttached() {
        Environment environment = new Environment();
        environment.setResourceCrn("crn");
        environment.setCreateFreeIpa(false);

        when(freeIpaService.describe(environment.getResourceCrn())).thenReturn(Optional.empty());

        EnvironmentStatus actual = underTest.getStatusByFreeipa(environment);
        Assertions.assertEquals(EnvironmentStatus.AVAILABLE, actual);
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] getStatusByFreeipaParams() {
        return new Object[][]{
                // testCaseName                     freeipa status                                          expected env status
                {"FreeIPA is available",            getFreeipaResponse(Status.AVAILABLE),                   EnvironmentStatus.AVAILABLE},
                {"FreeIPA is stopped",              getFreeipaResponse(Status.STOPPED),                     EnvironmentStatus.ENV_STOPPED},
                {"FreeIPA is deleted on provider",  getFreeipaResponse(Status.DELETED_ON_PROVIDER_SIDE),    EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE},
                {"FreeIPA is stop failed",          getFreeipaResponse(Status.STOP_FAILED),                 EnvironmentStatus.STOP_FREEIPA_FAILED},
                {"FreeIPA is start failed",         getFreeipaResponse(Status.START_FAILED),                EnvironmentStatus.START_FREEIPA_FAILED},
                {"FreeIPA is start in progress",    getFreeipaResponse(Status.START_IN_PROGRESS),           EnvironmentStatus.START_FREEIPA_STARTED},
                {"FreeIPA is stop in progress",     getFreeipaResponse(Status.STOP_IN_PROGRESS),            EnvironmentStatus.STOP_FREEIPA_STARTED}
        };
    }

    private static DescribeFreeIpaResponse getFreeipaResponse(Status status) {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        freeIpaResponse.setStatus(status);
        return freeIpaResponse;
    }
    // CHECKSTYLE:ON
    // @formatter:on
}
