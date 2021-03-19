package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.EventAssertionCommon;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

@Component
public class FreeIpaListStructuredEventAssertions {

    private static final List<String> FREEIPA_CREATE_FLOW_STATES = Arrays.asList(
            "INIT_STATE",
            "BOOTSTRAPPING_MACHINES_STATE",
            "COLLECTING_HOST_METADATA_STATE",
            "FREEIPA_INSTALL_STATE",
            "CLUSTERPROXY_UPDATE_REGISTRATION_STATE",
            "FREEIPA_POST_INSTALL_STATE",
            "FREEIPA_PROVISION_FINISHED_STATE"
    );

    private static final List<String> FREEIPA_CREATE_REST_STATES = Collections.singletonList(
            "post-freeipa"
    );

    private static final List<String> FREEIPA_STOP_FLOW_STATES = Arrays.asList(
            "INIT_STATE",
            "STOP_STATE",
            "STOP_FINISHED_STATE"
    );

    private static final List<String> FREEIPA_STOP_REST_STATES = Collections.singletonList(
            "put-freeipa-stop"
    );

    private static final List<String> FREEIPA_START_FLOW_STATES = Arrays.asList(
            "INIT_STATE",
            "START_STATE",
            "COLLECTING_METADATA",
            "START_FINISHED_STATE"
    );

    private static final List<String> FREEIPA_START_REST_STATES = Collections.singletonList(
            "put-freeipa-start"
    );

    private static final List<String> FREEIPA_DELETE_FLOW_STATES = Arrays.asList(
            "INIT_STATE",
            "DEREGISTER_CLUSTERPROXY_STATE",
            "DEREGISTER_CCMKEY_STATE",
            "STOP_TELEMETRY_AGENT_STATE",
            "REMOVE_MACHINE_USER_STATE",
            "TERMINATION_STATE",
            "TERMINATION_FINISHED_STATE"
    );

    @Inject
    private EventAssertionCommon eventAssertionCommon;

    public FreeIpaTestDto checkCreateEvents(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkFlowEvents(auditEvents, FREEIPA_CREATE_FLOW_STATES);
        eventAssertionCommon.checkRestEvents(auditEvents, FREEIPA_CREATE_REST_STATES);
        return testDto;
    }

    public FreeIpaTestDto checkDeleteEvents(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkFlowEvents(auditEvents, FREEIPA_DELETE_FLOW_STATES);
        eventAssertionCommon.checkRestEvents(auditEvents, Collections.singletonList("delete-freeipa"));
        return testDto;
    }

    public FreeIpaTestDto checkStopEvents(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkFlowEvents(auditEvents, FREEIPA_STOP_FLOW_STATES);
        eventAssertionCommon.checkRestEvents(auditEvents, FREEIPA_STOP_REST_STATES);
        return testDto;
    }

    public FreeIpaTestDto checkStartEvents(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkFlowEvents(auditEvents, FREEIPA_START_FLOW_STATES);
        eventAssertionCommon.checkRestEvents(auditEvents, FREEIPA_START_REST_STATES);
        return testDto;
    }
}
