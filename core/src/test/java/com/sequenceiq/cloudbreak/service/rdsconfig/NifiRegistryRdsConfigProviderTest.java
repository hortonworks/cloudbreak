package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

class NifiRegistryRdsConfigProviderTest {
    private NifiRegistryRdsConfigProvider underTest = new NifiRegistryRdsConfigProvider();

    @Test
    public void testKnoxGatewayServiceRdsConfigProvider() {
        ReflectionTestUtils.setField(underTest, "nifiRegistryDbPort", "5432");
        ReflectionTestUtils.setField(underTest, "nifiRegistryDbUser", "nifi_registry");
        ReflectionTestUtils.setField(underTest, "nifiRegistryDb", "nifi_registry");

        assertThat(underTest.getDb()).isEqualTo("nifi_registry");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("nifi_registry");
        assertThat(underTest.getPillarKey()).isEqualTo("nifi_registry");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.NIFIREGISTRY);
    }
}