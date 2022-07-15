package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@ExtendWith(MockitoExtension.class)
public class AwsRdsVersionOperationsTest {

    private final AwsRdsVersionOperations underTest = new AwsRdsVersionOperations();

    @Test
    void testGetHighestUpgradeVersion() {
        Set<String> validUpgradeVersions = Set.of("1.2");

        String selectedTargetVersion = underTest.getHighestUpgradeVersion(validUpgradeVersions, () -> "1");

        assertEquals("1.2", selectedTargetVersion);
    }

    @Test
    void testGetHighestUpgradeVersionWhenMultipleWithSameMajorThenHighestSelected() {
        Set<String> validUpgradeVersions = Set.of("1.2", "1.3");

        String selectedTargetVersion = underTest.getHighestUpgradeVersion(validUpgradeVersions, () -> "1");

        assertEquals("1.3", selectedTargetVersion);
    }

    @Test
    void testGetHighestUpgradeVersionWhenMultipleMajorsThenHighestSelected() {
        Set<String> validUpgradeVersions = Set.of("1.2", "2.3", "3.4");

        String selectedTargetVersion = underTest.getHighestUpgradeVersion(validUpgradeVersions, () -> "2");

        assertEquals("2.3", selectedTargetVersion);
    }

    @Test
    void testGetHighestUpgradeVersionWhenNoMatchingMajorVersion() {
        Set<String> validUpgradeVersions = Set.of("1.2", "1.3");

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.getHighestUpgradeVersion(validUpgradeVersions, () -> "2")
        );
    }

}
