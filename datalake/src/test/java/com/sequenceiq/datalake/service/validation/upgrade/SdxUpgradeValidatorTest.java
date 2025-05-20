package com.sequenceiq.datalake.service.validation.upgrade;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ClouderaManagerLicenseProvider;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.PaywallAccessChecker;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

@ExtendWith(MockitoExtension.class)
class SdxUpgradeValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ACCOUNT_ID = Crn.fromString(USER_CRN).getAccountId();

    @InjectMocks
    private SdxUpgradeValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private PaywallAccessChecker paywallAccessChecker;

    @Mock
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    @ParameterizedTest(name = "[{index}] Rolling upgrade enabled: {0} Cluster shape: {1} skipRollingUpgradeValidationEnabled: {2} "
            + "should throw validation exception: {3}")
    @MethodSource("provideTestParameters")
    void testValidateRollingUpgradeByClusterShape(boolean rollingUpgradeEnabled, SdxClusterShape clusterShape, boolean skipRollingUpgradeValidationEnabled,
            boolean shouldThrowValidationException) {

        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRollingUpgradeEnabled(rollingUpgradeEnabled);
        lenient().when(entitlementService.isSkipRollingUpgradeValidationEnabled(ACCOUNT_ID)).thenReturn(skipRollingUpgradeValidationEnabled);

        if (shouldThrowValidationException) {
            assertThrows(BadRequestException.class, () -> doAs(USER_CRN, () -> underTest.validateRollingUpgradeByClusterShape(request, clusterShape)));
        } else {
            assertDoesNotThrow(() -> doAs(USER_CRN, () -> underTest.validateRollingUpgradeByClusterShape(request, clusterShape)));
        }
    }

    private static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of(false, SdxClusterShape.LIGHT_DUTY,     false, false),
                Arguments.of(true,  SdxClusterShape.LIGHT_DUTY,     false, true),
                Arguments.of(true,  SdxClusterShape.CUSTOM,         false, true),
                Arguments.of(true,  SdxClusterShape.MEDIUM_DUTY_HA, false, true),
                Arguments.of(true,  SdxClusterShape.ENTERPRISE,     false, false),
                Arguments.of(true,  SdxClusterShape.MICRO_DUTY,     false, true),
                Arguments.of(true,  SdxClusterShape.CONTAINERIZED,  false, true),

                Arguments.of(true,  SdxClusterShape.LIGHT_DUTY,     true, true),
                Arguments.of(true,  SdxClusterShape.CUSTOM,         true, false),
                Arguments.of(true,  SdxClusterShape.MEDIUM_DUTY_HA, true, false),
                Arguments.of(true,  SdxClusterShape.ENTERPRISE,     true, false),
                Arguments.of(true,  SdxClusterShape.MICRO_DUTY,     true, true),
                Arguments.of(true,  SdxClusterShape.CONTAINERIZED,  true, true)

        );
    }

    @Test
    void testVerifyPaywallAccessWhenTheUpgradeRequestIsNull() {
        underTest.verifyPaywallAccess(USER_CRN, null);
        verifyNoInteractions(entitlementService, clouderaManagerLicenseProvider, paywallAccessChecker);
    }

    @Test
    void testVerifyPaywallAccessWhenTheLockComponentsIsTrue() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setLockComponents(true);
        underTest.verifyPaywallAccess(USER_CRN, sdxUpgradeRequest);
        verifyNoInteractions(entitlementService, clouderaManagerLicenseProvider, paywallAccessChecker);
    }

    @Test
    void testVerifyPaywallAccessWhenTheLockComponentsIsFalseAndInternalRepositoryEnabled() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setLockComponents(false);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(true);
        underTest.verifyPaywallAccess(USER_CRN, sdxUpgradeRequest);
        verifyNoInteractions(clouderaManagerLicenseProvider, paywallAccessChecker);
    }

    @Test
    void testVerifyPaywallAccessWhenTheLockComponentsIsFalseAndInternalRepositoryDisabled() {
        String paywallUrl = "https://archive.coudera.com/p/cdp-public/";
        JsonCMLicense license = new JsonCMLicense();
        ReflectionTestUtils.setField(underTest, "paywallUrl", paywallUrl);
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setLockComponents(false);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(false);
        when(clouderaManagerLicenseProvider.getLicense(USER_CRN)).thenReturn(license);

        underTest.verifyPaywallAccess(USER_CRN, sdxUpgradeRequest);

        verify(paywallAccessChecker).checkPaywallAccess(license, paywallUrl);
    }
}