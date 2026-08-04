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

    private static final String TLS_1_3_CIPHER = "TLS_AES_256_GCM_SHA384";

    private static final String CM_SUPPORTED_TLS_1_3_REGEX = "SUPPORTED_TLS_VERSIONS\\s* TLSv1.3";

    private static final String CM_SUPPORTED_TLS_1_2_1_3_REGEX = "SUPPORTED_TLS_VERSIONS\\s* TLSv1.2,TLSv1.3";

    private static final String CM_TLS_CIPHER_REGEX = "tls_ciphers\\s* " + TLS_1_3_CIPHER;

    private static final String NGINX_TLS_1_3_REGEX = "ssl_protocols\\s*TLSv1.3";

    private static final String NGINX_TLS_1_2_1_3_REGEX = "ssl_protocols\\s*TLSv1.2\\s*TLSv1.3";

    private static final String NGINX_TLS_1_3_CIPHER_REGEX = "ssl_conf_command Ciphersuites\\s*" + TLS_1_3_CIPHER;

    @Inject
    private StackAssertion stackAssertion;

    public SdxTestDto assertTls13EncryptionProfile(SdxTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_TLS_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_CIPHER_REGEX);
        return testDto;
    }

    public DistroXTestDto assertTls13EncryptionProfile(DistroXTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_TLS_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_CIPHER_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_CIPHER_REGEX);
        return testDto;
    }

    public FreeIpaTestDto assertTls13EncryptionProfile(FreeIpaTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_CIPHER_REGEX);
        return testDto;
    }

    public SdxTestDto assertDefaultEncryptionProfile(SdxTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_2_1_3_REGEX);
        return testDto;
    }

    public DistroXTestDto assertDefaultEncryptionProfile(DistroXTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, CM_SETTINGS, CM_SUPPORTED_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_2_1_3_REGEX);
        return testDto;
    }

    public FreeIpaTestDto assertDefaultEncryptionProfile(FreeIpaTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_1_3_REGEX);
        return testDto;
    }
}
