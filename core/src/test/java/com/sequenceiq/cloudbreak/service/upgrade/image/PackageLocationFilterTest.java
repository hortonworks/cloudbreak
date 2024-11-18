package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PackageLocationFilterTest {

    @ParameterizedTest
    @MethodSource("testData")
    public void testPattern(String url, boolean expected) {
        assertEquals(expected, PackageLocationFilter.URL_PATTERNS.stream().anyMatch(url::matches));
    }

    private static Stream<Arguments> testData() {
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
                Arguments.of("https://archive.random.com/asdf", false),
                Arguments.of("https://archive.releng.gov-dev.cloudera.com/p/cdh7/7.2.16.400/parcels/", true),
                Arguments.of("https://archive.releng.gov-dev.cloudera.com/s3/build/44307214/cm7/7.9.2/redhat8/yum/", true),
                Arguments.of("https://archive.repo.cdp.clouderagovt.com/p/cdh7/7.2.18.100/parcels/", true),
                Arguments.of("https://stage.repo.cdp.clouderagovt.com/s3/build/54590435/cm7/7.12.0.300/redhat8/yum/", true)
        );
    }

}