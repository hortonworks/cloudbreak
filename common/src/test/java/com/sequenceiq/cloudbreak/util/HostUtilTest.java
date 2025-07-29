package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HostUtilTest {

    static Stream<Arguments> testHasPortArguments() {
        return Stream.of(
                Arguments.of("localhost:9", true),
                Arguments.of("localhost:99", true),
                Arguments.of("localhost:999", true),
                Arguments.of("localhost:9999", true),
                Arguments.of("localhost:99999", true),
                Arguments.of("http://asdasd:99999", true),
                Arguments.of("http://asdasd:99999/", true),
                Arguments.of("http://asdasd:99999/asd", true),
                Arguments.of("http://asdasd:99999/asd/", true),
                Arguments.of("http://lts2100-dps-cluster-proxy.lts2100.svc.cluster.local:99999", true),

                Arguments.of(null, false),
                Arguments.of("localhost", false),
                Arguments.of("localhost:", false),
                Arguments.of("local2100host", false),
                Arguments.of("local2100host:", false),
                Arguments.of("http://asdasd", false),
                Arguments.of("http://asdasd/", false),
                Arguments.of("http://asdasd/asd", false),
                Arguments.of("http://asdasd/asd/", false),
                Arguments.of("http://lts2100-dps-cluster-proxy.lts2100.svc.cluster.local", false),
                Arguments.of("http://lts2100-dps-cluster-proxy.lts2100.svc.cluster.local:", false)
        );
    }

    @ParameterizedTest
    @MethodSource("testHasPortArguments")
    void testHasPort(String url, boolean expected) {
        assertEquals(expected, HostUtil.hasPort(url));
    }
}
