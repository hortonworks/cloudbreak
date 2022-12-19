package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

class KnoxGatewayServiceRdsConfigProviderTest {

    private KnoxGatewayServiceRdsConfigProvider underTest = new KnoxGatewayServiceRdsConfigProvider();

    @Test
    public void testKnoxGatewayServiceRdsConfigProvider() {
        ReflectionTestUtils.setField(underTest, "port", "5432");
        ReflectionTestUtils.setField(underTest, "userName", "knox_gateway");
        ReflectionTestUtils.setField(underTest, "db", "knox_gateway");

        assertThat(underTest.getDb()).isEqualTo("knox_gateway");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("knox_gateway");
        assertThat(underTest.getPillarKey()).isEqualTo("knox_gateway");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.KNOX_GATEWAY);
    }

}