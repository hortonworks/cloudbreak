package com.sequenceiq.cloudbreak.cloud.aws.common.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

class AwsServiceEndpointProviderTest {

    private AwsServiceEndpointProvider underTest = new AwsServiceEndpointProvider();

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "fipsSuffixedServices", Set.of("kms", "elasticfilesystem", "s3"));
    }

    @ParameterizedTest(name = "With service: {0}, with govCloud: {1}, expected: {2}.")
    @MethodSource("scenariosForServiceProvider")
    public void test(String inputService, boolean govCloud, String expected) {
        String region = underTest.service(inputService, govCloud);
        assertEquals(expected, region);
    }

    static Object[][] scenariosForServiceProvider() {
        return new Object[][]{
                {"kms", true, "kms-fips"},
                {"kms", false, "kms"},

                {"elasticfilesystem", true, "elasticfilesystem-fips"},
                {"elasticfilesystem", false, "elasticfilesystem"},

                {"s3", true, "s3-fips"},
                {"s3", false, "s3"},

                {"autoscale", true, "autoscale"},
                {"autoscale", false, "autoscale"},
        };
    }

}