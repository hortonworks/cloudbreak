package com.sequenceiq.cloudbreak.cloud.model.secret;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;

class CloudSecretTest {

    private static final EncryptionKeySource KEY_SOURCE = EncryptionKeySource.builder().build();

    private static final Instant DELETION_DATE = Instant.EPOCH;

    private static final Map<String, String> TAGS = Map.of();

    private static final List<String> CRYPTOGRAPHIC_PRINCIPALS = List.of("principal");

    private static final List<String> CRYPTOGRAPHIC_AUTHORIZED_CLIENTS = List.of("client");

    private static final String SECRET_ID = "secretId";

    private static final String SECRET_NAME = "secretName";

    private static final String DESCRIPTION = "description";

    private static final String SECRET_VALUE = "secretValue";

    @Test
    void builderTest() {
        CloudSecret underTest = CloudSecret.builder()
                .withSecretId(SECRET_ID)
                .withSecretName(SECRET_NAME)
                .withDescription(DESCRIPTION)
                .withSecretValue(SECRET_VALUE)
                .withKeySource(KEY_SOURCE)
                .withDeletionDate(DELETION_DATE)
                .withTags(TAGS)
                .withCryptographicPrincipals(CRYPTOGRAPHIC_PRINCIPALS)
                .withCryptographicAuthorizedClients(CRYPTOGRAPHIC_AUTHORIZED_CLIENTS)
                .build();

        assertThat(underTest.secretId()).isEqualTo(SECRET_ID);
        assertThat(underTest.secretName()).isEqualTo(SECRET_NAME);
        assertThat(underTest.description()).isEqualTo(DESCRIPTION);
        assertThat(underTest.secretValue()).isEqualTo(SECRET_VALUE);
        assertThat(underTest.keySource()).isSameAs(KEY_SOURCE);
        assertThat(underTest.deletionDate()).isSameAs(DELETION_DATE);
        assertThat(underTest.tags()).isSameAs(TAGS);
        assertThat(underTest.cryptographicPrincipals()).isSameAs(CRYPTOGRAPHIC_PRINCIPALS);
        assertThat(underTest.cryptographicAuthorizedClients()).isSameAs(CRYPTOGRAPHIC_AUTHORIZED_CLIENTS);
    }

    @Test
    void toStringTestShouldNotIncludeSecretValue() {
        CloudSecret underTest = CloudSecret.builder()
                .withSecretId(SECRET_ID)
                .withSecretName(SECRET_NAME)
                .withSecretValue(SECRET_VALUE)
                .build();

        assertThat(underTest.toString()).doesNotContain(SECRET_VALUE);
    }

    @Test
    void toBuilderTest() {
        CloudSecret cloudSecret = CloudSecret.builder()
                .withSecretId(SECRET_ID)
                .withSecretName(SECRET_NAME)
                .withDescription(DESCRIPTION)
                .withSecretValue(SECRET_VALUE)
                .withKeySource(KEY_SOURCE)
                .withDeletionDate(DELETION_DATE)
                .withTags(TAGS)
                .withCryptographicPrincipals(CRYPTOGRAPHIC_PRINCIPALS)
                .withCryptographicAuthorizedClients(CRYPTOGRAPHIC_AUTHORIZED_CLIENTS)
                .build();

        CloudSecret underTest = cloudSecret.toBuilder().build();

        assertThat(underTest).isEqualTo(cloudSecret);
    }

}