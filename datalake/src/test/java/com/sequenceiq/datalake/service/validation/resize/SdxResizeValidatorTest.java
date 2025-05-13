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

}