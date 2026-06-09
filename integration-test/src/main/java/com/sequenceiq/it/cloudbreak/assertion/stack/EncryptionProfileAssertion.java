package com.sequenceiq.it.cloudbreak.assertion.stack;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

@Component
public class EncryptionProfileAssertion {

    private static final String CM_SETTINGS = "/etc/cloudera-scm-server/cm.settings";

    private static final String NGINX_SSL_CONF = "/etc/nginx/sites-enabled/ssl.conf";

    private static final String NGINX_SSL_USER_FACING_CONF = "/etc/nginx/sites-enabled/ssl-user-facing.conf";

    private static final String POSTGRES_OPENSSL_CONF = "/etc/pki/tls/postgres-openssl.cnf";

    private static final String POSTGRES_SYSTEMD_DROPIN = "/etc/systemd/system/postgresql-17.service.d/openssl.conf";

    private static final String TLS_1_3_CIPHER = "TLS_AES_256_GCM_SHA384";

    private static final String TLS_1_2_CIPHER = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";

    private static final String TLS_1_2_OPENSSL_CIPHER = "ECDHE-RSA-AES128-GCM-SHA256";

    private static final String CM_SUPPORTED_TLS_1_3_REGEX = "SUPPORTED_TLS_VERSIONS\\s* TLSv1.3";

    private static final String CM_SUPPORTED_TLS_1_2_REGEX = "SUPPORTED_TLS_VERSIONS\\s* TLSv1.2";

    private static final String CM_SUPPORTED_TLS_1_2_1_3_REGEX = "SUPPORTED_TLS_VERSIONS\\s* TLSv1.2,TLSv1.3";

    private static final String CM_TLS_1_3_CIPHER_REGEX = "tls_ciphers\\s* " + TLS_1_3_CIPHER;

    private static final String CM_TLS_1_2_CIPHER_REGEX = "tls_ciphers\\s* " + TLS_1_2_CIPHER;

    private static final String CM_TLS_1_2_1_3_CIPHERS_REGEX = "tls_ciphers\\s* " + TLS_1_3_CIPHER + "," + TLS_1_2_CIPHER;

    private static final String NGINX_TLS_1_3_REGEX = "ssl_protocols\\s*TLSv1.3";

    private static final String NGINX_TLS_1_2_REGEX = "ssl_protocols\\s*TLSv1.2";

    private static final String NGINX_TLS_1_2_1_3_REGEX = "ssl_protocols\\s*TLSv1.2\\s*TLSv1.3";

    private static final String NGINX_TLS_1_3_CIPHERSUITES_REGEX = "ssl_conf_command Ciphersuites\\s*" + TLS_1_3_CIPHER;

    private static final String NGINX_SSL_CIPHERS_TLS_1_2_REGEX = "ssl_ciphers\\s*" + TLS_1_2_OPENSSL_CIPHER;

    private static final String NGINX_SSL_CIPHERS_TLS_1_2_MIXED_REGEX = "ssl_ciphers\\s*" + TLS_1_2_OPENSSL_CIPHER;

    private static final String POSTGRES_TLS_1_3_CIPHER_REGEX = "Ciphersuites\\s*=\\s*" + TLS_1_3_CIPHER;

    private static final String POSTGRES_TLS_1_3_MIN_PROTOCOL_REGEX = "MinProtocol\\s*=\\s*TLSv1.3";

    private static final String POSTGRES_TLS_1_2_MIN_PROTOCOL_REGEX = "MinProtocol\\s*=\\s*TLSv1.2";

    private static final String POSTGRES_SYSTEMD_OPENSSL_CONF_REGEX = "Environment=OPENSSL_CONF=" + POSTGRES_OPENSSL_CONF;

    @Inject
    private StackAssertion stackAssertion;

    public FreeIpaTestDto assertTls13EncryptionProfile(FreeIpaTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        return testDto;
    }

    public SdxTestDto assertTls13EncryptionProfile(SdxTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_TLS_1_3_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_3_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_3_MIN_PROTOCOL_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_SYSTEMD_DROPIN, POSTGRES_SYSTEMD_OPENSSL_CONF_REGEX);
        return testDto;
    }

    public DistroXTestDto assertTls13EncryptionProfile(DistroXTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_TLS_1_3_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_3_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_3_MIN_PROTOCOL_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_SYSTEMD_DROPIN, POSTGRES_SYSTEMD_OPENSSL_CONF_REGEX);
        return testDto;
    }

    public FreeIpaTestDto assertTls12EncryptionProfile(FreeIpaTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_SSL_CIPHERS_TLS_1_2_REGEX);
        return testDto;
    }

    public SdxTestDto assertTls12EncryptionProfile(SdxTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_TLS_1_2_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_SSL_CIPHERS_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_SSL_CIPHERS_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_2_MIN_PROTOCOL_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_SYSTEMD_DROPIN, POSTGRES_SYSTEMD_OPENSSL_CONF_REGEX);
        return testDto;
    }

    public DistroXTestDto assertTls12EncryptionProfile(DistroXTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_TLS_1_2_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_SSL_CIPHERS_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_SSL_CIPHERS_TLS_1_2_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_2_MIN_PROTOCOL_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_SYSTEMD_DROPIN, POSTGRES_SYSTEMD_OPENSSL_CONF_REGEX);
        return testDto;
    }

    public DistroXTestDto assertTls12Tls13EncryptionProfile(DistroXTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_TLS_1_2_1_3_CIPHERS_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_SSL_CIPHERS_TLS_1_2_MIXED_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_SSL_CIPHERS_TLS_1_2_MIXED_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_3_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_2_MIN_PROTOCOL_REGEX);
        stackAssertion.validateFileContentExists(testDto, POSTGRES_SYSTEMD_DROPIN, POSTGRES_SYSTEMD_OPENSSL_CONF_REGEX);
        return testDto;
    }

    public FreeIpaTestDto assertDefaultTls12Tls13EncryptionProfile(FreeIpaTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_1_3_REGEX);
        return testDto;
    }

    public SdxTestDto assertDefaultTls12Tls13EncryptionProfile(SdxTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_2_1_3_REGEX);
        return testDto;
    }

    public DistroXTestDto assertDefaultTls12Tls13EncryptionProfile(DistroXTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_2_1_3_REGEX);
        return testDto;
    }
}
