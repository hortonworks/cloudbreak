package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

class HueRdsConfigProviderTest {

    private HueRdsConfigProvider underTest = new HueRdsConfigProvider();

    @Test
    public void testKnoxGatewayServiceRdsConfigProvider() {
        ReflectionTestUtils.setField(underTest, "hueDbPort", "5432");
        ReflectionTestUtils.setField(underTest, "hueDbUser", "hue");
        ReflectionTestUtils.setField(underTest, "hueDb", "hue");

        assertThat(underTest.getDb()).isEqualTo("hue");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("hue");
        assertThat(underTest.getPillarKey()).isEqualTo("hue");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.HUE);
    }
}