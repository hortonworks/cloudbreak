package com.sequenceiq.cloudbreak.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PlatformStringTransformerTest {

    private static final String AWS = "AWS";

    private static final String AWS_GOV = "AWS_GOV";

    private static final String AWS_NATIVE = "AWS_NATIVE";

    private static final String AWS_NATIVE_GOV = "AWS_NATIVE_GOV";

    private static final String GCP = "GCP";

    private static final String AZURE = "AZURE";

    private static final String MOCK = "MOCK";

    private static final String YARN = "YARN";

    private final PlatformStringTransformer underTest = new PlatformStringTransformer();

    private static Stream<Arguments> variantFlags() {
        return Stream.of(
                Arguments.of(AWS, AWS, AWS),
                Arguments.of(AWS, AWS_NATIVE, AWS),
                Arguments.of(AWS, AWS_NATIVE_GOV, AWS_GOV),
                Arguments.of(AWS, null, AWS),
                Arguments.of(AWS, "", AWS),

                Arguments.of(GCP, GCP, GCP),
                Arguments.of(GCP, null, GCP),
                Arguments.of(GCP, "", GCP),

                Arguments.of(AZURE, AZURE, AZURE),
                Arguments.of(AZURE, null, AZURE),
                Arguments.of(AZURE, "", AZURE),

                Arguments.of(MOCK, null, MOCK),
                Arguments.of(MOCK, "", MOCK),

                Arguments.of(YARN, null, YARN),
                Arguments.of(YARN, "", YARN)
        );
    }

    @ParameterizedTest
    @MethodSource("variantFlags")
    public void testGetPlatformStringForImageCatalog(String platform, String variant, String expected) {
        assertEquals(expected.toLowerCase(Locale.ROOT), underTest.getPlatformStringForImageCatalog(platform, variant).nameToLowerCase());
    }

    private static Stream<Arguments> regionSource() {
        return Stream.of(
                Arguments.of(AWS, "us-west", AWS),
                Arguments.of(AWS, "us-gov-west", AWS_GOV),
                Arguments.of(AWS_NATIVE_GOV, "us-gov-west", AWS_NATIVE_GOV),
                Arguments.of(AWS_NATIVE, "us-gov-west", AWS_NATIVE_GOV),
                Arguments.of(AWS_NATIVE, "us-west", AWS_NATIVE),
                Arguments.of(AWS, null, AWS),
                Arguments.of(AWS, "", AWS),

                Arguments.of(GCP, "us-west", GCP),
                Arguments.of(GCP, null, GCP),
                Arguments.of(GCP, "", GCP),

                Arguments.of(AZURE, "us-west", AZURE),
                Arguments.of(AZURE, null, AZURE),
                Arguments.of(AZURE, "", AZURE),

                Arguments.of(MOCK, null, MOCK),
                Arguments.of(MOCK, "", MOCK),

                Arguments.of(YARN, null, YARN),
                Arguments.of(YARN, "", YARN)
        );
    }

    @ParameterizedTest
    @MethodSource("regionSource")
    public void testGetPlatformStringForImageCatalogByRegion(String platform, String region, String expected) {
        assertEquals(expected.toLowerCase(Locale.ROOT), underTest.getPlatformStringForImageCatalogByRegion(platform, region).nameToLowerCase());
    }

}