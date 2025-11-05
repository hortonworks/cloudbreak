package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MEDIUM_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MICRO_DUTY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class ShapeValidatorTest {
    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private SdxService sdxService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ShapeValidator underTest;

    static Object[][] notSupportedRuntimes() {
        return new Object[][]{
                {"7.1.0"},
                {"7.2.0"},
                {"7.2.6"},
        };
    }

    @ParameterizedTest
    @MethodSource("notSupportedRuntimes")
    void testValidateShapeMediumDutyInNotSupportedRuntime(String notSupportedRuntime) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateShape(MEDIUM_DUTY_HA, notSupportedRuntime, detailedEnvironmentResponse));

        assertEquals("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + notSupportedRuntime, badRequestException.getMessage());
    }

    @Test
    void testValidateShape() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCreator("crn:cdp:iam:us-west-1:hortonworks:user:test@test.com");

        lenient().when(entitlementService.isSdxRuntimeUpgradeEnabledOnMediumDuty(any())).thenReturn(false);
        lenient().when(entitlementService.microDutySdxEnabled(any())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> underTest.validateShape(MICRO_DUTY, "7.2.11", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(MICRO_DUTY, "7.2.12", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(MICRO_DUTY, "7.2.15", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(MICRO_DUTY, "7.2.16", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(MICRO_DUTY, "7.3.0", detailedEnvironmentResponse));

        assertDoesNotThrow(() -> underTest.validateShape(LIGHT_DUTY, "7.2.16", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(LIGHT_DUTY, "7.2.17", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(LIGHT_DUTY, "7.2.18", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(LIGHT_DUTY, "7.2.19", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(LIGHT_DUTY, "7.3.0", detailedEnvironmentResponse));

        assertDoesNotThrow(() -> underTest.validateShape(MEDIUM_DUTY_HA, "7.2.15", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(MEDIUM_DUTY_HA, "7.2.16", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(MEDIUM_DUTY_HA, "7.2.17", detailedEnvironmentResponse));
        assertThrows(BadRequestException.class, () -> underTest.validateShape(MEDIUM_DUTY_HA, "7.2.18", detailedEnvironmentResponse));
        assertThrows(BadRequestException.class, () -> underTest.validateShape(MEDIUM_DUTY_HA, "7.2.19", detailedEnvironmentResponse));
        assertThrows(BadRequestException.class, () -> underTest.validateShape(MEDIUM_DUTY_HA, "7.3.0", detailedEnvironmentResponse));

        assertThrows(BadRequestException.class, () -> underTest.validateShape(ENTERPRISE, "7.2.16", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(ENTERPRISE, "7.2.17", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(ENTERPRISE, "7.2.18", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(ENTERPRISE, "7.2.19", detailedEnvironmentResponse));
        assertDoesNotThrow(() -> underTest.validateShape(ENTERPRISE, "7.3.0", detailedEnvironmentResponse));
    }

    @Test
    void testValidateShapeMicroDutyNotEnabled() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCreator("crn:cdp:iam:us-west-1:hortonworks:user:test@test.com");

        when(entitlementService.microDutySdxEnabled(any())).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                underTest.validateShape(MICRO_DUTY, "7.2.12", detailedEnvironmentResponse));

        assertEquals(String.format("Provisioning a micro duty data lake cluster is not enabled for %s. ", AWS.name()) +
                "Contact Cloudera support to enable CDP_MICRO_DUTY_SDX entitlement for the account.", ex.getMessage());
    }
}