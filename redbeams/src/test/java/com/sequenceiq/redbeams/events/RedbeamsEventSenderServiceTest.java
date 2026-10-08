package com.sequenceiq.redbeams.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.notification.WebSocketNotificationService;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.events.notification.DBEmailDto;
import com.sequenceiq.redbeams.events.notification.DbStackEmailNotificationService;

@ExtendWith(MockitoExtension.class)
class RedbeamsEventSenderServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:acc:databaseServer:res";

    private static final String OWNER_CRN = "crn:cdp:iam:us-west-1:acc:user:bob";

    private static final String CLUSTER_CRN = "crn:cdp:datahub:us-west-1:acc:cluster:dh";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc:environment:env";

    private static final String DB_NAME = "my-external-db";

    private static final Long DB_ID = 42L;

    private static final String NODE_ID = "node-1";

    private static final String SERVICE_VERSION = "1.2.3";

    private static final String LOCALIZED_MESSAGE = "External database my-external-db is low on storage: 7.3% free remaining.";

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private DbStackEmailNotificationService dbStackEmailNotificationService;

    @Captor
    private ArgumentCaptor<CDPStructuredNotificationEvent> structuredEventCaptor;

    private RedbeamsEventSenderService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RedbeamsEventSenderService(webSocketNotificationService, cdpDefaultStructuredEventClient, nodeConfig,
                cloudbreakMessagesService, dbStackEmailNotificationService, SERVICE_VERSION);
        lenient().when(nodeConfig.getId()).thenReturn(NODE_ID);
        lenient().when(cloudbreakMessagesService.getMessage(eq(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW.getMessage()), anyList()))
                .thenReturn(LOCALIZED_MESSAGE);
    }

    private DBStack dbStack() {
        DBStack dbStack = new DBStack();
        dbStack.setId(DB_ID);
        dbStack.setResourceCrn(RESOURCE_CRN);
        dbStack.setName(DB_NAME);
        dbStack.setEnvironmentId(ENV_CRN);
        dbStack.setOwnerCrn(Crn.safeFromString(OWNER_CRN));
        return dbStack;
    }

    private DatabaseServerConfig databaseServerConfig() {
        DatabaseServerConfig config = new DatabaseServerConfig();
        config.setClusterCrn(CLUSTER_CRN);
        return config;
    }

    private Optional<DatabaseServerConfig> optionalDatabaseServerConfig() {
        return Optional.of(databaseServerConfig());
    }

    @Test
    void shouldStoreCdpStructuredNotificationEvent() {
        DBStack dbStack = dbStack();

        underTest.sendStorageLowNotification(optionalDatabaseServerConfig(), dbStack, 7.25d);

        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(structuredEventCaptor.capture());
        CDPStructuredNotificationEvent event = structuredEventCaptor.getValue();

        assertThat(event.getOperation().getEventType()).isEqualTo(StructuredEventType.NOTIFICATION);
        assertThat(event.getOperation().getResourceType()).isEqualTo(CloudbreakEventService.REDBEAMS_RESOURCE_TYPE);
        assertThat(event.getOperation().getResourceId()).isEqualTo(DB_ID);
        assertThat(event.getOperation().getResourceName()).isEqualTo(DB_NAME);
        assertThat(event.getOperation().getResourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(event.getOperation().getEnvironmentCrn()).isEqualTo(ENV_CRN);
        assertThat(event.getOperation().getCloudbreakId()).isEqualTo(NODE_ID);
        assertThat(event.getOperation().getCloudbreakVersion()).isEqualTo(SERVICE_VERSION);
        assertThat(event.getOperation().getResourceEvent()).isEqualTo(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW.name());

        assertThat(event.getNotificationDetails().getResourceEvent()).isEqualTo(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW);
        assertThat(event.getNotificationDetails().getResourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(event.getNotificationDetails().getResourceType()).isEqualTo(CloudbreakEventService.REDBEAMS_RESOURCE_TYPE);

        ExternalDatabaseStorageAlert deserializedPayload = new Gson().fromJson(
                event.getNotificationDetails().getPayload(), ExternalDatabaseStorageAlert.class);
        assertThat(deserializedPayload.resourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(deserializedPayload.freeStoragePercentage()).isEqualTo(7.25d);

        assertThat(event.getStatusReason()).isEqualTo(LOCALIZED_MESSAGE);
    }

    @Test
    void shouldForwardStoredEventToEmailNotificationService() {
        DBStack dbStack = dbStack();
        DatabaseServerConfig config = databaseServerConfig();

        underTest.sendStorageLowNotification(Optional.of(config), dbStack, 7.25d);

        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(structuredEventCaptor.capture());
        verify(dbStackEmailNotificationService).notify(eq(config), eq(dbStack), any(DBEmailDto.class), eq(structuredEventCaptor.getValue()));
    }

    @Test
    void shouldStillSendWebSocketAndEmailWhenStructuredEventStoreFails() {
        DBStack dbStack = dbStack();
        DatabaseServerConfig config = databaseServerConfig();
        doThrow(new RuntimeException("db down")).when(cdpDefaultStructuredEventClient)
                .sendStructuredEvent(any(CDPStructuredNotificationEvent.class));

        assertThatCode(() -> underTest.sendStorageLowNotification(Optional.of(config), dbStack, 7.25d)).doesNotThrowAnyException();

        verify(webSocketNotificationService).send(eq(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW), anyList(), any(), eq(OWNER_CRN), isNull());
        verify(dbStackEmailNotificationService).notify(eq(config), eq(dbStack), any(DBEmailDto.class), any(CDPStructuredNotificationEvent.class));
    }

    @Test
    void shouldSendWebSocketNotificationToOwner() {
        DBStack dbStack = dbStack();

        underTest.sendStorageLowNotification(optionalDatabaseServerConfig(), dbStack, 7.25d);

        ExternalDatabaseStorageAlert expectedPayload = new ExternalDatabaseStorageAlert(RESOURCE_CRN, DB_NAME, ENV_CRN, 7.25d);
        verify(webSocketNotificationService).send(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW, List.of(DB_NAME, String.format("%.1f", 7.25d)),
                expectedPayload, OWNER_CRN, null);
    }

    @Test
    void shouldSwallowWhenWebSocketNotificationFails() {
        DBStack dbStack = dbStack();
        DatabaseServerConfig config = databaseServerConfig();
        doThrow(new RuntimeException("websocket down")).when(webSocketNotificationService).send(any(ResourceEvent.class), anyList(), any(), any(), any());

        assertThatCode(() -> underTest.sendStorageLowNotification(Optional.of(config), dbStack, 7.25d)).doesNotThrowAnyException();

        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(any(CDPStructuredNotificationEvent.class));
        verify(dbStackEmailNotificationService).notify(eq(config), eq(dbStack), any(DBEmailDto.class), any(CDPStructuredNotificationEvent.class));
    }

    @Test
    void shouldSkipEmailButSendOtherNotificationsWhenDatabaseServerConfigIsMissing() {
        DBStack dbStack = dbStack();

        underTest.sendStorageLowNotification(Optional.empty(), dbStack, 7.25d);

        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(any(CDPStructuredNotificationEvent.class));
        verify(webSocketNotificationService).send(eq(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW), anyList(), any(), eq(OWNER_CRN), isNull());
        verifyNoInteractions(dbStackEmailNotificationService);
    }

    @Test
    void shouldSwallowWhenEmailNotificationFails() {
        DBStack dbStack = dbStack();
        doThrow(new RuntimeException("notification down")).when(dbStackEmailNotificationService)
                .notify(any(DatabaseServerConfig.class), any(DBStack.class), any(DBEmailDto.class), any(CDPStructuredNotificationEvent.class));

        assertThatCode(() -> underTest.sendStorageLowNotification(optionalDatabaseServerConfig(), dbStack, 7.25d)).doesNotThrowAnyException();

        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(any(CDPStructuredNotificationEvent.class));
    }
}
