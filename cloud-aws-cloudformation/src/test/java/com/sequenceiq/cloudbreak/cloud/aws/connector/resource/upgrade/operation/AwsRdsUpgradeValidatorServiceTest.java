package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class AwsRdsUpgradeValidatorServiceTest {

    private final AwsRdsUpgradeValidatorService underTest = new AwsRdsUpgradeValidatorService();

    @Test
    void testValidateRdsIsAvailableOrUpgrading() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, Map.of(), new RdsEngineVersion("10"));

        Assertions.assertDoesNotThrow(() ->
                underTest.validateRdsIsAvailableOrUpgrading(rdsInfo)
        );
    }

    @ParameterizedTest
    @EnumSource(value = RdsState.class, names = {"AVAILABLE", "UPGRADING"}, mode = EnumSource.Mode.EXCLUDE)
    void testValidateRdsIsAvailableOrUpgradingWhenStateNotOk(RdsState rdsState) {
        RdsInfo rdsInfo = new RdsInfo(rdsState, Map.of(), new RdsEngineVersion("10"));

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.validateRdsIsAvailableOrUpgrading(rdsInfo)
        );
    }

    @Test
    void testValidateUpgradePresentForTargetMajorVersion() {
        Optional<RdsEngineVersion> upgradeTargetsForMajorVersion = Optional.of(new RdsEngineVersion("v1"));

        Assertions.assertDoesNotThrow(() ->
                underTest.validateUpgradePresentForTargetMajorVersion(upgradeTargetsForMajorVersion)
        );
    }

    @Test
    void testValidateUpgradePresentForTargetMajorVersionWhenNoTargetVersionPresent() {
        Optional<RdsEngineVersion> upgradeTargetsForMajorVersion = Optional.empty();

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.validateUpgradePresentForTargetMajorVersion(upgradeTargetsForMajorVersion)
        );
    }

    @Test
    void testClusterHasSingleVersion() {
        Set<String> dbVersions = Set.of("version");

        underTest.validateClusterHasASingleVersion(dbVersions);
    }

    @Test
    void testClusterHasSingleVersionWhenMultipleVersions() {
        Set<String> dbVersions = Set.of("v1", "v2");

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.validateClusterHasASingleVersion(dbVersions)
        );
    }

    @Test
    void testClusterHasSingleVersionWhenVersionsEmpty() {
        Set<String> dbVersions = Set.of();

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.validateClusterHasASingleVersion(dbVersions)
        );
    }

}
