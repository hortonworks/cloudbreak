package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

class ProfilerMetricsRdsConfigProviderTest {
    private ProfilerMetricsRdsConfigProvider underTest = new ProfilerMetricsRdsConfigProvider();

    @Test
    public void testKnoxGatewayServiceRdsConfigProvider() {
        ReflectionTestUtils.setField(underTest, "profilerMetricsDbPort", "5432");
        ReflectionTestUtils.setField(underTest, "profilerMetricsDbUser", "profiler_metrics");
        ReflectionTestUtils.setField(underTest, "profilerMetricsDb", "profiler_metrics");

        assertThat(underTest.getDb()).isEqualTo("profiler_metrics");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("profiler_metrics");
        assertThat(underTest.getPillarKey()).isEqualTo("profiler_metrics");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.PROFILER_METRIC);
    }
}