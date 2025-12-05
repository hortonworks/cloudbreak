package com.sequenceiq.cloudbreak.notification.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.notification.client.converter.GetPublishedEventStatusResponseConverter;
import com.sequenceiq.cloudbreak.notification.client.dto.GetPublishedEventStatusResponseDto;

import io.grpc.ManagedChannel;

@ExtendWith(MockitoExtension.class)
class GrpcNotificationClientTest {

    private static final String EVENT_ID = "event-123";

    private static final String EVENT_TYPE_ID = "event-type-456";

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:account:cluster:resource-123";

    private static final String PUBLISHED_EVENT_ID = "published-event-789";

    private static final String TITLE = "Test Event";

    @Mock
    private ManagedChannelWrapper channelWrapper;

    @Mock
    private GetPublishedEventStatusResponseConverter getPublishedEventStatusResponseConverter;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    private GrpcNotificationClient underTest;

    @BeforeEach
    void setUp() throws Exception {
        ManagedChannel channel = mock(ManagedChannel.class);
        when(channelWrapper.getChannel()).thenReturn(channel);

        // Create an anonymous subclass that overrides makeClient to return our mock
        underTest = new GrpcNotificationClient() {
            @Override
            protected NotificationServiceClient makeClient(ManagedChannel ch) {
                return notificationServiceClient;
            }
        };

        // Set required fields using reflection
        setField(underTest, "channelWrapper", channelWrapper);
        setField(underTest, "getPublishedEventStatusResponseConverter", getPublishedEventStatusResponseConverter);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void getPublishedEventStatusByEventIdReturnsConvertedResponse() {
        NotificationAdminProto.GetPublishedEventStatusResponse protoResponse = createProtoResponse();
        GetPublishedEventStatusResponseDto expectedDto = createExpectedDto();

        when(notificationServiceClient.getPublishedEventStatus(EVENT_ID)).thenReturn(protoResponse);
        when(getPublishedEventStatusResponseConverter.convert(protoResponse)).thenReturn(expectedDto);

        GetPublishedEventStatusResponseDto result = underTest.getPublishedEventStatus(EVENT_ID);

        assertNotNull(result);
        assertEquals(PUBLISHED_EVENT_ID, result.publishedEventId());
        assertEquals(TITLE, result.title());
        verify(notificationServiceClient).getPublishedEventStatus(EVENT_ID);
        verify(getPublishedEventStatusResponseConverter).convert(protoResponse);
    }

    @Test
    void getPublishedEventStatusByEventTypeAndResourceCrnReturnsConvertedResponse() {
        NotificationAdminProto.GetPublishedEventStatusResponse protoResponse = createProtoResponse();
        GetPublishedEventStatusResponseDto expectedDto = createExpectedDto();

        when(notificationServiceClient.getPublishedEventStatus(EVENT_TYPE_ID, RESOURCE_CRN)).thenReturn(protoResponse);
        when(getPublishedEventStatusResponseConverter.convert(protoResponse)).thenReturn(expectedDto);

        GetPublishedEventStatusResponseDto result = underTest.getPublishedEventStatus(EVENT_TYPE_ID, RESOURCE_CRN);

        assertNotNull(result);
        assertEquals(PUBLISHED_EVENT_ID, result.publishedEventId());
        assertEquals(RESOURCE_CRN, result.resourceCrn());
        verify(notificationServiceClient).getPublishedEventStatus(EVENT_TYPE_ID, RESOURCE_CRN);
        verify(getPublishedEventStatusResponseConverter).convert(protoResponse);
    }

    @Test
    void getPublishedEventStatusByEventIdCallsServiceClientWithCorrectParameters() {
        NotificationAdminProto.GetPublishedEventStatusResponse protoResponse = createProtoResponse();
        GetPublishedEventStatusResponseDto expectedDto = createExpectedDto();

        when(notificationServiceClient.getPublishedEventStatus(anyString())).thenReturn(protoResponse);
        when(getPublishedEventStatusResponseConverter.convert(any())).thenReturn(expectedDto);

        underTest.getPublishedEventStatus("test-event-id");

        verify(notificationServiceClient).getPublishedEventStatus("test-event-id");
    }

    @Test
    void getPublishedEventStatusByEventTypeAndResourceCrnCallsServiceClientWithCorrectParameters() {
        NotificationAdminProto.GetPublishedEventStatusResponse protoResponse = createProtoResponse();
        GetPublishedEventStatusResponseDto expectedDto = createExpectedDto();

        when(notificationServiceClient.getPublishedEventStatus(anyString(), anyString())).thenReturn(protoResponse);
        when(getPublishedEventStatusResponseConverter.convert(any())).thenReturn(expectedDto);

        underTest.getPublishedEventStatus("test-event-type", "test-resource-crn");

        verify(notificationServiceClient).getPublishedEventStatus("test-event-type", "test-resource-crn");
    }

    @Test
    void getPublishedEventStatusByEventIdConvertsResponseCorrectly() {
        NotificationAdminProto.GetPublishedEventStatusResponse protoResponse = createProtoResponseWithMultipleChannels();
        GetPublishedEventStatusResponseDto expectedDto = createDtoWithMultipleChannels();

        when(notificationServiceClient.getPublishedEventStatus(EVENT_ID)).thenReturn(protoResponse);
        when(getPublishedEventStatusResponseConverter.convert(protoResponse)).thenReturn(expectedDto);

        GetPublishedEventStatusResponseDto result = underTest.getPublishedEventStatus(EVENT_ID);

        assertNotNull(result);
        assertEquals(2, result.channelStatuses().size());
        verify(getPublishedEventStatusResponseConverter).convert(protoResponse);
    }

    private NotificationAdminProto.GetPublishedEventStatusResponse createProtoResponse() {
        NotificationAdminProto.PublishedEventStatus eventStatus = NotificationAdminProto.PublishedEventStatus.newBuilder()
                .setPublishedEventId(PUBLISHED_EVENT_ID)
                .setEventTypeId(EVENT_TYPE_ID)
                .setTitle(TITLE)
                .setResourceCrn(RESOURCE_CRN)
                .setTargetedEventType("TARGETED")
                .setDescription("Test description")
                .setCreatedAt(System.currentTimeMillis())
                .addStatus(NotificationAdminProto.ChannelEventStatus.newBuilder()
                        .setChannelType(NotificationAdminProto.ChannelType.Value.EMAIL)
                        .setEventStatus(NotificationAdminProto.EventStatus.Value.PROCESSED)
                        .build())
                .build();

        return NotificationAdminProto.GetPublishedEventStatusResponse.newBuilder()
                .setPublishedEventStatus(eventStatus)
                .build();
    }

    private NotificationAdminProto.GetPublishedEventStatusResponse createProtoResponseWithMultipleChannels() {
        NotificationAdminProto.PublishedEventStatus eventStatus = NotificationAdminProto.PublishedEventStatus.newBuilder()
                .setPublishedEventId(PUBLISHED_EVENT_ID)
                .setEventTypeId(EVENT_TYPE_ID)
                .setTitle(TITLE)
                .setResourceCrn(RESOURCE_CRN)
                .setTargetedEventType("TARGETED")
                .setDescription("Test description")
                .setCreatedAt(System.currentTimeMillis())
                .addStatus(NotificationAdminProto.ChannelEventStatus.newBuilder()
                        .setChannelType(NotificationAdminProto.ChannelType.Value.EMAIL)
                        .setEventStatus(NotificationAdminProto.EventStatus.Value.PROCESSED)
                        .build())
                .addStatus(NotificationAdminProto.ChannelEventStatus.newBuilder()
                        .setChannelType(NotificationAdminProto.ChannelType.Value.IN_APP)
                        .setEventStatus(NotificationAdminProto.EventStatus.Value.UNPROCESSED)
                        .build())
                .build();

        return NotificationAdminProto.GetPublishedEventStatusResponse.newBuilder()
                .setPublishedEventStatus(eventStatus)
                .build();
    }

    private GetPublishedEventStatusResponseDto createExpectedDto() {
        return new GetPublishedEventStatusResponseDto(
                PUBLISHED_EVENT_ID,
                EVENT_TYPE_ID,
                TITLE,
                RESOURCE_CRN,
                "TARGETED",
                "Test description",
                List.of(new GetPublishedEventStatusResponseDto.ChannelStatusDto("EMAIL", "PROCESSED")),
                System.currentTimeMillis()
        );
    }

    private GetPublishedEventStatusResponseDto createDtoWithMultipleChannels() {
        return new GetPublishedEventStatusResponseDto(
                PUBLISHED_EVENT_ID,
                EVENT_TYPE_ID,
                TITLE,
                RESOURCE_CRN,
                "TARGETED",
                "Test description",
                List.of(
                        new GetPublishedEventStatusResponseDto.ChannelStatusDto("EMAIL", "PROCESSED"),
                        new GetPublishedEventStatusResponseDto.ChannelStatusDto("IN_APP", "UNPROCESSED")
                ),
                System.currentTimeMillis()
        );
    }
}

