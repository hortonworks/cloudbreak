package com.sequenceiq.cloudbreak.notification.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminGrpc;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.GrpcErrorHandler.NotificationServiceException;
import com.sequenceiq.cloudbreak.notification.client.GrpcErrorHandler.NotificationServiceRateLimitException;
import com.sequenceiq.cloudbreak.notification.client.GrpcErrorHandler.NotificationServiceTimeoutException;
import com.sequenceiq.cloudbreak.notification.client.GrpcErrorHandler.NotificationServiceUnavailableException;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListDto;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceClientTest {

    private static final String EVENT_ID = "event-123";

    private static final String EVENT_TYPE_ID = "event-type-456";

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:account:cluster:resource-123";

    private static final String ACCOUNT_ID = "account";

    private static final String DISTRIBUTION_LIST_ID = "dl-123";

    @Mock
    private ManagedChannel channel;

    @Mock
    private NotificationServiceConfig notificationServiceConfig;

    @Mock
    private StubProvider stubProvider;

    @Mock
    private NotificationAdminGrpc.NotificationAdminBlockingStub stub;

    private NotificationServiceClient underTest;

    @BeforeEach
    void setUp() {
        underTest = new NotificationServiceClient(channel, notificationServiceConfig, stubProvider);
        when(stubProvider.newInternalAdminStub(any(), anyString(), anyLong(), anyString(), anyString())).thenReturn(stub);
        when(notificationServiceConfig.getGrpcTimeoutSec()).thenReturn(120L);
        when(notificationServiceConfig.internalCrnForIamServiceAsString()).thenReturn("internal-crn");
        when(notificationServiceConfig.getCallingServiceName()).thenReturn("test-service");
    }

    @ParameterizedTest
    @EnumSource(ClientMethod.class)
    void allMethodsReturnSuccessfulResponses(ClientMethod method) {
        method.setupSuccess(stub);

        Object result = method.call(underTest);

        assertNotNull(result);
        method.verify(stub);
    }

    @ParameterizedTest
    @MethodSource("provideAllMethodsWithStatusCodes")
    void allMethodsHandleGrpcStatusCodes(ClientMethod method, Status.Code statusCode, Class<? extends RuntimeException> expectedExceptionType) {
        method.setupException(stub, statusCode);

        RuntimeException exception = assertThrows(expectedExceptionType, () -> method.call(underTest));

        assertNotNull(exception);
        assertInstanceOf(StatusRuntimeException.class, exception.getCause());
    }

    private static Stream<Arguments> provideAllMethodsWithStatusCodes() {
        return Stream.of(ClientMethod.values())
                .flatMap(method -> Stream.of(Status.Code.values())
                        .filter(code -> code != Status.Code.OK)
                        .map(statusCode -> Arguments.of(method, statusCode, getExpectedExceptionType(statusCode))));
    }

    private static Class<? extends RuntimeException> getExpectedExceptionType(Status.Code statusCode) {
        return switch (statusCode) {
            case UNAVAILABLE -> NotificationServiceUnavailableException.class;
            case DEADLINE_EXCEEDED -> NotificationServiceTimeoutException.class;
            case RESOURCE_EXHAUSTED -> NotificationServiceRateLimitException.class;
            default -> NotificationServiceException.class;
        };
    }

    enum ClientMethod {
        GET_PUBLISHED_EVENT_STATUS_BY_ID {
            @Override
            void setupSuccess(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                when(stub.getPublishedEventStatus(any(NotificationAdminProto.GetPublishedEventStatusRequest.class)))
                        .thenReturn(createGetPublishedEventStatusResponse());
            }

            @Override
            void setupException(NotificationAdminGrpc.NotificationAdminBlockingStub stub, Status.Code statusCode) {
                when(stub.getPublishedEventStatus(any(NotificationAdminProto.GetPublishedEventStatusRequest.class)))
                        .thenThrow(new StatusRuntimeException(statusCode.toStatus()));
            }

            @Override
            Object call(NotificationServiceClient client) {
                return client.getPublishedEventStatus(EVENT_ID);
            }

            @Override
            void verify(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                org.mockito.Mockito.verify(stub).getPublishedEventStatus(any(NotificationAdminProto.GetPublishedEventStatusRequest.class));
            }
        },
        GET_PUBLISHED_EVENT_STATUS_BY_TYPE_AND_CRN {
            @Override
            void setupSuccess(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                when(stub.getPublishedEventStatus(any(NotificationAdminProto.GetPublishedEventStatusRequest.class)))
                        .thenReturn(createGetPublishedEventStatusResponse());
            }

            @Override
            void setupException(NotificationAdminGrpc.NotificationAdminBlockingStub stub, Status.Code statusCode) {
                when(stub.getPublishedEventStatus(any(NotificationAdminProto.GetPublishedEventStatusRequest.class)))
                        .thenThrow(new StatusRuntimeException(statusCode.toStatus()));
            }

            @Override
            Object call(NotificationServiceClient client) {
                return client.getPublishedEventStatus(EVENT_TYPE_ID, RESOURCE_CRN);
            }

            @Override
            void verify(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                org.mockito.Mockito.verify(stub).getPublishedEventStatus(any(NotificationAdminProto.GetPublishedEventStatusRequest.class));
            }
        },
        PUBLISH_TARGETED_EVENT {
            @Override
            void setupSuccess(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                when(stub.publishTargetedEvent(any(NotificationAdminProto.PublishTargetedEventRequest.class)))
                        .thenReturn(NotificationAdminProto.PublishTargetedEventResponse.newBuilder().build());
            }

            @Override
            void setupException(NotificationAdminGrpc.NotificationAdminBlockingStub stub, Status.Code statusCode) {
                when(stub.publishTargetedEvent(any(NotificationAdminProto.PublishTargetedEventRequest.class)))
                        .thenThrow(new StatusRuntimeException(statusCode.toStatus()));
            }

            @Override
            Object call(NotificationServiceClient client) {
                return client.publishTargetedEvent("Title", "Message",
                        NotificationAdminProto.SeverityType.Value.INFO, RESOURCE_CRN, "event-type");
            }

            @Override
            void verify(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                org.mockito.Mockito.verify(stub).publishTargetedEvent(any(NotificationAdminProto.PublishTargetedEventRequest.class));
            }
        },
        CREATE_OR_UPDATE_DISTRIBUTION_LIST {
            @Override
            void setupSuccess(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                when(stub.createOrUpdateDistributionList(any(NotificationAdminProto.CreateOrUpdateDistributionListRequest.class)))
                        .thenReturn(NotificationAdminProto.CreateOrUpdateDistributionListResponse.newBuilder().build());
            }

            @Override
            void setupException(NotificationAdminGrpc.NotificationAdminBlockingStub stub, Status.Code statusCode) {
                when(stub.createOrUpdateDistributionList(any(NotificationAdminProto.CreateOrUpdateDistributionListRequest.class)))
                        .thenThrow(new StatusRuntimeException(statusCode.toStatus()));
            }

            @Override
            Object call(NotificationServiceClient client) {
                CreateOrUpdateDistributionListDto dto = new CreateOrUpdateDistributionListDto(
                        RESOURCE_CRN,
                        "test-resource",
                        null,
                        null,
                        null,
                        null,
                        null,
                        NotificationAdminProto.DistributionListManagementType.Value.USER_MANAGED.name());
                return client.createOrUpdateDistributionList(dto);
            }

            @Override
            void verify(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                org.mockito.Mockito.verify(stub).createOrUpdateDistributionList(any(NotificationAdminProto.CreateOrUpdateDistributionListRequest.class));
            }
        },
        DELETE_DISTRIBUTION_LIST {
            @Override
            void setupSuccess(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                when(stub.deleteDistributionList(any(NotificationAdminProto.DeleteDistributionListRequest.class)))
                        .thenReturn(NotificationAdminProto.DeleteDistributionListResponse.newBuilder().build());
            }

            @Override
            void setupException(NotificationAdminGrpc.NotificationAdminBlockingStub stub, Status.Code statusCode) {
                when(stub.deleteDistributionList(any(NotificationAdminProto.DeleteDistributionListRequest.class)))
                        .thenThrow(new StatusRuntimeException(statusCode.toStatus()));
            }

            @Override
            Object call(NotificationServiceClient client) {
                return client.deleteDistributionList(DISTRIBUTION_LIST_ID);
            }

            @Override
            void verify(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                org.mockito.Mockito.verify(stub).deleteDistributionList(any(NotificationAdminProto.DeleteDistributionListRequest.class));
            }
        },
        LIST_DISTRIBUTION_LISTS {
            @Override
            void setupSuccess(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                when(stub.listDistributionLists(any(NotificationAdminProto.ListDistributionListsRequest.class)))
                        .thenReturn(NotificationAdminProto.ListDistributionListsResponse.newBuilder().build());
            }

            @Override
            void setupException(NotificationAdminGrpc.NotificationAdminBlockingStub stub, Status.Code statusCode) {
                when(stub.listDistributionLists(any(NotificationAdminProto.ListDistributionListsRequest.class)))
                        .thenThrow(new StatusRuntimeException(statusCode.toStatus()));
            }

            @Override
            Object call(NotificationServiceClient client) {
                return client.listDistributionLists(RESOURCE_CRN);
            }

            @Override
            void verify(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                org.mockito.Mockito.verify(stub).listDistributionLists(any(NotificationAdminProto.ListDistributionListsRequest.class));
            }
        },
        CREATE_OR_UPDATE_ACCOUNT_METADATA {
            @Override
            void setupSuccess(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                when(stub.createOrUpdateAccountMetadata(any(NotificationAdminProto.CreateOrUpdateAccountMetadataRequest.class)))
                        .thenReturn(NotificationAdminProto.CreateOrUpdateAccountMetadataResponse.newBuilder().build());
            }

            @Override
            void setupException(NotificationAdminGrpc.NotificationAdminBlockingStub stub, Status.Code statusCode) {
                when(stub.createOrUpdateAccountMetadata(any(NotificationAdminProto.CreateOrUpdateAccountMetadataRequest.class)))
                        .thenThrow(new StatusRuntimeException(statusCode.toStatus()));
            }

            @Override
            Object call(NotificationServiceClient client) {
                return client.createOrUpdateAccountMetadata(ACCOUNT_ID, List.of("example.com"));
            }

            @Override
            void verify(NotificationAdminGrpc.NotificationAdminBlockingStub stub) {
                org.mockito.Mockito.verify(stub).createOrUpdateAccountMetadata(any(NotificationAdminProto.CreateOrUpdateAccountMetadataRequest.class));
            }
        };

        abstract void setupSuccess(NotificationAdminGrpc.NotificationAdminBlockingStub stub);

        abstract void setupException(NotificationAdminGrpc.NotificationAdminBlockingStub stub, Status.Code statusCode);

        abstract Object call(NotificationServiceClient client);

        abstract void verify(NotificationAdminGrpc.NotificationAdminBlockingStub stub);
    }

    private static NotificationAdminProto.GetPublishedEventStatusResponse createGetPublishedEventStatusResponse() {
        NotificationAdminProto.PublishedEventStatus eventStatus = NotificationAdminProto.PublishedEventStatus.newBuilder()
                .setPublishedEventId(EVENT_ID)
                .setEventTypeId(EVENT_TYPE_ID)
                .setTitle("Test Event")
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
}