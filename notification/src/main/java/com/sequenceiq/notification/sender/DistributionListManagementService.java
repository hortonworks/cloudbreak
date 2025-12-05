package com.sequenceiq.notification.sender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.UserWithResourceRole;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.notification.client.GrpcNotificationClient;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateAccountMetadataDto;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListRequestDto;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListResponseDto;
import com.sequenceiq.cloudbreak.notification.client.dto.DeleteDistributionListRequestDto;
import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDetailsDto;
import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDto;
import com.sequenceiq.cloudbreak.notification.client.dto.EventChannelPreferenceDto;
import com.sequenceiq.cloudbreak.notification.client.dto.ListDistributionListsRequestDto;
import com.sequenceiq.cloudbreak.notification.client.dto.ListDistributionListsResponseDto;
import com.sequenceiq.notification.config.NotificationConfig;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.DistributionListManagementType;
import com.sequenceiq.notification.domain.EventChannelPreference;
import com.sequenceiq.notification.sender.converter.EventChannelPreferenceToEventChannelPreferenceDtoConverter;
import com.sequenceiq.notification.sender.dto.CreateDistributionListRequest;

@Service
public class DistributionListManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionListManagementService.class);

    private final NotificationConfig config;

    private final GrpcNotificationClient grpcNotificationClient;

    private final GrpcUmsClient grpcUmsClient;

    private final RoleCrnGenerator roleCrnGenerator;

    private final EventChannelPreferenceToEventChannelPreferenceDtoConverter channelPreferenceConverter;

    public DistributionListManagementService(GrpcNotificationClient grpcNotificationClient,
            GrpcUmsClient grpcUmsClient,
            RoleCrnGenerator roleCrnGenerator,
            NotificationConfig config,
            EventChannelPreferenceToEventChannelPreferenceDtoConverter channelPreferenceConverter) {
        this.grpcNotificationClient = grpcNotificationClient;
        this.grpcUmsClient = grpcUmsClient;
        this.roleCrnGenerator = roleCrnGenerator;
        this.config = config;
        this.channelPreferenceConverter = channelPreferenceConverter;
    }

    public List<DistributionList> createOrUpdateLists(Set<CreateDistributionListRequest> requests) {
        LOGGER.debug("Creating or updating distribution lists for resource CRNs {}", requests);
        return requests.stream()
                .map(this::createOrUpdateList)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Optional<DistributionList> createOrUpdateList(CreateDistributionListRequest request) {
        String resourceCrnAsString = request.getResourceCrn();
        Crn resourceCrn = Crn.safeFromString(resourceCrnAsString);
        boolean enabled = config.isEnabled(resourceCrn);
        LOGGER.debug("Creating or updating distribution list enabled: {} with request: {}", enabled, request);
        if (enabled) {
            List<EventChannelPreference> eventChannelPreferences = request.getEventChannelPreferences();
            String accountId = resourceCrn.getAccountId();
            String resourceName = request.getResourceName();
            try {
                List<DistributionList> existingDistributionLists = listDistributionListsForResource(resourceCrnAsString);
                if (hasDistributionListForResource(existingDistributionLists)) {
                    return updateDistributionList(
                            existingDistributionLists,
                            resourceCrn,
                            accountId,
                            resourceCrnAsString,
                            eventChannelPreferences
                    );
                } else {
                    return createDistributionList(
                            accountId,
                            resourceCrnAsString,
                            resourceName,
                            eventChannelPreferences
                    );
                }
            } catch (Exception e) {
                LOGGER.warn("Error during creating or updating distribution list for resourceCrn: {}", resourceCrnAsString, e);
            }
        }
        return Optional.empty();
    }

    private Optional<DistributionList> updateDistributionList(List<DistributionList> existingDistributionLists, Crn resourceCrn, String accountId,
            String resourceCrnAsString, List<EventChannelPreference> eventChannelPreferences) {
        DistributionList existingDistributionList = existingDistributionLists.getFirst();
        if (!userManagedList(existingDistributionList)) {
            LOGGER.warn("Distribution list with id {} and non user-managed type {} exists for resourceCrn: {}, updating it now!",
                    existingDistributionList.getExternalId(),
                    existingDistributionList.getType(),
                    resourceCrn);
            DistributionList distributionList = updateDistributionList(
                    existingDistributionList,
                    getEmailList(getUserWithResourceRoles(accountId, resourceCrnAsString)),
                    channelPreferenceConverter.convert(eventChannelPreferences));
            return Optional.ofNullable(distributionList);
        } else {
            LOGGER.warn("Distribution list with id {} and type {} already exists for resourceCrn: {}, nothing to do..",
                    existingDistributionList.getExternalId(),
                    existingDistributionList.getType(),
                    resourceCrn);
        }
        return Optional.empty();
    }

    private Optional<DistributionList> createDistributionList(String accountId, String resourceCrnAsString, String resourceName,
            List<EventChannelPreference> eventChannelPreferences) {
        // This is a security requirement of Notification Service
        Set<String> emailList = getEmailList(getUserWithResourceRoles(accountId, resourceCrnAsString));
        createAccountMetadataForResource(
                resourceCrnAsString,
                emailList);

        DistributionList distributionList = createDistributionList(
                resourceCrnAsString,
                resourceName,
                emailList,
                channelPreferenceConverter.convert(eventChannelPreferences));
        return Optional.ofNullable(distributionList);
    }

    private boolean hasDistributionListForResource(List<DistributionList> existingDistributionLists) {
        return CollectionUtils.isNotEmpty(existingDistributionLists);
    }

    private boolean userManagedList(DistributionList existingDistributionList) {
        return DistributionListManagementType.USER_MANAGED.equals(existingDistributionList.getType());
    }

    private List<UserWithResourceRole> getUserWithResourceRoles(String accountId, String resourceCrnAsString) {
        return grpcUmsClient.listUsersWithResourceRoles(
                Set.of(
                        roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(accountId),
                        roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(accountId)
                ),
                resourceCrnAsString);
    }

    private Set<String> getEmailList(List<UserWithResourceRole> userWithResourceRoles) {
        Set<String> emailList =
                userWithResourceRoles.stream()
                        .map(UserWithResourceRole::userCrn)
                        .map(e -> e)
                        .collect(Collectors.toSet());
        LOGGER.debug("Extracted email list contains {} emails", emailList.size());
        return emailList;
    }

    public void createAccountMetadataForResource(String resourceCrn, Set<String> emails) {
        if (CollectionUtils.isEmpty(emails)) {
            LOGGER.debug("Email list is empty, skipping account metadata creation for resourceCrn: {}", resourceCrn);
        } else {
            try {
                String accountId = Crn.safeFromString(resourceCrn).getAccountId();
                Set<String> allowedDomains = extractAllowedDomains(emails);
                LOGGER.debug("Ensuring account metadata for accountId: {} with allowed domains: {}", accountId, allowedDomains);
                createAccountMetadata(accountId, List.copyOf(allowedDomains));
            } catch (Exception e) {
                LOGGER.warn("Error during ensuring account metadata for resourceCrn: {}", resourceCrn, e);
            }
        }
    }

    private Set<String> extractAllowedDomains(Set<String> emails) {
        return Optional.ofNullable(emails).orElse(Set.of()).stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(email -> StringUtils.substringAfter(email, "@"))
                .filter(StringUtils::isNotBlank)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private void createAccountMetadata(String accountId, List<String> allowedDomains) {
        grpcNotificationClient.createOrUpdateAccountMetadata(new CreateOrUpdateAccountMetadataDto(accountId, allowedDomains));
    }

    private DistributionList updateDistributionList(DistributionList existingDistributionList,
            Set<String> emailList, List<EventChannelPreferenceDto> eventChannelPreferences) {
        LOGGER.debug("Updating existing distribution list id {} for resourceCrn {}",
                existingDistributionList.getExternalId(), existingDistributionList.getResourceCrn());
        CreateOrUpdateDistributionListRequestDto request = new CreateOrUpdateDistributionListRequestDto();
        request.setResourceCrn(existingDistributionList.getResourceCrn());
        request.setResourceName(existingDistributionList.getResourceName());
        request.setEventChannelPreferences(eventChannelPreferences);
        request.setEmailAddresses(emailList);
        request.setDistributionListId(existingDistributionList.getExternalId());
        request.setDistributionListManagementType(existingDistributionList.getType().name());
        CreateOrUpdateDistributionListResponseDto response = grpcNotificationClient.createOrUpdateDistributionList(request);
        return getList(response, existingDistributionList.getResourceName());
    }

    private DistributionList createDistributionList(String resourceCrn, String resourceName, Set<String> emailList,
            List<EventChannelPreferenceDto> eventChannelPreferences) {
        LOGGER.debug("Creating new distribution list for resourceCrn: {} with {} emails", resourceCrn, emailList.size());
        CreateOrUpdateDistributionListRequestDto request = new CreateOrUpdateDistributionListRequestDto();
        request.setResourceCrn(resourceCrn);
        request.setResourceName(resourceName);
        request.setEventChannelPreferences(eventChannelPreferences);
        request.setEmailAddresses(emailList);
        request.setDistributionListManagementType(DistributionListManagementType.USER_MANAGED.name());
        CreateOrUpdateDistributionListResponseDto response = grpcNotificationClient.createOrUpdateDistributionList(request);
        return getList(response, resourceName);
    }

    public void deleteDistributionList(String resourceCrn) {
        LOGGER.debug("Deleting distribution list(s) for resourceCrn: {} ", resourceCrn);
        List<DistributionList> distributionLists = listDistributionListsForResource(resourceCrn);
        for (DistributionList distributionList : distributionLists) {
            grpcNotificationClient.deleteDistributionList(
                    new DeleteDistributionListRequestDto(distributionList.getExternalId()));
        }
        LOGGER.debug("Deleted distribution list(s) for resourceCrn: {} ", resourceCrn);
    }

    public List<DistributionList> listDistributionListsForResource(String resourceCrn) {
        List<DistributionList> lists = new ArrayList<>();
        ListDistributionListsRequestDto listRequest = new ListDistributionListsRequestDto(resourceCrn);
        ListDistributionListsResponseDto distributionListsResponse = grpcNotificationClient.listDistributionLists(listRequest);
        LOGGER.debug("Listed {} distribution lists for resourceCrn: {} ", distributionListsResponse.distributionLists().size(), resourceCrn);

        List<DistributionListDetailsDto> distributionLists = distributionListsResponse.distributionLists();
        if (CollectionUtils.isNotEmpty(distributionLists)) {
            for (DistributionListDetailsDto list : distributionLists) {
                DistributionListManagementType type = DistributionListManagementType.valueOf(list.distributionListManagementType());
                DistributionList distributionList = DistributionList.builder()
                        .externalDistributionListId(list.distributionListId())
                        .resourceCrn(list.resourceCrn())
                        .resourceName(list.resourceName())
                        .type(type)
                        .build();
                lists.add(distributionList);
            }
        } else {
            LOGGER.debug("No distribution lists returned for resourceCrn {}", resourceCrn);
        }
        return lists;
    }

    private DistributionList getList(CreateOrUpdateDistributionListResponseDto response, String resourceName) {
        DistributionListDto details = Optional.ofNullable(response)
                .map(CreateOrUpdateDistributionListResponseDto::distributionLists)
                .map(List::getFirst)
                .orElse(null);
        if (details != null) {
            DistributionList distributionList = new DistributionList();
            distributionList.setExternalId(details.distributionListId());
            distributionList.setResourceCrn(details.resourceCrn());
            distributionList.setResourceName(resourceName);
            distributionList.setType(DistributionListManagementType.USER_MANAGED);
            return distributionList;
        } else {
            LOGGER.warn("No distribution list details found in the response");
            return null;
        }
    }
}