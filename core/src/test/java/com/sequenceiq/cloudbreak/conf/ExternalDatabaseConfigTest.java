package com.sequenceiq.cloudbreak.conf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class ExternalDatabaseConfigTest {

    private ExternalDatabaseConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExternalDatabaseConfig();

        ReflectionTestUtils.setField(underTest, "dbServiceSslEnforcementSupportedPlatforms", Set.of(CloudPlatform.AWS, CloudPlatform.AZURE));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE"}, mode = EnumSource.Mode.INCLUDE)
    void isExternalDatabaseSslEnforcementSupportedForTestWhenTrue(CloudPlatform cloudPlatform) {
        assertThat(underTest.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE"}, mode = EnumSource.Mode.EXCLUDE)
    void isExternalDatabaseSslEnforcementSupportedForTestWhenFalse(CloudPlatform cloudPlatform) {
        assertThat(underTest.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform)).isFalse();
    }

}