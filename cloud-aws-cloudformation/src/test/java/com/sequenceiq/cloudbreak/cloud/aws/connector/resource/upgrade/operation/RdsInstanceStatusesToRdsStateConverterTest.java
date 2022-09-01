package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RdsInstanceStatusesToRdsStateConverterTest {

    private RdsInstanceStatusesToRdsStateConverter underTest = new RdsInstanceStatusesToRdsStateConverter();

    @Test
    void testConvertWhenNoInstanceStatus() {
        Map<String, String> dbArnToInstanceStatusesMap = Map.of();

        assertEquals(RdsState.UNKNOWN, underTest.convert(dbArnToInstanceStatusesMap));
    }

    @Test
    void testConvertWhenInstancesAvailable() {
        Map<String, String> dbArnToInstanceStatusesMap = Map.of("instance1", "available");

        assertEquals(RdsState.AVAILABLE, underTest.convert(dbArnToInstanceStatusesMap));
    }

    @Test
    void testConvertWhenInstancesUpgrading() {
        Map<String, String> dbArnToInstanceStatusesMap = Map.of("instance1", "upgrading");

        assertEquals(RdsState.UPGRADING, underTest.convert(dbArnToInstanceStatusesMap));
    }

    @Test
    void testConvertWhenAtLeastOneInstanceIsNotAvailable() {
        Map<String, String> dbArnToInstanceStatusesMap = Map.of("instance1", "available", "instance2", "anotherState");

        assertEquals(RdsState.OTHER, underTest.convert(dbArnToInstanceStatusesMap));
    }

    @Test
    void testConvertWhenAtLeastOneInstanceIsUpgrading() {
        Map<String, String> dbArnToInstanceStatusesMap = Map.of("instance1", "upgrading", "instance2", "anotherState");

        assertEquals(RdsState.UPGRADING, underTest.convert(dbArnToInstanceStatusesMap));
    }

    @Test
    void testConvertWhenInstancesAnyOtherState() {
        Map<String, String> dbArnToInstanceStatusesMap = Map.of("instance1", "any other state");

        assertEquals(RdsState.OTHER, underTest.convert(dbArnToInstanceStatusesMap));
    }

    @ParameterizedTest
    @ValueSource(strings = {"availablePlusSomething", "upgradingPlusSomething"})
    void testConvertWhenInstanceStateResemblesMappedStatus(String resemblingState) {
        Map<String, String> dbArnToInstanceStatusesMap = Map.of("instance1", resemblingState);

        assertEquals(RdsState.OTHER, underTest.convert(dbArnToInstanceStatusesMap));
    }

}
