package com.sequenceiq.cloudbreak.domain.stack.cluster;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ClusterTest {
    @Test
    public void testTrustStorePwdWhenNothingIsGiven() {
        // GIVEN
        Cluster cluster = new Cluster();
        // WHEN
        String actualPwd = cluster.getTrustStorePwd();
        // THEN
        Assertions.assertNull(actualPwd);
    }

    @ParameterizedTest
    @MethodSource("passwords")
    public void testTrustStorePwds(String ambariPwd, String cmPwd, String truststorePwd, String expectedPwd) {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setCloudbreakAmbariPassword(ambariPwd);
        cluster.setCloudbreakClusterManagerPassword(cmPwd);
        cluster.setTrustStorePwd(truststorePwd);
        // WHEN
        String actualPwd = cluster.getTrustStorePwd();
        // THEN
        Assertions.assertEquals(expectedPwd, actualPwd);
    }

    @Test
    public void testKeyStorePwdWhenNothingIsGiven() {
        // GIVEN
        Cluster cluster = new Cluster();
        // WHEN
        String actualPwd = cluster.getKeyStorePwd();
        // THEN
        Assertions.assertNull(actualPwd);
    }

    @ParameterizedTest
    @MethodSource("passwords")
    public void testKeyStorePwds(String ambariPwd, String cmPwd, String truststorePwd, String expectedPwd) {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setCloudbreakAmbariPassword(ambariPwd);
        cluster.setCloudbreakClusterManagerPassword(cmPwd);
        cluster.setKeyStorePwd(truststorePwd);
        // WHEN
        String actualPwd = cluster.getKeyStorePwd();
        // THEN
        Assertions.assertEquals(expectedPwd, actualPwd);
    }

    private static Stream<Arguments> passwords() {
        return Stream.of(
                Arguments.arguments(null, null, null, null),
                Arguments.arguments("ambari", null, null, "ambari"),
                Arguments.arguments(null, "cm", null, "cm"),
                Arguments.arguments("ambari", "cm", "pwd", "pwd"),
                Arguments.arguments("ambari", "cm", null, "cm"),
                Arguments.arguments(null, "cm", null, "cm"),
                Arguments.arguments(null, null, "pwd", "pwd"));
    }
}
