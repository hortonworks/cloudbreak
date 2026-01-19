package com.sequenceiq.it.cloudbreak.assertion.kerberos;

import static com.sequenceiq.it.cloudbreak.util.StructuredEventUtil.getAuditEvents;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.assertion.EventAssertionCommon;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

@Component
public class KerberosConfigListStructuredEventAssertions {

    @Inject
    private EventAssertionCommon eventAssertionCommon;

    public KerberosTestDto checkDeleteEvents(TestContext testContext, KerberosTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = getAuditEvents(
                client.getDefaultClient(testContext).structuredEventsV1Endpoint(),
                testDto.getCrn());
        eventAssertionCommon.noRestEventsAreAllowedInDB(auditEvents);
        return testDto;
    }
}
