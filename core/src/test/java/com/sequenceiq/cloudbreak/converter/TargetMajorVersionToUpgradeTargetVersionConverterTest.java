package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;

@ExtendWith(MockitoExtension.class)
public class TargetMajorVersionToUpgradeTargetVersionConverterTest {

    @InjectMocks
    private TargetMajorVersionToUpgradeTargetVersionConverter converter;

    @Test
    void testConvertVersion11Success() {
        // Given
        TargetMajorVersion sourceTargetVersion = TargetMajorVersion.VERSION_11;

        // When
        UpgradeTargetMajorVersion result = converter.convert(sourceTargetVersion);

        // Then
        assertEquals(UpgradeTargetMajorVersion.VERSION_11, result);
    }

    @Test
    void testConvertVersion14Success() {
        // Given
        TargetMajorVersion sourceTargetVersion = TargetMajorVersion.VERSION_14;

        // When
        UpgradeTargetMajorVersion result = converter.convert(sourceTargetVersion);

        // Then
        assertEquals(UpgradeTargetMajorVersion.VERSION_14, result);
    }
}
