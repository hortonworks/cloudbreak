package com.sequenceiq.cloudbreak.cloud.model.secret.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

class UpdateCloudSecretResourceAccessRequestTest {

    private static final List<String> CRYPTOGRAPHIC_PRINCIPALS = List.of("principal");

    private static final List<String> CRYPTOGRAPHIC_AUTHORIZED_CLIENTS = List.of("client");

    private CloudContext cloudContext;

    private CloudCredential cloudCredential;

    private CloudResource cloudResource;

    @BeforeEach
    void setUp() {
        cloudContext = CloudContext.Builder.builder().build();
        cloudCredential = new CloudCredential();
        cloudResource = CloudResource.builder()
                .withReference("reference")
                .withName("name")
                .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                .build();
    }

    @Test
    void builderTest() {
        UpdateCloudSecretResourceAccessRequest underTest = UpdateCloudSecretResourceAccessRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResource(cloudResource)
                .withCryptographicPrincipals(CRYPTOGRAPHIC_PRINCIPALS)
                .withCryptographicAuthorizedClients(CRYPTOGRAPHIC_AUTHORIZED_CLIENTS)
                .build();

        assertThat(underTest.cloudContext()).isSameAs(cloudContext);
        assertThat(underTest.cloudCredential()).isSameAs(cloudCredential);
        assertThat(underTest.cloudResource()).isSameAs(cloudResource);
        assertThat(underTest.cryptographicPrincipals()).isSameAs(CRYPTOGRAPHIC_PRINCIPALS);
        assertThat(underTest.cryptographicAuthorizedClients()).isSameAs(CRYPTOGRAPHIC_AUTHORIZED_CLIENTS);
    }

    @Test
    void toBuilderTest() {
        UpdateCloudSecretResourceAccessRequest request = UpdateCloudSecretResourceAccessRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResource(cloudResource)
                .withCryptographicPrincipals(CRYPTOGRAPHIC_PRINCIPALS)
                .withCryptographicAuthorizedClients(CRYPTOGRAPHIC_AUTHORIZED_CLIENTS)
                .build();

        UpdateCloudSecretResourceAccessRequest underTest = request.toBuilder().build();

        assertThat(underTest).isEqualTo(request);
    }

}