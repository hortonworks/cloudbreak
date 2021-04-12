package com.sequenceiq.environment.environment.validation.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

@ExtendWith(MockitoExtension.class)
public class EnvironmentBackupStorageLocationValidatorTest {

    private static final String REGION_1 = "region-1";

    @Mock
    private CloudStorageLocationValidator validator;

    @Mock
    private Environment environment;

    @Mock
    private EnvironmentBackup backup;

    @InjectMocks
    private EnvironmentBackupStorageLocationValidator underTest;

    @Test
    public void validateBackupStorageLocationNoBackup() {
        when(environment.getBackup()).thenReturn(null);
        ValidationResult result = underTest.validateBackupStorageLocation(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateBackupStorageLocationNoStorageLocation() {
        when(environment.getBackup()).thenReturn(backup);
        when(backup.getS3()).thenReturn(new S3CloudStorageParameters());
        when(backup.getStorageLocation()).thenReturn(null);
        ValidationResult result = underTest.validateBackupStorageLocation(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateBackupStorageLocationValidatorPasses() {
        when(environment.getBackup()).thenReturn(backup);
        when(backup.getS3()).thenReturn(new S3CloudStorageParameters());
        when(backup.getStorageLocation()).thenReturn(REGION_1);
        ValidationResult result = underTest.validateBackupStorageLocation(environment);
        assertFalse(result.hasError());
    }
}
