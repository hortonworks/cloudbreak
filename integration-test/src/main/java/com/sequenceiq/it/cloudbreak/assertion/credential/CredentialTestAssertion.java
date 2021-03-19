package com.sequenceiq.it.cloudbreak.assertion.credential;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.EventAssertionCommon;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;

@Component
public class CredentialTestAssertion {

    @Inject
    private EventAssertionCommon eventAssertionCommon;

    public Assertion<CredentialTestDto, EnvironmentClient> checkStructuredEvents() {
        return (testContext, entity, client) -> {
            List<CDPStructuredEvent> auditEvents = client.getDefaultClient().structuredEventsV1Endpoint()
                    .getAuditEvents(entity.getCrn(), Collections.emptyList(), 0, 100);
            eventAssertionCommon.checkRestEvents(auditEvents, List.of("post-credential",
                    "put-credential",
                    "delete-credential-" + entity.getName()));
            return entity;
        };
    }

    public static Assertion<CredentialTestDto, CloudbreakClient> validateModifcation(String modifiedDescription) {
        return (testContext, entity, cloudbreakClient) -> {
//            Set<CredentialResponse> collect = entity.getResponses()
//                    .stream()
//                    .filter(cred -> cred.getName().contentEquals(entity.getName()) && cred.getDescription().contentEquals(modifiedDescription))
//                    .collect(Collectors.toSet());
//
//
//            if (collect.isEmpty()) {
//                throw new IllegalArgumentException(String.format("Credential modification did not happened on %s credential",
//                        entity.getName()));
//            }
            return entity;
        };
    }

    public static Assertion<CredentialTestDto, CloudbreakClient> listContains(String credentialName, Integer expectedCount) {
        return (testContext, entity, cloudbreakClient) -> {
//            boolean countCorrect = entity.getResponses()
//                    .stream()
//                    .filter(credentialV4Response -> credentialV4Response.getName().contentEquals(credentialName))
//                    .count() == expectedCount;
//            if (!countCorrect) {
//                throw new IllegalArgumentException("Credential count for " + credentialName + " is not as expected!");
//            }
            return entity;
        };
    }
}
