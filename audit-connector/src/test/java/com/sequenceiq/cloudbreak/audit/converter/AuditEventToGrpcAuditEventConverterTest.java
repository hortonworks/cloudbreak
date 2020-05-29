package com.sequenceiq.cloudbreak.audit.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ActorBase;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;
import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.AuditEvent;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.audit.model.EventData;
import com.sequenceiq.cloudbreak.audit.model.ServiceEventData;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

class AuditEventToGrpcAuditEventConverterTest {

    private static final String UUID_ID = "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String ACCOUNT_ID = "accountId";

    private static final String REQUEST_ID = "requestId";

    private static final String SOURCE_IP = "1.2.3.4";

    private static final String EVENT_NAME = "CreateDatahubCluster";

    private static final String EVENT_SOURCE = "environments";

    private static final String ACTOR_SERVICE_NAME = "datalake";

    private static final String SERVICE_EVENT_VERSION = "version";

    private static final String SERVICE_EVENT_DETAILS = "{\"param\":\"value\"}";

    private static final boolean MUTATING = true;

    private static final String API_VERSION = "apiVersion";

    private static final String REQUEST_PARAMETERS = "requestParameters";

    private static final String USER_AGENT = "userAgent";

    private final AuditEventToGrpcAuditEventConverter underTest = new AuditEventToGrpcAuditEventConverter();

    @Test
    void testPreventPossibleNullValuesInSouceServiceEventData() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        EventData eventData = ServiceEventData.builder().build();
        AuditEvent source = makeMinimalAuditEvent(actor, eventData);
        underTest.convert(source);
    }

    @Test
    void testPreventPossibleNullValuesInSouceApiRequestData() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        EventData eventData = ApiRequestData.builder().build();
        AuditEvent source = makeMinimalAuditEvent(actor, eventData);
        underTest.convert(source);
    }

    @Test
    void convertActorCrnNoEventData() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        AuditEvent source = makeAuditEvent(actor, null);

        AuditProto.AuditEvent target = underTest.convert(source);
        assertGeneric(target);
        assertThat(target.getActorCase()).isEqualTo(AuditProto.AuditEvent.ActorCase.ACTORCRN);
        assertThat(target.getActorCrn()).isEqualTo(USER_CRN);
        assertThat(target.getEventTypeCase()).isEqualTo(AuditProto.AuditEvent.EventTypeCase.EVENTTYPE_NOT_SET);
    }

    @Test
    void convertNoEventData() {
        ActorBase actor = ActorService.builder().withActorServiceName(ACTOR_SERVICE_NAME).build();
        AuditEvent source = makeAuditEvent(actor, null);

        AuditProto.AuditEvent target = underTest.convert(source);
        assertGeneric(target);
        assertThat(target.getActorCase()).isEqualTo(AuditProto.AuditEvent.ActorCase.ACTORSERVICENAME);
        assertThat(target.getActorServiceName()).isEqualTo(ACTOR_SERVICE_NAME);
        assertThat(target.getEventTypeCase()).isEqualTo(AuditProto.AuditEvent.EventTypeCase.EVENTTYPE_NOT_SET);
    }

    @Test
    void convertWithServiceEventData() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        EventData eventData = ServiceEventData.builder()
                .withVersion(SERVICE_EVENT_VERSION)
                .withEventDetails(SERVICE_EVENT_DETAILS)
                .build();
        AuditEvent source = makeAuditEvent(actor, eventData);

        AuditProto.AuditEvent target = underTest.convert(source);
        assertGeneric(target);
        assertThat(target.getEventTypeCase()).isEqualTo(AuditProto.AuditEvent.EventTypeCase.SERVICEEVENTDATA);
        assertThat(target.getServiceEventData().getDetailsVersion()).isEqualTo(SERVICE_EVENT_VERSION);
        assertThat(target.getServiceEventData().getEventDetails()).isEqualTo(SERVICE_EVENT_DETAILS);
    }

    @Test
    void convertWithApiRequestData() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        EventData eventData = ApiRequestData.builder()
                .withApiVersion(API_VERSION)
                .withMutating(MUTATING)
                .withRequestParameters(REQUEST_PARAMETERS)
                .withUserAgent(USER_AGENT)
                .build();
        AuditEvent source = makeAuditEvent(actor, eventData);

        AuditProto.AuditEvent target = underTest.convert(source);
        assertGeneric(target);
        assertThat(target.getEventTypeCase()).isEqualTo(AuditProto.AuditEvent.EventTypeCase.APIREQUESTDATA);
        assertThat(target.getApiRequestData().getApiVersion()).isEqualTo(API_VERSION);
        assertThat(target.getApiRequestData().getMutating()).isEqualTo(MUTATING);
        assertThat(target.getApiRequestData().getRequestParameters()).isEqualTo(REQUEST_PARAMETERS);
        assertThat(target.getApiRequestData().getUserAgent()).isEqualTo(USER_AGENT);
    }

    @Test
    void convertUnknownEventDataThrows() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();

        class Unknown extends EventData {
        }

        AuditEvent source = makeAuditEvent(actor, new Unknown());

        assertThatThrownBy(() -> underTest.convert(source)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertUnknownActorThrows() {

        class Unknown extends ActorBase {
        }

        AuditEvent source = makeAuditEvent(new Unknown(), null);

        assertThatThrownBy(() -> underTest.convert(source)).isInstanceOf(IllegalArgumentException.class);
    }

    private void assertGeneric(AuditProto.AuditEvent target) {
        assertThat(target.getId()).isEqualTo(UUID_ID);
        assertThat(target.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(target.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(target.getEventName()).isEqualTo(EVENT_NAME);
        assertThat(target.getEventSource()).isEqualTo(EVENT_SOURCE);
        assertThat(target.getSourceIPAddress()).isEqualTo(SOURCE_IP);
    }

    private AuditEvent makeAuditEvent(ActorBase actor, EventData eventData) {
        return AuditEvent.builder()
                .withId(UUID_ID)
                .withAccountId(ACCOUNT_ID)
                .withRequestId(REQUEST_ID)
                .withEventName(AuditEventName.CREATE_DATAHUB_CLUSTER)
                .withEventSource(Crn.Service.ENVIRONMENTS)
                .withSourceIp(SOURCE_IP)
                .withActor(actor)
                .withEventData(eventData)
                .build();
    }

    private AuditEvent makeMinimalAuditEvent(ActorBase actor, EventData eventData) {
        return AuditEvent.builder()
                .withAccountId(ACCOUNT_ID)
                .withEventName(AuditEventName.CREATE_DATAHUB_CLUSTER)
                .withEventSource(Crn.Service.ENVIRONMENTS)
                .withActor(actor)
                .withEventData(eventData)
                .build();
    }
}
