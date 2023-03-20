package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;

public class EfmRdsConfigProviderTest {

    private static final String EFM = "efm";

    private static final String PORT = "5432";

    private EfmRdsConfigProvider efmRdsConfigProvider;

    @BeforeEach
    public void setup() {
        efmRdsConfigProvider = new EfmRdsConfigProvider();
    }

    @Test
    public void testEfmServiceRdsConfigProvider() {
        ReflectionTestUtils.setField(efmRdsConfigProvider, "efmDbPort", PORT);
        ReflectionTestUtils.setField(efmRdsConfigProvider, "efmDbUser", EFM);
        ReflectionTestUtils.setField(efmRdsConfigProvider, "efmDb", EFM);

        assertThat(efmRdsConfigProvider.getDb()).isEqualTo(EFM);
        assertThat(efmRdsConfigProvider.getDbPort()).isEqualTo(PORT);
        assertThat(efmRdsConfigProvider.getDbUser()).isEqualTo(EFM);
        assertThat(efmRdsConfigProvider.getPillarKey()).isEqualTo(EFM);
        assertThat(efmRdsConfigProvider.getRdsType()).isEqualTo(DatabaseType.EFM);
    }
}
