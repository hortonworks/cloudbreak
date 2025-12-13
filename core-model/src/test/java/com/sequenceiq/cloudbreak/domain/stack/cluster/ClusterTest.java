package com.sequenceiq.cloudbreak.domain.stack.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

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
        assertNull(actualPwd);
    }

    @ParameterizedTest
    @MethodSource("passwords")
    public void testTrustStorePwds(String cmPwd, String truststorePwd, String expectedPwd) {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setCloudbreakClusterManagerPassword(cmPwd);
        cluster.setTrustStorePwd(truststorePwd);
        // WHEN
        String actualPwd = cluster.getTrustStorePwd();
        // THEN
        assertEquals(expectedPwd, actualPwd);
    }

    @Test
    public void testKeyStorePwdWhenNothingIsGiven() {
        // GIVEN
        Cluster cluster = new Cluster();
        // WHEN
        String actualPwd = cluster.getKeyStorePwd();
        // THEN
        assertNull(actualPwd);
    }

    @ParameterizedTest
    @MethodSource("passwords")
    public void testKeyStorePwds(String cmPwd, String truststorePwd, String expectedPwd) {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setCloudbreakClusterManagerPassword(cmPwd);
        cluster.setKeyStorePwd(truststorePwd);
        // WHEN
        String actualPwd = cluster.getKeyStorePwd();
        // THEN
        assertEquals(expectedPwd, actualPwd);
    }

    private static Stream<Arguments> passwords() {
        return Stream.of(
                Arguments.arguments(null, null, null),
                Arguments.arguments("cm", null, "cm"),
                Arguments.arguments("cm", "pwd", "pwd"),
                Arguments.arguments(null, "pwd", "pwd"));
    }
}
