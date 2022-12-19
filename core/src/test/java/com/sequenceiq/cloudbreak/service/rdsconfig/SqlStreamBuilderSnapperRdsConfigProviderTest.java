package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

public class SqlStreamBuilderSnapperRdsConfigProviderTest {
    private SqlStreamBuilderSnapperRdsConfigProvider underTest = new SqlStreamBuilderSnapperRdsConfigProvider();

    @Test
    public void testSqlStreamBuilderAdminRdsConfigProviderTest() {
        ReflectionTestUtils.setField(underTest, "port", "5432");
        ReflectionTestUtils.setField(underTest, "userName", "ssb_snapper");
        ReflectionTestUtils.setField(underTest, "db", "ssb_snapper");

        assertThat(underTest.getDb()).isEqualTo("ssb_snapper");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("ssb_snapper");
        assertThat(underTest.getPillarKey()).isEqualTo("ssb_snapper");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.SQL_STREAM_BUILDER_SNAPPER);
    }
}
