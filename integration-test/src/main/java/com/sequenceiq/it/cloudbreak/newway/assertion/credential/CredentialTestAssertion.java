package com.sequenceiq.it.cloudbreak.newway.assertion.credential;

import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;

public class CredentialTestAssertion {

    private CredentialTestAssertion() {

    }

    public static AssertionV2<CredentialTestDto> validateModifcation(String modifiedDescription) {
        return (testContext, entity, cloudbreakClient) -> {
            Set<CredentialV4Response> collect = entity.getResponses()
                    .stream()
                    .filter(cred -> cred.getName().contentEquals(entity.getName()) && cred.getDescription().contentEquals(modifiedDescription))
                    .collect(Collectors.toSet());


            if (collect.isEmpty()) {
                throw new IllegalArgumentException(String.format("Credential modification did not happened on %s credential",
                        entity.getName()));
            }
            return entity;
        };
    }

    public static AssertionV2<CredentialTestDto> listContains(String credentialName, Integer expectedCount) {
        return (testContext, entity, cloudbreakClient) -> {
            boolean countCorrect = entity.getResponses()
                    .stream()
                    .filter(credentialV4Response -> credentialV4Response.getName().contentEquals(credentialName))
                    .count() == expectedCount;
            if (!countCorrect) {
                throw new IllegalArgumentException("Credential count for " + credentialName + " is not as expected!");
            }
            return entity;
        };
    }
}
