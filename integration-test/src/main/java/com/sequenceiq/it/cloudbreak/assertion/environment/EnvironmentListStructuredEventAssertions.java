package com.sequenceiq.it.cloudbreak.assertion.environment;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CLUSTER_DEFINITION_DELETE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CREATION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_DATABASE_DELETION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_DATAHUB_CLUSTERS_DELETION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_DATALAKE_CLUSTERS_DELETION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_DELETION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_FREEIPA_DELETION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_IDBROKER_MAPPINGS_DELETION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_INITIALIZATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_NETWORK_CREATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_NETWORK_DELETION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_PUBLICKEY_CREATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_PUBLICKEY_DELETION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_S3GUARD_TABLE_DELETION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_START_DATAHUB_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_START_DATALAKE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_START_FREEIPA_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_START_SYNCHRONIZE_USERS_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_STOPPED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_STOP_DATAHUB_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_STOP_DATALAKE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_STOP_FREEIPA_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_UMS_RESOURCE_DELETION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_VALIDATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_RESOURCE_ENCRYPTION_DELETION_STARTED;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.assertion.EventAssertionCommon;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Component
public class EnvironmentListStructuredEventAssertions {

    private static final List<String> ENV_CREATE_FLOW_STATES = Arrays.asList(
            "INIT_STATE",
            "ENVIRONMENT_INITIALIZATION_STATE",
            "ENVIRONMENT_CREATION_VALIDATION_STATE",
            "NETWORK_CREATION_STARTED_STATE",
            "PUBLICKEY_CREATION_STARTED_STATE",
            "ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED_STATE",
            "FREEIPA_CREATION_STARTED_STATE",
            "ENV_CREATION_FINISHED_STATE"
    );

    private static final List<String> ENV_CREATE_REST_STATES = Collections.singletonList(
            "post-environment"
    );

    private static final List<ResourceEvent> ENV_CREATE_NOTIFICATION_EVENTS = Arrays.asList(
            ENVIRONMENT_INITIALIZATION_STARTED,
            ENVIRONMENT_VALIDATION_STARTED,
            ENVIRONMENT_NETWORK_CREATION_STARTED,
            ENVIRONMENT_PUBLICKEY_CREATION_STARTED,
            ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED,
            ENVIRONMENT_FREEIPA_CREATION_STARTED,
            ENVIRONMENT_CREATION_FINISHED
    );

    private static final List<String> ENV_STOP_FLOW_STATES = Arrays.asList(
            "INIT_STATE",
            "STOP_DATAHUB_STATE",
            "STOP_DATALAKE_STATE",
            "STOP_FREEIPA_STATE",
            "ENV_STOP_FINISHED_STATE"
    );

    private static final List<String> ENV_STOP_REST_STATES = Collections.singletonList(
            "post-environment-stop"
    );

    private static final List<ResourceEvent> ENV_STOP_NOTIFICATION_EVENTS = Arrays.asList(
            ENVIRONMENT_STOP_DATAHUB_STARTED,
            ENVIRONMENT_STOP_DATALAKE_STARTED,
            ENVIRONMENT_STOP_FREEIPA_STARTED,
            ENVIRONMENT_STOPPED
    );

    private static final List<String> ENV_START_FLOW_STATES = Arrays.asList(
            "INIT_STATE",
            "START_DATAHUB_STATE",
            "START_DATALAKE_STATE",
            "START_FREEIPA_STATE",
            "SYNCHRONIZE_USERS_STATE",
            "ENV_START_FINISHED_STATE"
    );

    private static final List<String> ENV_START_REST_STATES = Collections.singletonList(
            "post-environment-start"
    );

    private static final List<ResourceEvent> ENV_START_NOTIFICATION_EVENTS = Arrays.asList(
            ENVIRONMENT_START_DATAHUB_STARTED,
            ENVIRONMENT_START_DATALAKE_STARTED,
            ENVIRONMENT_START_FREEIPA_STARTED,
            ENVIRONMENT_START_SYNCHRONIZE_USERS_STARTED
    );

    private static final List<String> ENV_DELETE_FLOW_STATES = Arrays.asList(
            "INIT_STATE",
            "FREEIPA_DELETE_STARTED_STATE",
            "RDBMS_DELETE_STARTED_STATE",
            "ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_STARTED_STATE",
            "PUBLICKEY_DELETE_STARTED_STATE",
            "NETWORK_DELETE_STARTED_STATE",
            "IDBROKER_MAPPINGS_DELETE_STARTED_STATE",
            "S3GUARD_TABLE_DELETE_STARTED_STATE",
            "CLUSTER_DEFINITION_DELETE_STARTED_STATE",
            "UMS_RESOURCE_DELETE_STARTED_STATE",
            "ENV_DELETE_FINISHED_STATE"
    );

    private static final List<ResourceEvent> ENV_DELETE_NOTIFICATION_EVENTS = Arrays.asList(
            ENVIRONMENT_NETWORK_DELETION_STARTED,
            ENVIRONMENT_PUBLICKEY_DELETION_STARTED,
            ENVIRONMENT_CLUSTER_DEFINITION_DELETE_STARTED,
            ENVIRONMENT_DATABASE_DELETION_STARTED,
            ENVIRONMENT_FREEIPA_DELETION_STARTED,
            ENVIRONMENT_IDBROKER_MAPPINGS_DELETION_STARTED,
            ENVIRONMENT_S3GUARD_TABLE_DELETION_STARTED,
            ENVIRONMENT_UMS_RESOURCE_DELETION_STARTED,
            ENVIRONMENT_DATAHUB_CLUSTERS_DELETION_STARTED,
            ENVIRONMENT_DATALAKE_CLUSTERS_DELETION_STARTED,
            ENVIRONMENT_DELETION_FINISHED,
            ENVIRONMENT_RESOURCE_ENCRYPTION_DELETION_STARTED
    );

    @Inject
    private EventAssertionCommon eventAssertionCommon;

    public EnvironmentTestDto checkCreateEvents(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkFlowEvents(auditEvents, ENV_CREATE_FLOW_STATES);
        eventAssertionCommon.checkNotificationEvents(auditEvents, ENV_CREATE_NOTIFICATION_EVENTS);
        eventAssertionCommon.checkRestEvents(auditEvents, ENV_CREATE_REST_STATES);
        return testDto;
    }

    public EnvironmentTestDto checkDeleteEvents(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkFlowEvents(auditEvents, ENV_DELETE_FLOW_STATES);
        eventAssertionCommon.checkNotificationEvents(auditEvents, ENV_DELETE_NOTIFICATION_EVENTS);
        eventAssertionCommon.checkRestEvents(auditEvents, Collections.singletonList("delete-environment-" + testDto.getCrn()));
        return testDto;
    }

    public EnvironmentTestDto checkStopEvents(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkFlowEvents(auditEvents, ENV_STOP_FLOW_STATES);
        eventAssertionCommon.checkNotificationEvents(auditEvents, ENV_STOP_NOTIFICATION_EVENTS);
        eventAssertionCommon.checkRestEvents(auditEvents, ENV_STOP_REST_STATES);
        return testDto;
    }

    public EnvironmentTestDto checkStartEvents(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) {
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient()
                .structuredEventsV1Endpoint()
                .getAuditEvents(testDto.getCrn(), Collections.emptyList(), 0, 100);
        eventAssertionCommon.checkFlowEvents(auditEvents, ENV_START_FLOW_STATES);
        eventAssertionCommon.checkNotificationEvents(auditEvents, ENV_START_NOTIFICATION_EVENTS);
        eventAssertionCommon.checkRestEvents(auditEvents, ENV_START_REST_STATES);
        return testDto;
    }
}
