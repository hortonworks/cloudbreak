package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.ENABLED;
import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.OS_UPGRADE_DISABLED;
import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.ROLLING_UPGRADE_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@ExtendWith(MockitoExtension.class)
class BlueprintUpgradeOptionConditionTest {

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final String OS_UPGRADE_ERROR_MESSAGE = "The OS upgrade is not enabled for this blueprint.";

    private static final String ROLLING_UPGRADE_ERROR_MESSAGE = "Rolling upgrade is not supported for this cluster. For details please see the rolling upgrade "
            + "documentation. To check whether rolling upgrade could be enabled for this cluster please reach out to Cloudera Support.";

    @InjectMocks
    private BlueprintUpgradeOptionCondition underTest;

    @Mock
    private EntitlementService entitlementService;

    private static Object[][] testScenariosProvider() {
        return new Object[][] {
                { ENABLED, false, false, false, true, null},
                { ENABLED, true, false, false, true, null },
                { OS_UPGRADE_DISABLED, true, false, false, false, OS_UPGRADE_ERROR_MESSAGE },
                { ENABLED, false, false, false, true, null},
                { ENABLED, false, true, true, true, null },
                { ENABLED, false, true, false, false, ROLLING_UPGRADE_ERROR_MESSAGE },
                { ROLLING_UPGRADE_ENABLED, false, true, false, true, null },
        };
    }

    @ParameterizedTest(name = "BlueprintUpgradeOption: {0}, osUpgrade: {1}, skipValidations: {2}, dataHubUpgradeEntitled: {3}, expected: {4}, "
            + "expected message: {5}")
    @MethodSource("testScenariosProvider")
    public void test(BlueprintUpgradeOption blueprintUpgradeOption, boolean osUpgrade, boolean rollingUpgrade, boolean skipRollingUpgradeValidation,
            boolean expectedValue, String expectedErrorMessage) {
        ImageFilterParams imageFilterParams = createImageFilterParams(osUpgrade, rollingUpgrade);
        lenient().when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(skipRollingUpgradeValidation);

        BlueprintValidationResult actual = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.validate(imageFilterParams, blueprintUpgradeOption));
        assertEquals(expectedValue, actual.isValid());
        assertEquals(expectedErrorMessage, actual.getReason());
    }

    private ImageFilterParams createImageFilterParams(boolean osUpgrade, boolean rollingUpgrade) {
        return new ImageFilterParams(null, null, null, osUpgrade, null, null, null, null,
                new InternalUpgradeSettings(false, false, rollingUpgrade), null, null, null, false);
    }

}