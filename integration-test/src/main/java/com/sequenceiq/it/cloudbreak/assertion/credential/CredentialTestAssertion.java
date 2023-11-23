package com.sequenceiq.it.cloudbreak.assertion.credential;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.EventAssertionCommon;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

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
}
