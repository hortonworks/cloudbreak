package com.sequenceiq.it.cloudbreak.util.gcp.client;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.asset.v1.AssetServiceClient;
import com.google.cloud.asset.v1.AssetServiceSettings;

class GcpAssetClientLinkageTest {

    @Test
    void testAssetServiceClientClassesLinkCleanly() {
        assertThatCode(() -> {
            AssetServiceSettings settings = AssetServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(
                            GoogleCredentials.create(new AccessToken("fake-token-for-linkage-check", null))))
                    .build();
            try (AssetServiceClient client = AssetServiceClient.create(settings)) {
            }
        }).doesNotThrowAnyException();
    }
}
