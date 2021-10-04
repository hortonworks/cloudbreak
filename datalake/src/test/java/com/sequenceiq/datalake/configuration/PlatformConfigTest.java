package com.sequenceiq.datalake.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class PlatformConfigTest {

    private PlatformConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new PlatformConfig();
        ReflectionTestUtils.setField(underTest, "dbServiceSslEnforcementSupportedPlatforms", EnumSet.of(CloudPlatform.AWS, CloudPlatform.AZURE));
    }

    static Object[][] sslEnforcementPlatformsDataProvider() {
        return new Object[][]{
                // cloudPlatform resultExpected
                {CloudPlatform.AWS, true},
                {CloudPlatform.AZURE, true},
                {CloudPlatform.GCP, false},
                {CloudPlatform.MOCK, false},
                {CloudPlatform.YARN, false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslEnforcementPlatformsDataProvider")
    void isExternalDatabaseSslEnforcementSupportedForTest(CloudPlatform cloudPlatform, boolean resultExpected) {
        assertThat(underTest.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform)).isEqualTo(resultExpected);
    }

}