package com.sequenceiq.cloudbreak.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemplatePreparationObjectTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

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

}