package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

class OozieRdsConfigProviderTest {
    private OozieRdsConfigProvider underTest = new OozieRdsConfigProvider();

    @Test
    public void testKnoxGatewayServiceRdsConfigProvider() {
        ReflectionTestUtils.setField(underTest, "oozieDbPort", "5432");
        ReflectionTestUtils.setField(underTest, "oozieDbUser", "oozie");
        ReflectionTestUtils.setField(underTest, "oozieDb", "oozie");

        assertThat(underTest.getDb()).isEqualTo("oozie");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("oozie");
        assertThat(underTest.getPillarKey()).isEqualTo("oozie");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.OOZIE);
    }
}