package com.sequenceiq.it.cloudbreak.assertion.stack;

import static java.lang.String.format;

import java.util.List;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class EncryptionProfileAssertion {

    private static final String CM_SETTINGS = "/etc/cloudera-scm-server/cm.settings";

    private static final String CM_DEFAULT_SERVER = "/etc/default/cloudera-scm-server";

    private static final String NGINX_SSL_CONF = "/etc/nginx/sites-enabled/ssl.conf";

    private static final String NGINX_SSL_USER_FACING_CONF = "/etc/nginx/sites-enabled/ssl-user-facing.conf";

    private static final String POSTGRES_OPENSSL_CONF = "/etc/pki/tls/postgres-openssl.cnf";

    private static final String POSTGRES_SYSTEMD_DROPIN = "/etc/systemd/system/postgresql-17.service.d/openssl.conf";

    private static final String TLS_1_3_CIPHER = "TLS_AES_256_GCM_SHA384";

    private static final String TLS_1_2_CIPHER = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";

    private static final String TLS_1_2_AES256_CIPHER = "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384";

    private static final String TLS_1_2_OPENSSL_CIPHER = "ECDHE-RSA-AES128-GCM-SHA256";

    private static final String TLS_1_2_AES256_OPENSSL_CIPHER = "ECDHE-RSA-AES256-GCM-SHA384";

    private static final String CM_SUPPORTED_TLS_1_3_REGEX = "SUPPORTED_TLS_VERSIONS\\s* TLSv1.3";

    private static final String CM_SUPPORTED_TLS_1_2_REGEX = "SUPPORTED_TLS_VERSIONS\\s* TLSv1.2";

    private static final String CM_SUPPORTED_TLS_1_2_1_3_REGEX = "SUPPORTED_TLS_VERSIONS\\s* TLSv1.2,TLSv1.3";

    private static final String CM_TLS_1_3_CIPHER_REGEX = "tls_ciphers\\s* " + TLS_1_3_CIPHER;

    private static final String CM_TLS_1_2_CIPHER_REGEX = "tls_ciphers\\s* " + TLS_1_2_CIPHER;

    private static final String CM_TLS_1_2_1_3_CIPHERS_REGEX = "tls_ciphers\\s* " + TLS_1_3_CIPHER + "," + TLS_1_2_CIPHER;

    private static final String CM_TLS_1_2_AES256_1_3_CIPHERS_REGEX = "tls_ciphers\\s* " + TLS_1_3_CIPHER + "," + TLS_1_2_AES256_CIPHER + "," + TLS_1_2_CIPHER;

    private static final String NGINX_TLS_1_3_REGEX = "ssl_protocols\\s*TLSv1.3";

    private static final String NGINX_TLS_1_2_REGEX = "ssl_protocols\\s*TLSv1.2";

    private static final String NGINX_TLS_1_2_1_3_REGEX = "ssl_protocols\\s*TLSv1.2\\s*TLSv1.3";

    private static final String NGINX_TLS_1_3_CIPHERSUITES_REGEX = "ssl_conf_command Ciphersuites\\s*" + TLS_1_3_CIPHER;

    private static final String NGINX_SSL_CIPHERS_TLS_1_2_REGEX = "ssl_ciphers\\s*" + TLS_1_2_OPENSSL_CIPHER;

    private static final String NGINX_SSL_CIPHERS_TLS_1_2_MIXED_REGEX = "ssl_ciphers\\s*" + TLS_1_2_OPENSSL_CIPHER;

    private static final String NGINX_SSL_CIPHERS_TLS_1_2_AES256_REGEX = "ssl_ciphers\\s*" + TLS_1_2_AES256_OPENSSL_CIPHER + ":" + TLS_1_2_OPENSSL_CIPHER;

    private static final String POSTGRES_TLS_1_3_CIPHER_REGEX = "Ciphersuites\\s*=\\s*" + TLS_1_3_CIPHER;

    private static final String POSTGRES_TLS_1_3_MIN_PROTOCOL_REGEX = "MinProtocol\\s*=\\s*TLSv1.3";

    private static final String POSTGRES_TLS_1_2_MIN_PROTOCOL_REGEX = "MinProtocol\\s*=\\s*TLSv1.2";

    private static final String POSTGRES_SYSTEMD_OPENSSL_CONF_REGEX = "Environment=OPENSSL_CONF=" + POSTGRES_OPENSSL_CONF;

    private static final String CM_DELETE_SUPPORTED_TLS_VERSIONS_REGEX = "deletesettings SUPPORTED_TLS_VERSIONS";

    private static final String CM_DELETE_TLS_CIPHERS_REGEX = "deletesettings tls_ciphers";

    private static final String CM_SET_SUPPORTED_TLS_VERSIONS_REGEX = "setsettings SUPPORTED_TLS_VERSIONS";

    private static final String CM_TLS_ADVANCED_CONTROL_REGEX = "CMF_FF_TLS_ADVANCED_CONTROL";

    private static final String CM_OVERRIDE_TLS_CIPHERS_REGEX = "CMF_OVERRIDE_TLS_CIPHERS";

    private static final String NGINX_DEFAULT_TLS_REGEX = "ssl_protocols\\s*TLSv1.2";

    private static final String NGINX_ALTERNATIVE_USER_FACING_CERT_REGEX = "alternative-server.pem";

    private static final String NGINX_CUSTOM_TLS_1_3_CIPHERSUITES_REGEX = NGINX_TLS_1_3_CIPHERSUITES_REGEX + ";";

    private static final String NGINX_CUSTOM_SSL_CIPHERS_TLS_1_2_REGEX = NGINX_SSL_CIPHERS_TLS_1_2_REGEX + ";";

    private static final String NGINX_CUSTOM_SSL_CIPHERS_TLS_1_2_AES256_REGEX = NGINX_SSL_CIPHERS_TLS_1_2_AES256_REGEX + ";";

    private static final List<String> NGINX_CUSTOM_CIPHERS_REGEXES = List.of(NGINX_CUSTOM_TLS_1_3_CIPHERSUITES_REGEX,
            NGINX_CUSTOM_SSL_CIPHERS_TLS_1_2_REGEX, NGINX_CUSTOM_SSL_CIPHERS_TLS_1_2_AES256_REGEX);

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

    public FreeIpaTestDto assertTls12Aes256Tls13EncryptionProfile(FreeIpaTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_2_1_3_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_SSL_CIPHERS_TLS_1_2_AES256_REGEX);
        return testDto;
    }

    public SdxTestDto assertTls12Aes256Tls13EncryptionProfile(SdxTestDto testDto) {
        assertTls12Tls13Stack((file, regex) -> stackAssertion.validateFileContentExists(testDto, file, regex),
                CM_TLS_1_2_AES256_1_3_CIPHERS_REGEX, NGINX_SSL_CIPHERS_TLS_1_2_AES256_REGEX);
        return testDto;
    }

    public DistroXTestDto assertTls12Aes256Tls13EncryptionProfile(DistroXTestDto testDto) {
        assertTls12Tls13Stack((file, regex) -> stackAssertion.validateFileContentExists(testDto, file, regex),
                CM_TLS_1_2_AES256_1_3_CIPHERS_REGEX, NGINX_SSL_CIPHERS_TLS_1_2_AES256_REGEX);
        return testDto;
    }

    public DistroXTestDto assertTls12Tls13EncryptionProfile(DistroXTestDto testDto) {
        assertTls12Tls13Stack((file, regex) -> stackAssertion.validateFileContentExists(testDto, file, regex),
                CM_TLS_1_2_1_3_CIPHERS_REGEX, NGINX_SSL_CIPHERS_TLS_1_2_MIXED_REGEX);
        return testDto;
    }

    private void assertTls12Tls13Stack(BiConsumer<String, String> fileContentValidator, String cmCiphersRegex, String nginxCiphersRegex) {
        fileContentValidator.accept(CM_SETTINGS, CM_SUPPORTED_TLS_1_2_1_3_REGEX);
        fileContentValidator.accept(CM_SETTINGS, cmCiphersRegex);
        fileContentValidator.accept(NGINX_SSL_CONF, NGINX_TLS_1_2_1_3_REGEX);
        fileContentValidator.accept(NGINX_SSL_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        fileContentValidator.accept(NGINX_SSL_CONF, nginxCiphersRegex);
        fileContentValidator.accept(NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_2_1_3_REGEX);
        fileContentValidator.accept(NGINX_SSL_USER_FACING_CONF, NGINX_TLS_1_3_CIPHERSUITES_REGEX);
        fileContentValidator.accept(NGINX_SSL_USER_FACING_CONF, nginxCiphersRegex);
        fileContentValidator.accept(POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_3_CIPHER_REGEX);
        fileContentValidator.accept(POSTGRES_OPENSSL_CONF, POSTGRES_TLS_1_2_MIN_PROTOCOL_REGEX);
        fileContentValidator.accept(POSTGRES_SYSTEMD_DROPIN, POSTGRES_SYSTEMD_OPENSSL_CONF_REGEX);
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

    public FreeIpaTestDto assertEncryptionProfileDisabled(FreeIpaTestDto testDto) {
        stackAssertion.validateFileContentExists(testDto, NGINX_SSL_CONF, NGINX_DEFAULT_TLS_REGEX);
        NGINX_CUSTOM_CIPHERS_REGEXES.forEach(regex -> stackAssertion.validateFileContentDoesNotExist(testDto, NGINX_SSL_CONF, regex));
        return testDto;
    }

    public SdxTestDto assertEncryptionProfileDisabled(SdxTestDto testDto) {
        assertEncryptionProfileDisabledOnStack((file, regex) -> stackAssertion.validateFileContentExists(testDto, file, regex),
                (file, regex) -> stackAssertion.validateFileContentDoesNotExist(testDto, file, regex));
        return testDto;
    }

    public DistroXTestDto assertEncryptionProfileDisabled(DistroXTestDto testDto) {
        assertEncryptionProfileDisabledOnStack((file, regex) -> stackAssertion.validateFileContentExists(testDto, file, regex),
                (file, regex) -> stackAssertion.validateFileContentDoesNotExist(testDto, file, regex));
        return testDto;
    }

    private void assertEncryptionProfileDisabledOnStack(BiConsumer<String, String> fileContentExistsValidator,
            BiConsumer<String, String> fileContentDoesNotExistValidator) {
        fileContentExistsValidator.accept(CM_SETTINGS, CM_DELETE_SUPPORTED_TLS_VERSIONS_REGEX);
        fileContentExistsValidator.accept(CM_SETTINGS, CM_DELETE_TLS_CIPHERS_REGEX);
        fileContentDoesNotExistValidator.accept(CM_SETTINGS, CM_SET_SUPPORTED_TLS_VERSIONS_REGEX);
        fileContentExistsValidator.accept(CM_DEFAULT_SERVER, CM_OVERRIDE_TLS_CIPHERS_REGEX);
        fileContentDoesNotExistValidator.accept(CM_DEFAULT_SERVER, CM_TLS_ADVANCED_CONTROL_REGEX);
        fileContentExistsValidator.accept(NGINX_SSL_CONF, NGINX_DEFAULT_TLS_REGEX);
        fileContentExistsValidator.accept(NGINX_SSL_USER_FACING_CONF, NGINX_DEFAULT_TLS_REGEX);
        fileContentDoesNotExistValidator.accept(NGINX_SSL_USER_FACING_CONF, NGINX_ALTERNATIVE_USER_FACING_CERT_REGEX);
        NGINX_CUSTOM_CIPHERS_REGEXES.forEach(regex -> {
            fileContentDoesNotExistValidator.accept(NGINX_SSL_CONF, regex);
            fileContentDoesNotExistValidator.accept(NGINX_SSL_USER_FACING_CONF, regex);
        });
    }

    public EnvironmentTestDto assertEncryptionProfileCrnEmpty(EnvironmentTestDto testDto) {
        assertEncryptionProfileCrnEmpty("Environment", testDto.getName(), testDto.getResponse().getEncryptionProfileCrn());
        return testDto;
    }

    public SdxTestDto assertEncryptionProfileCrnEmpty(SdxTestDto testDto) {
        assertEncryptionProfileCrnEmpty("Data Lake", testDto.getName(),
                testDto.getResponse().getStackV4Response().getCluster().getEncryptionProfileCrn());
        return testDto;
    }

    public DistroXTestDto assertEncryptionProfileCrnEmpty(DistroXTestDto testDto) {
        assertEncryptionProfileCrnEmpty("Data Hub", testDto.getName(), testDto.getResponse().getCluster().getEncryptionProfileCrn());
        return testDto;
    }

    public DistroXTestDto assertEncryptionProfileCrn(DistroXTestDto testDto, String expectedEncryptionProfileCrn) {
        String encryptionProfileCrn = testDto.getResponse().getCluster().getEncryptionProfileCrn();
        if (!StringUtils.equals(expectedEncryptionProfileCrn, encryptionProfileCrn)) {
            throw new TestFailException(format("Data Hub '%s' has encryption profile '%s' but '%s' was expected",
                    testDto.getName(), encryptionProfileCrn, expectedEncryptionProfileCrn));
        }
        return testDto;
    }

    private void assertEncryptionProfileCrnEmpty(String resourceType, String resourceName, String encryptionProfileCrn) {
        if (StringUtils.isNotBlank(encryptionProfileCrn)) {
            throw new TestFailException(format("%s '%s' still has encryption profile '%s' after disabling it",
                    resourceType, resourceName, encryptionProfileCrn));
        }
    }
}
