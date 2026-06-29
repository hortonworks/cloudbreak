package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class UpgradePathRestrictionServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @InjectMocks
    private UpgradePathRestrictionService underTest;

    @Mock
    private EntitlementService entitlementService;

    @BeforeEach
    void setUp() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("definitions/upgrade-path-restrictions.json")) {
            List<BlockedUpgradePath> blockedPaths = new ObjectMapper().readValue(is, new TypeReference<>() { });
            ReflectionTestUtils.setField(underTest, "blockedUpgradePaths", blockedPaths);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load upgrade-path-restrictions.json", e);
        }
    }

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
                Arguments.of("7.2.17", 1100, "7.2.17", 1200, true),

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
                Arguments.of("7.2.18", 1100, "7.3.1", 500, true),
                Arguments.of("7.2.18", 1200, "7.3.2", 0, false),
                Arguments.of("7.3.1", 800, "7.3.2", 0, false),
                Arguments.of("7.3.1", 900, "7.3.2", 0, false),
                Arguments.of("7.3.1", 0, "7.3.2", 0, true)

        );
    }

    @ParameterizedTest(name = "[{index}] Upgrade from {0}.{1} to {2}.{3}, should be allowed: {4}")
    @MethodSource("provideTestParametersFor7218P1100")
    public void testIsUpgradePermittedFor7218P1100WhenTheMitigateReleaseFailure7218P1100EntitlementEnabled(
            String currentVersion, Integer currentPatchVersion, String candidateVersion, Integer candidatePatchVersion, boolean expectedResult) {
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
                Arguments.of("7.2.17", 1100, "7.2.17", 1200, true),
                Arguments.of("7.2.18", 1100, "7.2.18", 1100, true),
                Arguments.of("7.2.18", 1100, "7.2.18", 1101, false),
                Arguments.of("7.2.18", 1100, "7.3.1", 0, false),
                Arguments.of("7.2.18", 1100, "7.3.1", 400, false),
                Arguments.of("7.2.18", 1100, "7.3.1", 500, true)

        );
    }

    @ParameterizedTest(name = "[{index}] Upgrade from {0}.{1} to {2}.{3}, should be allowed: {4}")
    @MethodSource("provideTestParametersForInternalAccount")
    public void testPermitUpgradeForInternalAccountBypassesSourceRangeRestrictions(String currentVersion, Integer currentPatchVersion,
            String candidateVersion, Integer candidatePatchVersion, boolean expectedResult) {
        VersionComparisonContext currentVersionContext = createVersionComparisonContext(currentVersion, currentPatchVersion);
        VersionComparisonContext candidateVersionContext = createVersionComparisonContext(candidateVersion, candidatePatchVersion);
        lenient().when(entitlementService.internalTenant(any())).thenReturn(true);
        lenient().when(entitlementService.isMitigateReleaseFailure7218P1100Enabled(any())).thenReturn(false);

        assertEquals(expectedResult, doAs(USER_CRN, () -> underTest.permitUpgrade(currentVersionContext, candidateVersionContext)));
    }

    private static Stream<Arguments> provideTestParametersForInternalAccount() {
        return Stream.of(
                // these would be blocked for regular accounts but internal accounts bypass source-range restrictions on 7.3.1 targets
                Arguments.of("7.2.16", 0,   "7.3.1", 0,   true),
                Arguments.of("7.2.17", 100, "7.3.1", 0,   true),
                Arguments.of("7.2.17", 600, "7.3.1", 0,   true),
                Arguments.of("7.2.18", 300, "7.3.1", 0,   true),
                // rules without internal_account_override are still enforced for internal accounts
                Arguments.of("7.2.18", 1100, "7.2.18", 1101, false),
                Arguments.of("7.3.1",  800,  "7.3.2",  0,    false),
                Arguments.of("7.2.18", 1200, "7.3.2",  0,    false)
        );
    }

    private VersionComparisonContext createVersionComparisonContext(String majorVersion, Integer patchVersion) {
        return new VersionComparisonContext.Builder()
                .withMajorVersion(majorVersion)
                .withPatchVersion(patchVersion)
                .build();
    }

}
