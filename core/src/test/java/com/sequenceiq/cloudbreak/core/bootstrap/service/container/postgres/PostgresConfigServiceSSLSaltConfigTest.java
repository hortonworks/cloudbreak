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
                entry("ssl_for_cm_db_natively_supported", "false"),
                entry("tls_advanced_control", "false"), entry("tls_min_version", ""), entry("tls_max_version", ""),
                entry("tls12_ciphers", ""), entry("tls13_ciphers", "")));
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
                entry("ssl_for_cm_db_natively_supported", "false"),
                entry("tls_advanced_control", "false"), entry("tls_min_version", ""), entry("tls_max_version", ""),
                entry("tls12_ciphers", ""), entry("tls13_ciphers", "")));
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
                entry("ssl_for_cm_db_natively_supported", "false"),
                entry("tls_advanced_control", "false"), entry("tls_min_version", ""), entry("tls_max_version", ""),
                entry("tls12_ciphers", ""), entry("tls13_ciphers", "")));
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
                entry("ssl_for_cm_db_natively_supported", "true"),
                entry("tls_advanced_control", "false"), entry("tls_min_version", ""), entry("tls_max_version", ""),
                entry("tls12_ciphers", ""), entry("tls13_ciphers", "")));
    }

    @Test
    void testWhenTlsAdvancedControlEnabled() {
        PostgresConfigService.SSLSaltConfig underTest = new PostgresConfigService.SSLSaltConfig();
        underTest.setSslEnabled(true);
        underTest.setRootCertsBundle("myCert");
        underTest.setTlsAdvancedControl(true);
        underTest.setTlsMinVersion("TLSv1.2");
        underTest.setTlsMaxVersion("TLSv1.3");
        underTest.setTls12Ciphers("ECDHE-RSA-AES128-GCM-SHA256");
        underTest.setTls13Ciphers("TLS_AES_256_GCM_SHA384");

        Map<String, Object> result = underTest.toMap();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Map.ofEntries(entry("ssl_certs", "myCert"), entry("ssl_restart_required", "false"), entry("ssl_enabled", "true"),
                entry("ssl_for_cm_db_natively_supported", "false"),
                entry("tls_advanced_control", "true"),
                entry("tls_min_version", "TLSv1.2"),
                entry("tls_max_version", "TLSv1.3"),
                entry("tls12_ciphers", "ECDHE-RSA-AES128-GCM-SHA256"),
                entry("tls13_ciphers", "TLS_AES_256_GCM_SHA384")));
    }

}
