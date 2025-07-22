package com.sequenceiq.environment.environment.scheduled.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;

@ExtendWith(MockitoExtension.class)
public class EnvironmentSyncServiceTest {

    private final FreeIpaService freeIpaService = mock(FreeIpaService.class);

    private final EnvironmentSyncService underTest = new EnvironmentSyncService(freeIpaService);

    @ParameterizedTest(name = "{0}")
    @MethodSource("getStatusByFreeipaParams")
    void testGetStatusByFreeipa(
            String testName,
            DescribeFreeIpaResponse freeIpaResponse,
            EnvironmentStatus expected,
            EnvironmentType environmentType
    ) {
        Environment environment = new Environment();
        environment.setAccountId("cloudera");
        environment.setResourceCrn("crn");
        environment.setEnvironmentType(environmentType);
        environment.setStatus(expected);

        when(freeIpaService.internalDescribe(environment.getResourceCrn(), "cloudera")).thenReturn(Optional.of(freeIpaResponse));

        EnvironmentStatus actual = underTest.getStatusByFreeipa(environment);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testGetStatusByFreeipaWhenFreeipaAttachedButNotFound() {
        Environment environment = new Environment();
        environment.setResourceCrn("crn");
        environment.setCreateFreeIpa(true);

        when(freeIpaService.internalDescribe(environment.getResourceCrn(), "cloudera")).thenReturn(Optional.empty());

        EnvironmentStatus actual = underTest.getStatusByFreeipa(environment);
        Assertions.assertEquals(EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE, actual);
    }

    @Test
    void testGetStatusByFreeipaWhenFreeipaNotAttached() {
        Environment environment = new Environment();
        environment.setResourceCrn("crn");
        environment.setCreateFreeIpa(false);

        when(freeIpaService.internalDescribe(environment.getResourceCrn(), "cloudera")).thenReturn(Optional.empty());

        EnvironmentStatus actual = underTest.getStatusByFreeipa(environment);
        Assertions.assertEquals(EnvironmentStatus.AVAILABLE, actual);
    }

    @Test
    void allFreeIpaStatusesMapped() {
        Set<Status> testedSet = underTest.getStatusMap().keySet();
        assertThat(testedSet).hasSameElementsAs(Arrays.asList(Status.values()));
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] getStatusByFreeipaParams() {
        return new Object[][]{
                {
                        "FreeIPA is available",
                        getFreeipaResponse(Status.AVAILABLE),
                        EnvironmentStatus.AVAILABLE,
                        EnvironmentType.PUBLIC_CLOUD},
                {
                        "FreeIPA is stopped",
                        getFreeipaResponse(Status.STOPPED),
                        EnvironmentStatus.ENV_STOPPED,
                        EnvironmentType.PUBLIC_CLOUD
                },
                {
                        "FreeIPA is deleted on provider",
                        getFreeipaResponse(Status.DELETED_ON_PROVIDER_SIDE),
                        EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE,
                        EnvironmentType.PUBLIC_CLOUD
                },
                {
                        "FreeIPA is stop failed",
                        getFreeipaResponse(Status.STOP_FAILED),
                        EnvironmentStatus.STOP_FREEIPA_FAILED,
                        EnvironmentType.PUBLIC_CLOUD
                },
                {
                        "FreeIPA is start failed",
                        getFreeipaResponse(Status.START_FAILED),
                        EnvironmentStatus.START_FREEIPA_FAILED,
                        EnvironmentType.PUBLIC_CLOUD
                },
                {
                        "FreeIPA is start in progress",
                        getFreeipaResponse(Status.START_IN_PROGRESS),
                        EnvironmentStatus.START_FREEIPA_STARTED,
                        EnvironmentType.PUBLIC_CLOUD
                },
                {
                        "FreeIPA is stop in progress",
                        getFreeipaResponse(Status.STOP_IN_PROGRESS),
                        EnvironmentStatus.STOP_FREEIPA_STARTED,
                        EnvironmentType.PUBLIC_CLOUD
                },
                {
                        "FreeIPA is available",
                        getFreeipaResponse(Status.AVAILABLE),
                        EnvironmentStatus.AVAILABLE,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is stopped",
                        getFreeipaResponse(Status.STOPPED),
                        EnvironmentStatus.ENV_STOPPED,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is deleted on provider",
                        getFreeipaResponse(Status.DELETED_ON_PROVIDER_SIDE),
                        EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is stop failed",
                        getFreeipaResponse(Status.STOP_FAILED),
                        EnvironmentStatus.STOP_FREEIPA_FAILED,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is start failed",
                        getFreeipaResponse(Status.START_FAILED),
                        EnvironmentStatus.START_FREEIPA_FAILED,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is start in progress",
                        getFreeipaResponse(Status.START_IN_PROGRESS),
                        EnvironmentStatus.START_FREEIPA_STARTED,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is stop in progress",
                        getFreeipaResponse(Status.STOP_IN_PROGRESS),
                        EnvironmentStatus.STOP_FREEIPA_STARTED,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is AVAILABLE and Trust status UNKNOWN should be UNKNOWN",
                        getFreeipaResponse(Status.AVAILABLE, TrustStatus.UNKNOWN),
                        EnvironmentStatus.TRUST_SETUP_REQUIRED,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is AVAILABLE and Trust status TRUST_ACTIVE should be TRUST_ACTIVE",
                        getFreeipaResponse(Status.AVAILABLE, TrustStatus.TRUST_ACTIVE),
                        EnvironmentStatus.AVAILABLE,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is AVAILABLE and Trust status TRUST_BROKEN should be TRUST_BROKEN",
                        getFreeipaResponse(Status.AVAILABLE, TrustStatus.TRUST_BROKEN),
                        EnvironmentStatus.TRUST_BROKEN,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is AVAILABLE and Trust status TRUST_SETUP_REQUIRED should be TRUST_SETUP_REQUIRED",
                        getFreeipaResponse(Status.AVAILABLE, TrustStatus.TRUST_SETUP_REQUIRED),
                        EnvironmentStatus.TRUST_SETUP_REQUIRED,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is AVAILABLE and Trust status TRUST_SETUP_IN_PROGRESS should be TRUST_SETUP_IN_PROGRESS",
                        getFreeipaResponse(Status.AVAILABLE, TrustStatus.TRUST_SETUP_IN_PROGRESS),
                        EnvironmentStatus.TRUST_SETUP_IN_PROGRESS,
                        EnvironmentType.HYBRID
                },
                {
                        "FreeIPA is AVAILABLE and Trust status TRUST_SETUP_FAILED should be TRUST_SETUP_FAILED",
                        getFreeipaResponse(Status.AVAILABLE, TrustStatus.TRUST_SETUP_FAILED),
                        EnvironmentStatus.TRUST_SETUP_FAILED,
                        EnvironmentType.HYBRID},
                {
                        "FreeIPA is AVAILABLE and Trust status TRUST_SETUP_FINISH_REQUIRED should be TRUST_SETUP_FINISH_REQUIRED",
                        getFreeipaResponse(Status.AVAILABLE, TrustStatus.TRUST_SETUP_FINISH_REQUIRED),
                        EnvironmentStatus.TRUST_SETUP_FINISH_REQUIRED,
                        EnvironmentType.HYBRID},
                {
                        "FreeIPA is AVAILABLE and Trust status TRUST_SETUP_FINISH_FAILED should be TRUST_SETUP_FINISH_FAILED",
                        getFreeipaResponse(Status.AVAILABLE, TrustStatus.TRUST_SETUP_FINISH_FAILED),
                        EnvironmentStatus.TRUST_SETUP_FINISH_FAILED,
                        EnvironmentType.HYBRID},
                {
                        "FreeIPA is AVAILABLE and Trust status TRUST_SETUP_FINISH_IN_PROGRESS should be TRUST_SETUP_FINISH_IN_PROGRESS",
                        getFreeipaResponse(Status.AVAILABLE, TrustStatus.TRUST_SETUP_FINISH_IN_PROGRESS),
                        EnvironmentStatus.TRUST_SETUP_FINISH_IN_PROGRESS,
                        EnvironmentType.HYBRID
                },
        };
    }

    private static DescribeFreeIpaResponse getFreeipaResponse(Status status) {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        freeIpaResponse.setStatus(status);
        return freeIpaResponse;
    }

    private static DescribeFreeIpaResponse getFreeipaResponse(Status status, TrustStatus trustStatus) {
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        freeIpaResponse.setStatus(status);
        TrustResponse trustResponse = new TrustResponse();
        trustResponse.setTrustStatus(trustStatus.name());
        freeIpaResponse.setTrust(trustResponse);
        return freeIpaResponse;
    }
    // CHECKSTYLE:ON
    // @formatter:on
}
