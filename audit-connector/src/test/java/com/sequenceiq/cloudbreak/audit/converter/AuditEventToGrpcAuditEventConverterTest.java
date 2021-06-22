package com.sequenceiq.cloudbreak.audit.converter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.AuditEventBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.ActorBase;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;
import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.AuditEvent;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.audit.model.EventData;
import com.sequenceiq.cloudbreak.audit.model.ServiceEventData;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

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

    private AuditEventToGrpcAuditEventConverter underTest;

    @Mock
    private AuditEventBuilderUpdater mockAuditEventBuilderUpdater;

    @Mock
    private AuditEventBuilderProvider mockBuilderProvider;

    @Mock
    private AuditProto.AuditEvent.Builder mockAuditEventBuilder;

    @Mock
    private AuditProto.AuditEvent mockAuditEvent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockBuilderProvider.getNewAuditEventBuilder()).thenReturn(mockAuditEventBuilder);
        when(mockAuditEventBuilder.setId(any())).thenReturn(mockAuditEventBuilder);
        when(mockAuditEventBuilder.setTimestamp(anyLong())).thenReturn(mockAuditEventBuilder);
        when(mockAuditEventBuilder.setAccountId(any())).thenReturn(mockAuditEventBuilder);
        when(mockAuditEventBuilder.setRequestId(any())).thenReturn(mockAuditEventBuilder);
        when(mockAuditEventBuilder.setEventName(any())).thenReturn(mockAuditEventBuilder);
        when(mockAuditEventBuilder.setEventSource(any())).thenReturn(mockAuditEventBuilder);
        when(mockAuditEventBuilder.build()).thenReturn(mockAuditEvent);
        underTest = new AuditEventToGrpcAuditEventConverter(new LinkedHashMap<>(), mockBuilderProvider);
    }

    @Test
    void testPreventPossibleNullValuesInSourceServiceEventData() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        EventData eventData = ServiceEventData.builder().build();
        AuditEvent source = makeMinimalAuditEvent(actor, eventData);

        underTest = new AuditEventToGrpcAuditEventConverter(createMockUtilizer(ServiceEventData.class), mockBuilderProvider);

        underTest.convert(source);

        verify(mockAuditEventBuilderUpdater, times(1)).update(any(), any());
    }

    @Test
    void testPreventPossibleNullValuesInSouceApiRequestData() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        EventData eventData = ApiRequestData.builder().build();
        AuditEvent source = makeMinimalAuditEvent(actor, eventData);

        underTest = new AuditEventToGrpcAuditEventConverter(createMockUtilizer(ApiRequestData.class), mockBuilderProvider);

        underTest.convert(source);

        verify(mockAuditEventBuilderUpdater, times(1)).update(any(), any());
    }

    @Test
    void convertActorCrnNoEventData() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        AuditEvent source = makeAuditEvent(actor, null);

        underTest.convert(source);

        assertGeneric();
        verify(mockAuditEventBuilder, times(1)).setActorCrn(any());
        verify(mockAuditEventBuilder, times(1)).setActorCrn(USER_CRN);
    }

    @Test
    void convertNoEventData() {
        ActorBase actor = ActorService.builder().withActorServiceName(ACTOR_SERVICE_NAME).build();
        AuditEvent source = makeAuditEvent(actor, null);

        underTest.convert(source);

        assertGeneric();
    }

    @Test
    void convertWithServiceEventData() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        EventData eventData = ServiceEventData.builder()
                .withVersion(SERVICE_EVENT_VERSION)
                .withEventDetails(SERVICE_EVENT_DETAILS)
                .build();
        AuditEvent source = makeAuditEvent(actor, eventData);

        underTest = new AuditEventToGrpcAuditEventConverter(createMockUtilizer(ServiceEventData.class), mockBuilderProvider);

        underTest.convert(source);

        assertGeneric();
        verify(mockAuditEventBuilderUpdater, times(1)).update(any(), any());
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

        underTest = new AuditEventToGrpcAuditEventConverter(createMockUtilizer(ApiRequestData.class), mockBuilderProvider);

        underTest.convert(source);

        assertGeneric();
        verify(mockAuditEventBuilderUpdater, times(1)).update(any(), any());
    }

    @Test
    void testWhenResultEventDataIsNullThenNoUtilizerCallHappens() {
        ActorBase actor = ActorCrn.builder().withActorCrn(USER_CRN).build();
        AuditEvent source = makeMinimalAuditEvent(actor, null);

        underTest.convert(source);
        verify(mockAuditEventBuilderUpdater, never()).update(any(), any());
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

    private void assertGeneric() {
        verify(mockAuditEventBuilder, times(1)).setId(any());
        verify(mockAuditEventBuilder, times(1)).setId(UUID_ID);

        verify(mockAuditEventBuilder, times(1)).setAccountId(any());
        verify(mockAuditEventBuilder, times(1)).setAccountId(ACCOUNT_ID);

        verify(mockAuditEventBuilder, times(1)).setRequestId(any());
        verify(mockAuditEventBuilder, times(1)).setRequestId(REQUEST_ID);

        verify(mockAuditEventBuilder, times(1)).setEventName(any());
        verify(mockAuditEventBuilder, times(1)).setEventName(EVENT_NAME);

        verify(mockAuditEventBuilder, times(1)).setEventSource(any());
        verify(mockAuditEventBuilder, times(1)).setEventSource(EVENT_SOURCE);

        verify(mockAuditEventBuilder, times(1)).setSourceIPAddress(any());
        verify(mockAuditEventBuilder, times(1)).setSourceIPAddress(SOURCE_IP);

        verify(mockAuditEventBuilder, times(1)).build();
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

    private Map<Class, AuditEventBuilderUpdater> createMockUtilizer(Class clazz) {
        return Map.of(clazz, mockAuditEventBuilderUpdater);
    }

}
