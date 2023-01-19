package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class PostgresConfigServiceSSLSaltConfigTest {

    @Test
    void defaultConstructorTest() {
        PostgresConfigService.SSLSaltConfig underTest = new PostgresConfigService.SSLSaltConfig();

        assertThat(underTest.getRootCertsBundle()).isEqualTo("");
        assertThat(underTest.isSslEnabled()).isFalse();
        assertThat(underTest.isRestartRequired()).isFalse();
    }

    @Test
    void setRootCertsBundleTestWhenNull() {
        PostgresConfigService.SSLSaltConfig underTest = new PostgresConfigService.SSLSaltConfig();

        assertThrows(NullPointerException.class, () -> underTest.setRootCertsBundle(null));
    }

    @Test
    void toMapTestWhenDefault() {
        PostgresConfigService.SSLSaltConfig underTest = new PostgresConfigService.SSLSaltConfig();

        Map<String, Object> result = underTest.toMap();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Map.ofEntries(entry("ssl_certs", ""), entry("ssl_restart_required", "false"), entry("ssl_enabled", "false")));
    }

    @Test
    void toMapTestWhenSslWithoutRestart() {
        PostgresConfigService.SSLSaltConfig underTest = new PostgresConfigService.SSLSaltConfig();
        underTest.setRestartRequired(false);
        underTest.setSslEnabled(true);
        underTest.setRootCertsBundle("myCert");

        Map<String, Object> result = underTest.toMap();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Map.ofEntries(entry("ssl_certs", "myCert"), entry("ssl_restart_required", "false"), entry("ssl_enabled", "true")));
    }

    @Test
    void toMapTestWhenSslWithRestart() {
        PostgresConfigService.SSLSaltConfig underTest = new PostgresConfigService.SSLSaltConfig();
        underTest.setRestartRequired(true);
        underTest.setSslEnabled(true);
        underTest.setRootCertsBundle("myCert");

        Map<String, Object> result = underTest.toMap();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Map.ofEntries(entry("ssl_certs", "myCert"), entry("ssl_restart_required", "true"), entry("ssl_enabled", "true")));
    }

}
