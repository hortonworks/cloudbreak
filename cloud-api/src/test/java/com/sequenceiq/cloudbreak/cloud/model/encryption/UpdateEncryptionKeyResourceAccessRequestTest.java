package com.sequenceiq.cloudbreak.cloud.model.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

class UpdateEncryptionKeyResourceAccessRequestTest {

    @Test
    void builderTest() {
        CloudContext cloudContext = CloudContext.Builder.builder().build();
        CloudCredential cloudCredential = new CloudCredential();
        CloudResource cloudResource = CloudResource.builder()
                .withReference("reference")
                .withName("name")
                .withType(ResourceType.AWS_KMS_KEY)
                .build();
        List<String> administratorPrincipalsToAdd = List.of("principal1");
        List<String> administratorPrincipalsToRemove = List.of("principal2");
        List<String> cryptographicPrincipalsToAdd = List.of("principal3");
        List<String> cryptographicPrincipalsToRemove = List.of("principal4");
        List<String> cryptographicAuthorizedClientsToAdd = List.of("client1");
        List<String> cryptographicAuthorizedClientsToRemove = List.of("client2");

        UpdateEncryptionKeyResourceAccessRequest underTest = UpdateEncryptionKeyResourceAccessRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResource(cloudResource)
                .withAdministratorPrincipalsToAdd(administratorPrincipalsToAdd)
                .withAdministratorPrincipalsToRemove(administratorPrincipalsToRemove)
                .withCryptographicPrincipalsToAdd(cryptographicPrincipalsToAdd)
                .withCryptographicPrincipalsToRemove(cryptographicPrincipalsToRemove)
                .withCryptographicAuthorizedClientsToAdd(cryptographicAuthorizedClientsToAdd)
                .withCryptographicAuthorizedClientsToRemove(cryptographicAuthorizedClientsToRemove)
                .build();

        assertThat(underTest.cloudContext()).isSameAs(cloudContext);
        assertThat(underTest.cloudCredential()).isSameAs(cloudCredential);
        assertThat(underTest.cloudResource()).isSameAs(cloudResource);
        assertThat(underTest.administratorPrincipalsToAdd()).isSameAs(administratorPrincipalsToAdd);
        assertThat(underTest.administratorPrincipalsToRemove()).isSameAs(administratorPrincipalsToRemove);
        assertThat(underTest.cryptographicPrincipalsToAdd()).isSameAs(cryptographicPrincipalsToAdd);
        assertThat(underTest.cryptographicPrincipalsToRemove()).isSameAs(cryptographicPrincipalsToRemove);
        assertThat(underTest.cryptographicAuthorizedClientsToAdd()).isSameAs(cryptographicAuthorizedClientsToAdd);
        assertThat(underTest.cryptographicAuthorizedClientsToRemove()).isSameAs(cryptographicAuthorizedClientsToRemove);
    }

}