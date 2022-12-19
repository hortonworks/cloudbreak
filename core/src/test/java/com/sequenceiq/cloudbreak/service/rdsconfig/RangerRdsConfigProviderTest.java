package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

class RangerRdsConfigProviderTest {
    private RangerRdsConfigProvider underTest = new RangerRdsConfigProvider();

    @Test
    public void testRangerRdsConfigProviderTest() {
        ReflectionTestUtils.setField(underTest, "rangerDbPort", "5432");
        ReflectionTestUtils.setField(underTest, "rangerDbUser", "ranger");
        ReflectionTestUtils.setField(underTest, "rangerDb", "ranger");

        assertThat(underTest.getDb()).isEqualTo("ranger");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("ranger");
        assertThat(underTest.getPillarKey()).isEqualTo("ranger");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.RANGER);
    }
}