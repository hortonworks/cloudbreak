package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

class ProfilerAdminRdsConfigProviderTest {
    private ProfilerAdminRdsConfigProvider underTest = new ProfilerAdminRdsConfigProvider();

    @Test
    public void testKnoxGatewayServiceRdsConfigProvider() {
        ReflectionTestUtils.setField(underTest, "profilerAdminDbPort", "5432");
        ReflectionTestUtils.setField(underTest, "profilerAdminDbUser", "profiler_admin");
        ReflectionTestUtils.setField(underTest, "profilerAdminDb", "profiler_admin");

        assertThat(underTest.getDb()).isEqualTo("profiler_admin");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("profiler_admin");
        assertThat(underTest.getPillarKey()).isEqualTo("profiler_admin");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.PROFILER_AGENT);
    }
}