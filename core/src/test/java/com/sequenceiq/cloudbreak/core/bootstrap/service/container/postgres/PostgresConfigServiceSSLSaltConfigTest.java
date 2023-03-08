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
        assertThat(underTest.isSslForCmDbNativelySupported()).isFalse();
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
        assertThat(result).isEqualTo(Map.ofEntries(entry("ssl_certs", ""), entry("ssl_restart_required", "false"), entry("ssl_enabled", "false"),
                entry("ssl_for_cm_db_natively_supported", "false")));
    }

    @Test
    void toMapTestWhenSslWithoutRestart() {
        PostgresConfigService.SSLSaltConfig underTest = new PostgresConfigService.SSLSaltConfig();
        underTest.setRestartRequired(false);
        underTest.setSslEnabled(true);
        underTest.setSslForCmDbNativelySupported(false);
        underTest.setRootCertsBundle("myCert");

        Map<String, Object> result = underTest.toMap();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Map.ofEntries(entry("ssl_certs", "myCert"), entry("ssl_restart_required", "false"), entry("ssl_enabled", "true"),
                entry("ssl_for_cm_db_natively_supported", "false")));
    }

    @Test
    void toMapTestWhenSslWithRestart() {
        PostgresConfigService.SSLSaltConfig underTest = new PostgresConfigService.SSLSaltConfig();
        underTest.setRestartRequired(true);
        underTest.setSslEnabled(true);
        underTest.setSslForCmDbNativelySupported(false);
        underTest.setRootCertsBundle("myCert");

        Map<String, Object> result = underTest.toMap();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Map.ofEntries(entry("ssl_certs", "myCert"), entry("ssl_restart_required", "true"), entry("ssl_enabled", "true"),
                entry("ssl_for_cm_db_natively_supported", "false")));
    }

    @Test
    void toMapTestWhenSslWithCmDbNativeSupport() {
        PostgresConfigService.SSLSaltConfig underTest = new PostgresConfigService.SSLSaltConfig();
        underTest.setRestartRequired(false);
        underTest.setSslEnabled(true);
        underTest.setSslForCmDbNativelySupported(true);
        underTest.setRootCertsBundle("myCert");

        Map<String, Object> result = underTest.toMap();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Map.ofEntries(entry("ssl_certs", "myCert"), entry("ssl_restart_required", "false"), entry("ssl_enabled", "true"),
                entry("ssl_for_cm_db_natively_supported", "true")));
    }

}
