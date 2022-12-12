package com.sequenceiq.redbeams.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SslContentTest {

    @Test
    public void testSslContent() {
        SslContent sslContent = new SslContent();
        sslContent.setCert("cert");
        sslContent.setName("name");

        assertThat(sslContent.getCert()).isEqualTo("cert");
        assertThat(sslContent.getName()).isEqualTo("name");
    }
}