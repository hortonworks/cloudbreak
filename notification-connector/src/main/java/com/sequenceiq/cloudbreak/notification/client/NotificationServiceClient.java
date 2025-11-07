package com.sequenceiq.cloudbreak.notification.client;

import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataRequest;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateAccountMetadataResponse;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListRequest;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListRequest;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.DeleteDistributionListResponse;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.Event;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.EventMessage;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsRequest;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.MessageType;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventRequest;
import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.SeverityType;

import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminGrpc;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusRequest;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.GetPublishedEventStatusResponse;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishedEventTypeDetails;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListDto;

import io.grpc.ManagedChannel;

public class NotificationServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceClient.class);

    private final ManagedChannel channel;

    private final NotificationServiceConfig notificationServiceConfig;

    private final StubProvider stubProvider;

    public NotificationServiceClient(
            ManagedChannel channel,
            NotificationServiceConfig notificationServiceConfig,
            StubProvider stubProvider) {
        this.channel = channel;
        this.notificationServiceConfig = notificationServiceConfig;
        this.stubProvider = stubProvider;
    }

    public PublishTargetedEventResponse publishTargetedEvent(String title, String message,
            SeverityType.Value severity, String resourceCrn, String eventType) {
        // Event Message varies for EMAIL and IN_APP, so we set separate event Messages based on the channel
        EventMessage eventMessageRichText = EventMessage.newBuilder()
                .setMessageType(MessageType.Value.RICH_TEXT)
                .setTitle(title)
                .setDescription(message)
                .build();

        EventMessage eventMessagePlainText = EventMessage.newBuilder()
                .setMessageType(MessageType.Value.PLAIN_TEXT)
                .setTitle(title)
                .setDescription(message)
                .build();

        Event event = Event.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventTypeId(eventType)
                .setEventSeverity(severity)
                .addEventMessages(eventMessagePlainText)
                .addEventMessages(eventMessageRichText)
                .setEventTime(System.currentTimeMillis())
                .setResourceCrn(resourceCrn)
                .build();

        PublishTargetedEventRequest request = PublishTargetedEventRequest.newBuilder().setEvent(event).build();
        PublishTargetedEventResponse response =
                createNotificationServiceAdminStub().publishTargetedEvent(request);
        LOGGER.info("Published targeted event with id: {} for resource: {}", event.getEventId(), resourceCrn);
        return response;
    }

    /**
     * Get the status of a published event.
     *
     * @param eventId the ID of the event to check status for
     * @return the status response from the notification service
     */
    public GetPublishedEventStatusResponse getPublishedEventStatus(String eventId) {
        LOGGER.info("Getting published event status for event ID: {}", eventId);
        GetPublishedEventStatusRequest request =
                GetPublishedEventStatusRequest.newBuilder()
                        .setPublishedEventId(eventId)
                        .build();

        GetPublishedEventStatusResponse response =
                createNotificationServiceAdminStub().getPublishedEventStatus(request);

        LOGGER.info("Retrieved published event status for event ID: {}", eventId);
        return response;
    }

    /**
     * Get the status of a published event using the event type ID and resource CRN.
     *
     * @param eventTypeId the event type ID
     * @param resourceCrn the resource CRN
     * @return the status response from the notification service
     */
    public GetPublishedEventStatusResponse
    getPublishedEventStatus(String eventTypeId, String resourceCrn) {
        LOGGER.info("Getting published event status for event type ID: {} and resource CRN: {}", eventTypeId, resourceCrn);

        PublishedEventTypeDetails publishedEventTypeDetails =
                PublishedEventTypeDetails.newBuilder()
                        .setEventTypeId(eventTypeId)
                        .setResourceCrn(resourceCrn)
                        .build();

        GetPublishedEventStatusRequest request =
                GetPublishedEventStatusRequest.newBuilder()
                        .setPublishedEventTypeDetails(publishedEventTypeDetails)
                        .build();

        GetPublishedEventStatusResponse response =
                createNotificationServiceAdminStub().getPublishedEventStatus(request);

        LOGGER.info("Retrieved published event status for event type ID: {} and resource CRN: {}", eventTypeId, resourceCrn);
        return response;
    }

    public CreateOrUpdateDistributionListResponse
    createOrUpdateDistributionList(CreateOrUpdateDistributionListDto dto) {
        LOGGER.info("Creating or updating distribution list for resource: {}", dto.resourceCrn());

        CreateOrUpdateDistributionListRequest.Builder requestBuilder =
                CreateOrUpdateDistributionListRequest.newBuilder()
                        .setResourceCrn(dto.resourceCrn())
                        .setResourceName(dto.resourceName())
                        .setParentResourceCrn(dto.resourceCrn())
                        .setAccountId(Crn.safeFromString(dto.resourceCrn()).getAccountId())
                        .setDistributionListManagementType(dto.distributionListManagementType());

        if (CollectionUtils.isNotEmpty(dto.eventChannelPreferences())) {
            requestBuilder.addAllEventChannelPreferences(dto.eventChannelPreferences());
        }

        if (CollectionUtils.isNotEmpty(dto.emailAddresses())) {
            requestBuilder.addAllEmailAddresses(dto.emailAddresses());
        }

        if (dto.distributionListId() != null) {
            requestBuilder.setDistributionListId(dto.distributionListId());
        }

        if (dto.parentResourceCrn() != null) {
            requestBuilder.setParentResourceCrn(dto.parentResourceCrn());
        }

        if (CollectionUtils.isNotEmpty(dto.slackChannelIds())) {
            requestBuilder.addAllSlackChannelIds(dto.slackChannelIds());
        }

        CreateOrUpdateDistributionListResponse response =
                createNotificationServiceAdminStub().createOrUpdateDistributionList(requestBuilder.build());

        LOGGER.info("Created or updated distribution list for resource: {}", dto.resourceCrn());
        return response;
    }

    /**
     * Delete a distribution list.
     *
     * @param name the name of the distribution list to delete
     * @return the delete distribution list response
     */
    public DeleteDistributionListResponse
    deleteDistributionList(String name) {

        DeleteDistributionListRequest request =
                DeleteDistributionListRequest.newBuilder()
                        .setDistributionListId(name)
                        .build();

        DeleteDistributionListResponse response =
                createNotificationServiceAdminStub().deleteDistributionList(request);

        LOGGER.info("Deleted distribution list with name: {}", name);
        return response;
    }

    public ListDistributionListsResponse
    listDistributionLists(String resourceCrn) {

        ListDistributionListsRequest request =
                ListDistributionListsRequest.newBuilder()
                        .setResourceCrn(resourceCrn)
                        .setAccountId(Crn.safeFromString(resourceCrn).getAccountId())
                        .build();

        ListDistributionListsResponse response =
                createNotificationServiceAdminStub().listDistributionLists(request);

        LOGGER.info("Listed distribution lists for resource CRN: {}", resourceCrn);
        return response;
    }

    /**
     * Create or update account metadata.
     *
     * @param accountId the account ID for which to create or update metadata
     * @param allowedDomains list of allowed domains for the account
     * @return the service response
     */
    public CreateOrUpdateAccountMetadataResponse createOrUpdateAccountMetadata(
            String accountId, List<String> allowedDomains) {
        LOGGER.info("Creating or updating account metadata for account: {} with {} allowed domains", accountId, allowedDomains.size());

        CreateOrUpdateAccountMetadataRequest request =
                CreateOrUpdateAccountMetadataRequest.newBuilder()
                        .setAccountId(accountId)
                        .addAllAllowedDomains(allowedDomains)
                        .build();

        CreateOrUpdateAccountMetadataResponse response =
                createNotificationServiceAdminStub().createOrUpdateAccountMetadata(request);

        LOGGER.info("Account metadata create/update completed for account: {}", accountId);
        return response;
    }

    private NotificationAdminGrpc.NotificationAdminBlockingStub createNotificationServiceAdminStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return stubProvider.newInternalAdminStub(channel, requestId,
                notificationServiceConfig.getGrpcTimeoutSec(), notificationServiceConfig.internalCrnForIamServiceAsString(),
                notificationServiceConfig.getCallingServiceName());
    }
}
