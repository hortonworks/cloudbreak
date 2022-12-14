package com.sequenceiq.cloudbreak.cloud.aws.common.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

class AwsRegionEndpointProviderTest {

    private AwsRegionEndpointProvider underTest = new AwsRegionEndpointProvider();

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "regionIndependentServices", Set.of("iam"));
    }

    @ParameterizedTest(name = "With service: {0}, with region: {1}, with govCloud: {2}, expected: {3}.")
    @MethodSource("scenariosForRegionProvider")
    public void test(String inputService, String inputRegion, boolean govCloud, String expected) {
        String region = underTest.region(inputService, inputRegion, govCloud);
        assertEquals(expected, region);
    }

    static Object[][] scenariosForRegionProvider() {
        return new Object[][]{
                {"iam", "usgw1", true, "us-gov"},
                {"iam", "usgw1", false, "usgw1"},
                {"iam", "us-east-1", true, "us-gov"},
                {"iam", "us-east-1", false, "us-east-1"},
        };
    }

}