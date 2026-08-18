package com.sequenceiq.datalake.service.validation.resize;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
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

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.service.sdx.database.AzureDatabaseAttributesService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
class SdxResizeValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @InjectMocks
    private SdxResizeValidator underTest;

    @Mock
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @Mock
    private EntitlementService entitlementService;

    @Test
    void testValidateDatabaseTypeForResizeShouldThrowExceptionWhenTheDatabaseTypeIsSingleServer() {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.HA);

        when(azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase)).thenReturn(AzureDatabaseType.SINGLE_SERVER);

        assertThrows(BadRequestException.class, () -> doAs(USER_CRN, () -> underTest.validateDatabaseTypeForResize(sdxDatabase, CloudPlatform.AZURE)));
    }

    @ParameterizedTest(name = "[{index}] Cloud platform: {0}, Database availability type: {1}, Flexible server upgrade entitled: {2}, Azure database type: {3}")
    @MethodSource("provideTestParameters")
    void testValidateDatabaseTypeForResizeShouldNotThrowException(CloudPlatform cloudPlatform, SdxDatabaseAvailabilityType databaseAvailabilityType,
            boolean flexibleServerUpgradeEntitlementEnabled, AzureDatabaseType azureDatabaseType) {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(databaseAvailabilityType);

        lenient().when(azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase)).thenReturn(azureDatabaseType);

        doAs(USER_CRN, () -> underTest.validateDatabaseTypeForResize(sdxDatabase, cloudPlatform));
    }

    private static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of(CloudPlatform.AWS, SdxDatabaseAvailabilityType.HA, true, AzureDatabaseType.FLEXIBLE_SERVER),
                Arguments.of(CloudPlatform.AZURE, SdxDatabaseAvailabilityType.NONE, true, AzureDatabaseType.FLEXIBLE_SERVER),
                Arguments.of(CloudPlatform.AZURE, null, true, AzureDatabaseType.FLEXIBLE_SERVER),
                Arguments.of(CloudPlatform.AZURE, SdxDatabaseAvailabilityType.HA, false, AzureDatabaseType.FLEXIBLE_SERVER),
                Arguments.of(CloudPlatform.AZURE, SdxDatabaseAvailabilityType.HA, true, AzureDatabaseType.FLEXIBLE_SERVER),
                Arguments.of(CloudPlatform.AZURE, SdxDatabaseAvailabilityType.HA, true, null)
        );
    }

    @ParameterizedTest(name = "[{index}] current: {0}, target: {1}, singleToMultiAz: {2}")
    @MethodSource("provideInvalidShapeTransitions")
    void testValidateResizeShapeTransitionShouldThrowException(SdxClusterShape currentShape, SdxClusterShape targetShape, boolean singleToMultiAzTransition) {
        assertThrows(BadRequestException.class, () -> underTest.validateResizeShapeTransition(currentShape, targetShape, singleToMultiAzTransition));
    }

    @ParameterizedTest(name = "[{index}] current: {0}, target: {1}, singleToMultiAz: {2}")
    @MethodSource("provideValidShapeTransitions")
    void testValidateResizeShapeTransitionShouldNotThrowException(SdxClusterShape currentShape, SdxClusterShape targetShape, boolean singleToMultiAzTransition) {
        underTest.validateResizeShapeTransition(currentShape, targetShape, singleToMultiAzTransition);
    }

    private static Stream<Arguments> provideInvalidShapeTransitions() {
        return Stream.of(
                Arguments.of(SdxClusterShape.LIGHT_DUTY, SdxClusterShape.ENTERPRISE_WITHOUT_HBASE, false),
                Arguments.of(SdxClusterShape.MEDIUM_DUTY_HA, SdxClusterShape.ENTERPRISE_WITHOUT_HBASE, false),
                Arguments.of(SdxClusterShape.ENTERPRISE, SdxClusterShape.ENTERPRISE_WITHOUT_HBASE, false),
                Arguments.of(SdxClusterShape.ENTERPRISE_WITHOUT_HBASE, SdxClusterShape.ENTERPRISE_WITHOUT_HBASE, false),
                Arguments.of(SdxClusterShape.LIGHT_DUTY_WITHOUT_HBASE, SdxClusterShape.ENTERPRISE, false),
                Arguments.of(SdxClusterShape.ENTERPRISE_WITHOUT_HBASE, SdxClusterShape.ENTERPRISE, false),
                Arguments.of(SdxClusterShape.LIGHT_DUTY, SdxClusterShape.LIGHT_DUTY_WITHOUT_HBASE, false),
                Arguments.of(SdxClusterShape.LIGHT_DUTY_WITHOUT_HBASE, SdxClusterShape.LIGHT_DUTY_WITHOUT_HBASE, false),
                Arguments.of(SdxClusterShape.LIGHT_DUTY_WITHOUT_HBASE, SdxClusterShape.LIGHT_DUTY_WITHOUT_HBASE, true)
        );
    }

    private static Stream<Arguments> provideValidShapeTransitions() {
        return Stream.of(
                Arguments.of(SdxClusterShape.LIGHT_DUTY_WITHOUT_HBASE, SdxClusterShape.ENTERPRISE_WITHOUT_HBASE, false),
                Arguments.of(SdxClusterShape.ENTERPRISE_WITHOUT_HBASE, SdxClusterShape.ENTERPRISE_WITHOUT_HBASE, true),
                Arguments.of(SdxClusterShape.LIGHT_DUTY, SdxClusterShape.ENTERPRISE, false),
                Arguments.of(SdxClusterShape.MEDIUM_DUTY_HA, SdxClusterShape.ENTERPRISE, false),
                Arguments.of(SdxClusterShape.LIGHT_DUTY, SdxClusterShape.MEDIUM_DUTY_HA, false)
        );
    }

}