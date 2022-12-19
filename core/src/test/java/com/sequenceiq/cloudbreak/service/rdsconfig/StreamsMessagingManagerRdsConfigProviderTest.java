package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

public class StreamsMessagingManagerRdsConfigProviderTest {
    private StreamsMessagingManagerRdsConfigProvider underTest = new StreamsMessagingManagerRdsConfigProvider();

    @Test
    public void testSqlStreamBuilderAdminRdsConfigProviderTest() {
        ReflectionTestUtils.setField(underTest, "port", "5432");
        ReflectionTestUtils.setField(underTest, "userName", "smm");
        ReflectionTestUtils.setField(underTest, "db", "smm");

        assertThat(underTest.getDb()).isEqualTo("smm");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("smm");
        assertThat(underTest.getPillarKey()).isEqualTo("smm");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.STREAMS_MESSAGING_MANAGER);
    }

}
