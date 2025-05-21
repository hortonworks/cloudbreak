package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
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

    @ParameterizedTest(name = "Target runtime {0}, database engine version {1}, database availability type {2} should throw validation failure {4}")
    @MethodSource("testScenariosProvider")
    void testValidate(String targetRuntime, String databaseEngineVersion, DatabaseAvailabilityType databaseAvailabilityType, StackType stackType,
            boolean expectValidationFailure) {
        ServiceUpgradeValidationRequest request = createRequest(targetRuntime, databaseEngineVersion, databaseAvailabilityType, stackType);
        if (expectValidationFailure) {
            Assertions.assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));
        } else {
            underTest.validate(request);
        }
    }

    private static Object[][] testScenariosProvider() {
        return new Object[][] {
                { "7.2.18", "11", HA, DATALAKE, false},
                { "7.3.1", "11", HA, DATALAKE, true},
                { "7.3.1", "11", HA, WORKLOAD, true},
                { "7.3.1", "11", NONE, DATALAKE, false},
                { "7.3.1", "11", null, DATALAKE, false},
                { "7.3.1", "11", NONE, WORKLOAD, false},
                { "7.3.2", "11", HA, DATALAKE, true},
                { "7.2.18", "14", HA, DATALAKE, false},
                { "7.2.18", "14", NONE, DATALAKE, false},
                { "7.2.18", "14", null, DATALAKE, false},
                { "7.3.1", "14", HA, DATALAKE, false},
                { "7.3.2", "14", HA, DATALAKE, false},
                { "7.3.2", "16", HA, DATALAKE, false},
        };
    }

    private ServiceUpgradeValidationRequest createRequest(String targetRuntime, String databaseEngineVersion, DatabaseAvailabilityType availabilityType,
            StackType stackType) {
        Database database = new Database();
        if (DATALAKE.equals(stackType)) {
            database.setDatalakeDatabaseAvailabilityType(availabilityType);
        } else {
            database.setExternalDatabaseAvailabilityType(availabilityType);
        }
        database.setExternalDatabaseEngineVersion(databaseEngineVersion);

        Mockito.when(stackDto.getDatabase()).thenReturn(database);
        Mockito.when(stackDto.getType()).thenReturn(stackType);
        UpgradeImageInfo upgradeImageInfo = new UpgradeImageInfo(null, StatedImage.statedImage(Image.builder().withVersion(targetRuntime).build(), null, null));
        return new ServiceUpgradeValidationRequest(stackDto, false, false, upgradeImageInfo, false);
    }

}
