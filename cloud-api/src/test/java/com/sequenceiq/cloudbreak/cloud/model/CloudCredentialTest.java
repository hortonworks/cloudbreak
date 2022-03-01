package com.sequenceiq.cloudbreak.cloud.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class CloudCredentialTest {

    private static final String CONFIDENTIAL = "confidential";

    private static final String SECRET = "mySecret";

    @Test
    void toStringTestShouldNotIncludeSecrets() {
        CloudCredential underTest = new CloudCredential("id", "name", Map.of(CONFIDENTIAL, SECRET), true);
        String result = underTest.toString();

        assertThat(underTest.getStringParameter(CONFIDENTIAL)).isEqualTo(SECRET);
        assertThat(result).doesNotContain("DynamicModel");
        assertThat(result).doesNotContain(CONFIDENTIAL);
        assertThat(result).doesNotContain(SECRET);
    }

}