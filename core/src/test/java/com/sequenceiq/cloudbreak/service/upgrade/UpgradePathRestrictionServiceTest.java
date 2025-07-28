package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class UpgradePathRestrictionServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @InjectMocks
    private UpgradePathRestrictionService underTest;

    @Mock
    private EntitlementService entitlementService;

    @ParameterizedTest(name = "[{index}] Upgrade from {0}.{1} to {2}.{3}, should be allowed: {4}")
    @MethodSource("provideTestParameters")
    public void testPermitUpgrade(String currentVersion, Integer currentPatchVersion,
            String candidateVersion, Integer candidatePatchVersion, boolean expectedResult) {
        VersionComparisonContext currentVersionContext = createVersionComparisonContext(currentVersion, currentPatchVersion);
        VersionComparisonContext candidateVersionContext = createVersionComparisonContext(candidateVersion, candidatePatchVersion);
        lenient().when(entitlementService.internalTenant(any())).thenReturn(false);
        lenient().when(entitlementService.isMitigateReleaseFailure7218P1100Enabled(any())).thenReturn(false);

        assertEquals(expectedResult, doAs(USER_CRN, () -> underTest.permitUpgrade(currentVersionContext, candidateVersionContext)));
    }

    private static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of("7.2.16", 0, "7.3.1", 0, false),
                Arguments.of("7.2.16", 0, "7.3.1", 100, false),
                Arguments.of("7.2.16", 0, "7.3.1", 200, false),
                Arguments.of("7.2.16", 0, "7.3.1", 300, false),

                Arguments.of("7.2.17", 100, "7.3.1", 0, false),
                Arguments.of("7.2.17", 100, "7.3.1", 100, false),
                Arguments.of("7.2.17", 100, "7.3.1", 200, false),
                Arguments.of("7.2.17", 100, "7.3.1", 300, false),

                Arguments.of("7.2.17", 200, "7.3.1", 0, true),
                Arguments.of("7.2.17", 200, "7.3.1", 100, true),
                Arguments.of("7.2.17", 200, "7.3.1", 200, true),
                Arguments.of("7.2.17", 200, "7.3.1", 300, true),

                Arguments.of("7.2.17", 300, "7.3.1", 0, true),
                Arguments.of("7.2.17", 300, "7.3.1", 100, true),
                Arguments.of("7.2.17", 300, "7.3.1", 200, true),
                Arguments.of("7.2.17", 300, "7.3.1", 300, true),

                Arguments.of("7.2.17", 500, "7.3.1", 0, true),
                Arguments.of("7.2.17", 500, "7.3.1", 100, true),
                Arguments.of("7.2.17", 500, "7.3.1", 200, true),
                Arguments.of("7.2.17", 500, "7.3.1", 300, true),

                Arguments.of("7.2.17", 600, "7.3.1", 0, false),
                Arguments.of("7.2.17", 600, "7.3.1", 100, false),
                Arguments.of("7.2.17", 600, "7.3.1", 200, true),
                Arguments.of("7.2.17", 600, "7.3.1", 300, true),

                Arguments.of("7.2.18", 0, "7.3.1", 0, true),
                Arguments.of("7.2.18", 0, "7.3.1", 100, true),
                Arguments.of("7.2.18", 0, "7.3.1", 200, true),
                Arguments.of("7.2.18", 0, "7.3.1", 300, true),

                Arguments.of("7.2.18", 200, "7.3.1", 0, true),
                Arguments.of("7.2.18", 200, "7.3.1", 100, true),
                Arguments.of("7.2.18", 200, "7.3.1", 200, true),
                Arguments.of("7.2.18", 200, "7.3.1", 300, true),

                Arguments.of("7.2.18", 300, "7.3.1", 0, false),
                Arguments.of("7.2.18", 300, "7.3.1", 100, false),
                Arguments.of("7.2.18", 300, "7.3.1", 200, true),
                Arguments.of("7.2.18", 300, "7.3.1", 300, true),

                Arguments.of("7.2.17", 100, "7.3.2", 0, true),
                Arguments.of("7.3.1", 0, "7.3.1", 100, true),

                Arguments.of("7.2.16", 0, "7.2.18", 1100, false),
                Arguments.of("7.2.17", 0, "7.2.18", 1100, false),
                Arguments.of("7.2.18", 1100, "7.2.18", 1101, false),
                Arguments.of("7.2.18", 1100, "7.3.1", 0, false),
                Arguments.of("7.2.18", 1100, "7.3.1", 400, false),
                Arguments.of("7.2.18", 1100, "7.3.1", 500, true)

        );
    }

    @ParameterizedTest(name = "[{index}] Upgrade from {0}.{1} to {2}.{3}, should be allowed: {4}")
    @MethodSource("provideTestParametersFor7218P1100")
    public void testPermitUpgradeFor7218P1100WhenTheMitigateReleaseFailure7218P1100EntitlementEnabled(String currentVersion, Integer currentPatchVersion,
            String candidateVersion, Integer candidatePatchVersion, boolean expectedResult) {
        VersionComparisonContext currentVersionContext = createVersionComparisonContext(currentVersion, currentPatchVersion);
        VersionComparisonContext candidateVersionContext = createVersionComparisonContext(candidateVersion, candidatePatchVersion);
        lenient().when(entitlementService.internalTenant(any())).thenReturn(false);
        lenient().when(entitlementService.isMitigateReleaseFailure7218P1100Enabled(any())).thenReturn(true);

        assertEquals(expectedResult, doAs(USER_CRN, () -> underTest.permitUpgrade(currentVersionContext, candidateVersionContext)));
    }

    private static Stream<Arguments> provideTestParametersFor7218P1100() {
        return Stream.of(
                Arguments.of("7.2.16", 1100, "7.2.18", 1100, true),
                Arguments.of("7.2.16", 0, "7.2.18", 1100, true),
                Arguments.of("7.2.17", 0, "7.2.18", 1100, true),
                Arguments.of("7.2.18", 1100, "7.2.18", 1100, true),
                Arguments.of("7.2.18", 1100, "7.2.18", 1101, false),
                Arguments.of("7.2.18", 1100, "7.3.1", 0, false),
                Arguments.of("7.2.18", 1100, "7.3.1", 400, false),
                Arguments.of("7.2.18", 1100, "7.3.1", 500, true)

        );
    }

    private VersionComparisonContext createVersionComparisonContext(String majorVersion, Integer patchVersion) {
        return new VersionComparisonContext.Builder()
                .withMajorVersion(majorVersion)
                .withPatchVersion(patchVersion)
                .build();
    }

}