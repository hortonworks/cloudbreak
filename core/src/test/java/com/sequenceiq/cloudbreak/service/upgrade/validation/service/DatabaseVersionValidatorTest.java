package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @ParameterizedTest(name = "Target runtime {0}, database engine version {1} should throw validation failure {2}")
    @MethodSource("testScenariosProvider")
    void testValidate(String targetRuntime, String databaseEngineVersion, boolean expectValidationFailure) {
        ServiceUpgradeValidationRequest request = createRequest(targetRuntime, databaseEngineVersion);
        if (expectValidationFailure) {
            Assertions.assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));
        } else {
            underTest.validate(request);
        }
    }

    private static Object[][] testScenariosProvider() {
        return new Object[][] {
                { "7.2.18", "11", false},
                { "7.3.0", "11", false},
                { "7.3.1", "11", true},
                { "7.3.2", "11", true},
                { "7.2.18", "14", false},
                { "7.3.0", "14", false},
                { "7.3.1", "14", false},
                { "7.3.2", "14", false},
                { "7.3.2", "16", false},
        };
    }

    private ServiceUpgradeValidationRequest createRequest(String targetRuntime, String databaseEngineVersion) {
        Database database = new Database();
        database.setExternalDatabaseEngineVersion(databaseEngineVersion);
        Mockito.when(stackDto.getDatabase()).thenReturn(database);
        UpgradeImageInfo upgradeImageInfo = new UpgradeImageInfo(null, StatedImage.statedImage(Image.builder().withVersion(targetRuntime).build(), null, null));
        return new ServiceUpgradeValidationRequest(stackDto, false, false, upgradeImageInfo, false);
    }

}