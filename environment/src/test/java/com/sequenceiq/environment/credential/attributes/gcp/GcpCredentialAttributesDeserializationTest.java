package com.sequenceiq.environment.credential.attributes.gcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;

class GcpCredentialAttributesDeserializationTest {

    private static final String MY_PROJECT_ID = "myProjectId";

    @Test
    void legacyP12AttributesStillDeserialize() throws Exception {
        ObjectMapper strictMapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        String legacyAttributes =
                """
                {"gcp":{"json":{"credentialJson":"eyJwcm9qZWN0X2lkIjoibXlQcm9qZWN0SWQifQ==","projectId":"myProjectId"},
                        "p12":{"projectId":"p","serviceAccountId":"s","serviceAccountPrivateKey":"k"}}}""";

        CredentialAttributes attributes = strictMapper.readValue(legacyAttributes, CredentialAttributes.class);

        assertThat(attributes.getGcp()).isNotNull();
        assertThat(attributes.getGcp().getJson()).isNotNull();
        assertThat(attributes.getGcp().getJson().getProjectId()).isEqualTo(MY_PROJECT_ID);
    }
}
