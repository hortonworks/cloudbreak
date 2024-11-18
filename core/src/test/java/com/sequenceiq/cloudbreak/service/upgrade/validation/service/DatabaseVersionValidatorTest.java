package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType.HA;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType.NONE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

@ExtendWith(MockitoExtension.class)
class DatabaseVersionValidatorTest {

    @InjectMocks
    private DatabaseVersionValidator underTest;

    @Mock
    private StackDto stackDto;

    @ParameterizedTest(name = "Target runtime {0}, database engine version {1}, database availability type {2} should throw validation failure {3}")
    @MethodSource("testScenariosProvider")
    void testValidate(String targetRuntime, String databaseEngineVersion, DatabaseAvailabilityType databaseAvailabilityType, boolean expectValidationFailure) {
        ServiceUpgradeValidationRequest request = createRequest(targetRuntime, databaseEngineVersion, databaseAvailabilityType);
        if (expectValidationFailure) {
            Assertions.assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));
        } else {
            underTest.validate(request);
        }
    }

    private static Object[][] testScenariosProvider() {
        return new Object[][] {
                { "7.2.18", "11", HA, false},
                { "7.3.1", "11", HA, true},
                { "7.3.1", "11", NONE, false},
                { "7.3.2", "11", HA, true},
                { "7.2.18", "14", HA, false},
                { "7.2.18", "14", NONE, false},
                { "7.3.1", "14", HA, false},
                { "7.3.2", "14", HA, false},
                { "7.3.2", "16", HA, false},
        };
    }

    private ServiceUpgradeValidationRequest createRequest(String targetRuntime, String databaseEngineVersion, DatabaseAvailabilityType availabilityType) {
        Database database = new Database();
        database.setExternalDatabaseEngineVersion(databaseEngineVersion);
        database.setExternalDatabaseAvailabilityType(availabilityType);
        Mockito.when(stackDto.getDatabase()).thenReturn(database);
        UpgradeImageInfo upgradeImageInfo = new UpgradeImageInfo(null, StatedImage.statedImage(Image.builder().withVersion(targetRuntime).build(), null, null));
        return new ServiceUpgradeValidationRequest(stackDto, false, false, upgradeImageInfo, false);
    }

}