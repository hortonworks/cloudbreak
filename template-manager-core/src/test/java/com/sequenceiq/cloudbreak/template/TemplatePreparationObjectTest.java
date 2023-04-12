package com.sequenceiq.cloudbreak.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemplatePreparationObjectTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private static final String TEST_PLATFORM_VARIANT = "AWS_VARIANT";

    @Test
    void getRdsSslCertificateFilePathTestWhenFilePathAbsent() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .build();

        assertThat(tpo.getRdsSslCertificateFilePath()).isNull();
    }

    @Test
    void getRdsSslCertificateFilePathTestWhenFilePathPresent() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .build();

        assertThat(tpo.getRdsSslCertificateFilePath()).isEqualTo(SSL_CERTS_FILE_PATH);
    }

    @Test
    void getPlatformVariantTest() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withPlatformVariant(TEST_PLATFORM_VARIANT)
                .build();

        assertThat(tpo.getPlatformVariant()).isEqualTo(TEST_PLATFORM_VARIANT);
    }

}