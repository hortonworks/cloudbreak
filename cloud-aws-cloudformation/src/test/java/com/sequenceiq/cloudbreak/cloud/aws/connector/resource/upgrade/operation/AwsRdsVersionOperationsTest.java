package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;

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

    // Note: There is no easy way to test the "bad engine variant" case as enum classes are final and hard to mock.
    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndBadVersionFormat() {
        Assertions.assertThrows(IllegalStateException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "latest")
        );
    }

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndMissingMinorVersion() {
        String dbFamily = underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "10");

        assertEquals("postgres10", dbFamily);
    }

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndMajorVersionNumericOverflow() {
        Assertions.assertThrows(NumberFormatException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "12345678901234567890.1")
        );
    }

    static Object[][] unsupportedMajorVersionDataProvider() {
        return new Object[][]{
                // testCaseName version
                {"version 0.1", "0.1"},
                {"version 1.1", "1.1"},
                {"version 2.1", "2.1"},
                {"version 3.1", "3.1"},
                {"version 4.1", "4.1"},
                {"version 5.1", "5.1"},
                {"version 6.1", "6.1"},
                {"version 7.1", "7.1"},
                {"version 8.1", "8.1"},
                {"version 123.1", "123.1"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedMajorVersionDataProvider")
    void getDBParameterGroupFamilyTestWhenPgSqlAndUnsupportedMajorVersion(String testCaseName, String version) {
        Assertions.assertThrows(IllegalStateException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, version)
        );
    }

    static Object[][] validVersionDataProvider() {
        return new Object[][]{
                // testCaseName version familyExpected
                {"version 9.5", "9.5", "postgres9.5"},
                {"version 9.6", "9.6", "postgres9.6"},
                {"version 10.1", "10.1", "postgres10"},
                {"version 11.1", "11.1", "postgres11"},
                {"version 12.1", "12.1", "postgres12"},
                {"version 13.1", "13.1", "postgres13"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validVersionDataProvider")
    void getDBParameterGroupFamilyTestWhenPgSqlAndValidVersion(String testCaseName, String version, String familyExpected) {
        String dbFamily = underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, version);

        assertEquals(familyExpected, dbFamily);
    }

}
