package com.sequenceiq.cloudbreak.notification.client;

import static com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ListDistributionListsResponse;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.CreateOrUpdateDistributionListResponse;
import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.PublishTargetedEventResponse;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.notification.client.converter.CreateOrUpdateDistributionListResponseConverter;
import com.sequenceiq.cloudbreak.notification.client.converter.EventChannelPreferenceDtoConverter;
import com.sequenceiq.cloudbreak.notification.client.converter.GetPublishedEventStatusResponseConverter;
import com.sequenceiq.cloudbreak.notification.client.converter.ListDistributionListsResponseConverter;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateAccountMetadataDto;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListDto;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListRequestDto;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListResponseDto;
import com.sequenceiq.cloudbreak.notification.client.dto.DeleteDistributionListRequestDto;
import com.sequenceiq.cloudbreak.notification.client.dto.ListDistributionListsRequestDto;
import com.sequenceiq.cloudbreak.notification.client.dto.ListDistributionListsResponseDto;
import com.sequenceiq.cloudbreak.notification.client.dto.PublishEventForResourceRequestDto;

import io.grpc.ManagedChannel;

@Component
public class GrpcNotificationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcNotificationClient.class);

    @Qualifier("notificationManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private NotificationServiceConfig notificationServiceConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private StubProvider stubProvider;

    @Inject
    private CreateOrUpdateDistributionListResponseConverter createOrUpdateConverter;

    @Inject
    private ListDistributionListsResponseConverter listDistributionListsConverter;

    @Inject
    private GetPublishedEventStatusResponseConverter getPublishedEventStatusResponseConverter;

    @Inject
    private EventChannelPreferenceDtoConverter eventChannelPreferenceDtoConverter;

    public String publishEventForResource(PublishEventForResourceRequestDto request) {
        NotificationServiceClient serviceClient = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Publishing notification {} for environment [{}]",
                request.title(),
                request.resourceCrn());
        PublishTargetedEventResponse response = serviceClient.publishTargetedEvent(
                request.title(),
                request.message(),
                request.severity(),
                request.resourceCrn(),
                request.eventType());
        String eventId = response.getEventId();
        LOGGER.debug("Notification published with event ID [{}] for environment [{}]", eventId, request.resourceCrn());
        return eventId;
    }

    /**
     * Create or update a distribution list.
     *
     * @param request the request containing all parameters for creating or updating a distribution list
     * @return the create or update distribution list response
     */
    public CreateOrUpdateDistributionListResponseDto createOrUpdateDistributionList(CreateOrUpdateDistributionListRequestDto request) {

        NotificationServiceClient serviceClient = makeClient(channelWrapper.getChannel());

        LOGGER.debug("Creating or updating distribution list for resource [{}]", request.getResourceCrn());

        CreateOrUpdateDistributionListDto dto = new CreateOrUpdateDistributionListDto(
                request.getResourceCrn(),
                request.getResourceName(),
                request.getEventChannelPreferences().stream()
                        .map(e -> eventChannelPreferenceDtoConverter.convertToProto(e))
                        .collect(Collectors.toList()),
                request.getEmailAddresses(),
                request.getDistributionListId(),
                request.getParentResourceCrn(),
                request.getSlackChannelIds(),
                request.getDistributionListManagementType()
        );

        CreateOrUpdateDistributionListResponse response = serviceClient.createOrUpdateDistributionList(dto);

        LOGGER.debug("Created or updated distribution list for resource [{}] with ID [{}]",
                request.getResourceCrn(), response);

        return createOrUpdateConverter.convert(response);
    }

    /**
     * Delete a distribution list.
     *
     * @param request the request containing distribution list ID to delete
     * @return the delete distribution list response
     */
    public void deleteDistributionList(DeleteDistributionListRequestDto request) {
        NotificationServiceClient serviceClient = makeClient(channelWrapper.getChannel());

        LOGGER.debug("Deleting distribution list with ID [{}]", request.distributionListId());
        serviceClient.deleteDistributionList(request.distributionListId());
        LOGGER.debug("Deleted distribution list with ID [{}]", request.distributionListId());
    }

    public ListDistributionListsResponseDto listDistributionLists(ListDistributionListsRequestDto request) {
        NotificationServiceClient serviceClient = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Listing distribution lists for resource CRN [{}]", request.resourceCrn());
        ListDistributionListsResponse response =
                serviceClient.listDistributionLists(request.resourceCrn());
        LOGGER.debug("Listed distribution lists for resource CRN [{}], response: {}", request.resourceCrn(), response);
        return listDistributionListsConverter.convert(response);
    }

    public void createOrUpdateAccountMetadata(CreateOrUpdateAccountMetadataDto request) {
        NotificationServiceClient serviceClient = makeClient(channelWrapper.getChannel());
        List<String> allowedDomains = request.allowedDomains() == null ? List.of() : request.allowedDomains();
        LOGGER.debug("Creating or updating account metadata for account [{}] with allowed domains {}", request.accountId(), allowedDomains);
        NotificationAdminProto.CreateOrUpdateAccountMetadataResponse response = serviceClient.createOrUpdateAccountMetadata(
                request.accountId(), allowedDomains);
        LOGGER.debug("Created or updated account metadata for account [{}], response: {}", request.accountId(), response);
    }

    /**
     * Creates a new NotificationServiceClient instance.
     * @return a new NotificationServiceClient instance
     */
    protected NotificationServiceClient makeClient(ManagedChannel channel) {
        return new NotificationServiceClient(
                channel,
                notificationServiceConfig,
                stubProvider);
    }
}
