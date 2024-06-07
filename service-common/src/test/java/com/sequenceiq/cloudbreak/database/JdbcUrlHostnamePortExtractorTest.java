package com.sequenceiq.cloudbreak.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.data.util.Pair;

class JdbcUrlHostnamePortExtractorTest {

    @ParameterizedTest
    @NullSource
    @EmptySource
    void getHostnamePortWithEmptyString(String jdbcUrl) {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> JdbcUrlHostnamePortExtractor.getHostnamePort(jdbcUrl));

        assertEquals("JdbcUrl could not be empty String.", exception.getMessage());
    }

    static Stream<Arguments> getTestJdbcUrls() {
        return Stream.of(
                arguments("jdbc:postgres://customrds.rds.amazonaws.com:5432/dbname", "customrds.rds.amazonaws.com", 5432),
                arguments("jdbc:postgresql:cust-wrapper//myinstance.hostname:5111/dbname?user=user", "myinstance.hostname", 5111),
                arguments("jdbc:aws-wrapper:postgresql://db-identifier.hostname:5432/db", "db-identifier.hostname", 5432)
        );
    }

    @ParameterizedTest
    @MethodSource("getTestJdbcUrls")
    void getHostnamePort(String jdbcUrl, String expectedHost, int expectedPort) {
        Pair<String, Integer> hostNameAndPort = JdbcUrlHostnamePortExtractor.getHostnamePort(jdbcUrl);

        assertNotNull(hostNameAndPort);
        assertEquals(expectedHost, hostNameAndPort.getFirst());
        assertEquals(expectedPort, hostNameAndPort.getSecond());
    }
}