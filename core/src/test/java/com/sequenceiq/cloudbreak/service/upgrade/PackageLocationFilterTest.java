package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PackageLocationFilterTest {

    @ParameterizedTest
    @MethodSource("providaTestData")
    public void testPattern(String url, boolean expected) {
        assertEquals(expected, PackageLocationFilter.URL_PATTERN.matcher(url).find());
    }

    private static Stream<Arguments> providaTestData() {
        return Stream.of(
                Arguments.of("http://archive.cloudera.com", false),
                Arguments.of("https://archive.cloudera.com", false),
                Arguments.of("http://archive.cloudera.com/asdf", true),
                Arguments.of("https://archive.cloudera.com/asdf", true),
                Arguments.of("http://archiveXcloudera.com/asdf", false),
                Arguments.of("http://archive.clouderaXcom/asdf", false),
                Arguments.of("http://archive.cloudera.com/build/4448967/cm7/7.2.1/redhat7/yum/", true),
                Arguments.of("https://archive.cloudera.com/build/4448967/cm7/7.2.1/redhat7/yum/", true),
                Arguments.of("http://archive.cloudera.com/s3/build/4452156/cdh/7.x/parcels/", true),
                Arguments.of("https://archive.cloudera.com/s3/build/4452156/cdh/7.x/parcels/", true),
                Arguments.of("https://cloudera.com/asdf", false),
                Arguments.of("archive.cloudera.com/asdf", false),
                Arguments.of("https://cloudera.com/asdf", false),
                Arguments.of("https://archive.random.com/asdf", false)
        );
    }

}