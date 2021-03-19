package com.sequenceiq.it.cloudbreak.assertion.kerberos;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.EventAssertionCommon;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;

@Component
public class KerberosConfigListStructuredEventAssertions {

    @Inject
    private EventAssertionCommon eventAssertionCommon;

    public KerberosTestDto checkDeleteEvents(TestContext testContext, KerberosTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkRestEvents(auditEvents, Collections.singletonList("delete-kerberos"));
        return testDto;
    }
}
