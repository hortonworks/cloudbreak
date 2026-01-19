package com.sequenceiq.it.cloudbreak.assertion.ldap;

import static com.sequenceiq.it.cloudbreak.util.StructuredEventUtil.getAuditEvents;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.assertion.EventAssertionCommon;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

@Component
public class LdapListStructuredEventAssertions {

    @Inject
    private EventAssertionCommon eventAssertionCommon;

    public LdapTestDto checkCreateEvents(TestContext testContext, LdapTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = getAuditEvents(
                client.getDefaultClient(testContext).structuredEventsV1Endpoint(),
                testDto.getCrn());
        eventAssertionCommon.noRestEventsAreAllowedInDB(auditEvents);
        return testDto;
    }

    public LdapTestDto checkDeleteEvents(TestContext testContext, LdapTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = getAuditEvents(
                client.getDefaultClient(testContext).structuredEventsV1Endpoint(),
                testDto.getCrn());
        eventAssertionCommon.noRestEventsAreAllowedInDB(auditEvents);
        return testDto;
    }
}
