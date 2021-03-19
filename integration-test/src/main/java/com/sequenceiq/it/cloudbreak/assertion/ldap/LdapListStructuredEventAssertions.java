package com.sequenceiq.it.cloudbreak.assertion.ldap;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.EventAssertionCommon;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;

@Component
public class LdapListStructuredEventAssertions {

    @Inject
    private EventAssertionCommon eventAssertionCommon;

    public LdapTestDto checkCreateEvents(TestContext testContext, LdapTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkRestEvents(auditEvents, Collections.singletonList("post-ldap"));
        return testDto;
    }

    public LdapTestDto checkDeleteEvents(TestContext testContext, LdapTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkRestEvents(auditEvents, Collections.singletonList("delete-ldap"));
        return testDto;
    }
}
