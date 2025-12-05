package com.sequenceiq.notification.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.UserWithResourceRole;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.notification.client.GrpcNotificationClient;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateAccountMetadataDto;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListResponseDto;
import com.sequenceiq.cloudbreak.notification.client.dto.DeleteDistributionListRequestDto;
import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDetailsDto;
import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDto;
import com.sequenceiq.cloudbreak.notification.client.dto.EventChannelPreferenceDto;
import com.sequenceiq.cloudbreak.notification.client.dto.ListDistributionListsRequestDto;
import com.sequenceiq.cloudbreak.notification.client.dto.ListDistributionListsResponseDto;
import com.sequenceiq.notification.config.NotificationConfig;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.DistributionListManagementType;
import com.sequenceiq.notification.domain.EventChannelPreference;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.sender.converter.EventChannelPreferenceToEventChannelPreferenceDtoConverter;
import com.sequenceiq.notification.sender.dto.CreateDistributionListRequest;

@ExtendWith(MockitoExtension.class)
class DistributionListManagementServiceTest {

    private static final String ACCOUNT_ID = "acc123";

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:env123";

    private static final String RESOURCE_NAME = "envName";

    @Mock
    private NotificationConfig notificationConfig;

    @Mock
    private GrpcNotificationClient grpcNotificationClient;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private RoleCrnGenerator roleCrnGenerator;

    @Mock
    private EventChannelPreferenceToEventChannelPreferenceDtoConverter channelPreferenceConverter;

    @InjectMocks
    private DistributionListManagementService underTest;

    private EventChannelPreferenceDto samplePreferenceDto;

    private EventChannelPreference samplePreference;

    @BeforeEach
    void setUp() {
        samplePreferenceDto = new EventChannelPreferenceDto("EVENT_TYPE", Set.of(), Set.of(NotificationSeverity.INFO.name()));
        samplePreference = new EventChannelPreference("EVENT_TYPE", Set.of(ChannelType.EMAIL), Set.of(NotificationSeverity.INFO));
    }

    private UserWithResourceRole userWithEmail(String email) {
        UserManagementProto.User user = UserManagementProto.User.newBuilder()
                .setEmail(email)
                .setCrn("crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:user1")
                .build();
        return new UserWithResourceRole(user.getCrn(), "roleCrn");
    }

    @Test
    void createOrUpdateListWhenNotificationsDisabledReturnsNull() {
        when(notificationConfig.isEnabled(Crn.fromString(RESOURCE_CRN))).thenReturn(false);
        CreateDistributionListRequest request = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME, List.of(samplePreference));
        Optional<DistributionList> result = underTest.createOrUpdateList(request);
        assertTrue(result.isEmpty());
        verify(notificationConfig).isEnabled(any(Crn.class));
        verifyNoMoreInteractions(grpcNotificationClient, grpcUmsClient, roleCrnGenerator);
    }

    @Test
    void createOrUpdateListWhenExistingNonUserManagedUpdatesAndReturnsDistributionList() {
        when(notificationConfig.isEnabled(any(Crn.class))).thenReturn(true);
        when(roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(ACCOUNT_ID)).thenReturn("adminRoleCrn");
        when(roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(ACCOUNT_ID)).thenReturn("ownerRoleCrn");
        when(grpcUmsClient.listUsersWithResourceRoles(anySet(), eq(RESOURCE_CRN))).thenReturn(List.of(userWithEmail("user1@example.com")));
        when(channelPreferenceConverter.convert(List.of(samplePreference))).thenReturn(List.of(samplePreferenceDto));
        DistributionListDetailsDto existingDto = new DistributionListDetailsDto(
                "dl-1",
                RESOURCE_CRN,
                RESOURCE_NAME,
                null,
                Set.of("slack-1"),
                DistributionListManagementType.SYSTEM_MANAGED.name(),
                List.of()
        );
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of(existingDto)));
        DistributionListDto responseDto = new DistributionListDto("dl-1", RESOURCE_CRN);
        when(grpcNotificationClient.createOrUpdateDistributionList(any())).thenReturn(new CreateOrUpdateDistributionListResponseDto(List.of(responseDto)));

        CreateDistributionListRequest request = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME, List.of(samplePreference));
        Optional<DistributionList> result = underTest.createOrUpdateList(request);

        assertTrue(result.isPresent());
        assertEquals("dl-1", result.get().getExternalId());
        assertEquals(RESOURCE_CRN, result.get().getResourceCrn());
        verify(grpcNotificationClient).createOrUpdateDistributionList(any());
    }

    @Test
    void createOrUpdateListWhenExistingUserManagedReturnsNull() {
        when(notificationConfig.isEnabled(any(Crn.class))).thenReturn(true);
        DistributionListDetailsDto existingDto = new DistributionListDetailsDto(
                "dl-2",
                RESOURCE_CRN,
                RESOURCE_NAME,
                null,
                Set.of("slack-1"),
                DistributionListManagementType.USER_MANAGED.name(),
                List.of()
        );
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of(existingDto)));

        CreateDistributionListRequest request = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME, List.of(samplePreference));
        Optional<DistributionList> result = underTest.createOrUpdateList(request);
        assertTrue(result.isEmpty());
        verify(grpcNotificationClient, never()).createOrUpdateDistributionList(any());
    }

    @Test
    void createOrUpdateListWhenNoExistingCreatesAndReturnsDistributionList() {
        when(notificationConfig.isEnabled(any(Crn.class))).thenReturn(true);
        when(roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(ACCOUNT_ID)).thenReturn("adminRoleCrn");
        when(roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(ACCOUNT_ID)).thenReturn("ownerRoleCrn");
        // duplicate email
        when(grpcUmsClient.listUsersWithResourceRoles(anySet(), eq(RESOURCE_CRN))).thenReturn(List.of(userWithEmail("user1@example.com"),
                userWithEmail("user1@example.com")));
        when(channelPreferenceConverter.convert(List.of(samplePreference))).thenReturn(List.of(samplePreferenceDto));
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of()));
        DistributionListDto responseDto = new DistributionListDto("dl-new", RESOURCE_CRN);
        when(grpcNotificationClient.createOrUpdateDistributionList(any())).thenReturn(new CreateOrUpdateDistributionListResponseDto(List.of(responseDto)));

        CreateDistributionListRequest request = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME, List.of(samplePreference));
        Optional<DistributionList> result = underTest.createOrUpdateList(request);
        assertTrue(result.isPresent());
        assertEquals("dl-new", result.get().getExternalId());
    }

    @Test
    void createOrUpdateListWhenExceptionReturnsNull() {
        when(notificationConfig.isEnabled(any(Crn.class))).thenReturn(true);
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of()));
        doThrow(new RuntimeException("boom")).when(roleCrnGenerator).getBuiltInEnvironmentAdminResourceRoleCrn(ACCOUNT_ID);
        CreateDistributionListRequest request = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME, List.of(samplePreference));
        Optional<DistributionList> result = underTest.createOrUpdateList(request);
        assertTrue(result.isEmpty());
    }

    @Test
    void createOrUpdateListUpdateResponseEmptyReturnsNull() {
        when(notificationConfig.isEnabled(any(Crn.class))).thenReturn(true);
        when(roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(ACCOUNT_ID)).thenReturn("adminRoleCrn");
        when(roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(ACCOUNT_ID)).thenReturn("ownerRoleCrn");
        when(grpcUmsClient.listUsersWithResourceRoles(anySet(), eq(RESOURCE_CRN))).thenReturn(List.of(userWithEmail("user1@example.com")));
        when(channelPreferenceConverter.convert(List.of(samplePreference))).thenReturn(List.of(samplePreferenceDto));
        DistributionListDetailsDto existingDto = new DistributionListDetailsDto(
                "dl-x",
                RESOURCE_CRN,
                RESOURCE_NAME,
                null,
                Set.of("slack-1"),
                DistributionListManagementType.SYSTEM_MANAGED.name(),
                List.of()
        );
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of(existingDto)));
        when(grpcNotificationClient.createOrUpdateDistributionList(any())).thenReturn(new CreateOrUpdateDistributionListResponseDto(List.of()));

        CreateDistributionListRequest request = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME, List.of(samplePreference));
        Optional<DistributionList> result = underTest.createOrUpdateList(request);
        assertTrue(result.isEmpty());
    }

    @Test
    void createOrUpdateListCreateResponseEmptyReturnsNull() {
        when(notificationConfig.isEnabled(any(Crn.class))).thenReturn(true);
        when(roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(ACCOUNT_ID)).thenReturn("adminRoleCrn");
        when(roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(ACCOUNT_ID)).thenReturn("ownerRoleCrn");
        when(grpcUmsClient.listUsersWithResourceRoles(anySet(), eq(RESOURCE_CRN))).thenReturn(List.of(userWithEmail("user1@example.com")));
        when(channelPreferenceConverter.convert(List.of(samplePreference))).thenReturn(List.of(samplePreferenceDto));
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of()));
        when(grpcNotificationClient.createOrUpdateDistributionList(any())).thenReturn(new CreateOrUpdateDistributionListResponseDto(List.of()));

        CreateDistributionListRequest request = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME, List.of(samplePreference));
        Optional<DistributionList> result = underTest.createOrUpdateList(request);
        assertTrue(result.isEmpty());
    }

    @Test
    void createOrUpdateListUpdateResponseNullReturnsNull() {
        when(notificationConfig.isEnabled(any(Crn.class))).thenReturn(true);
        when(roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(ACCOUNT_ID)).thenReturn("adminRoleCrn");
        when(roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(ACCOUNT_ID)).thenReturn("ownerRoleCrn");
        when(grpcUmsClient.listUsersWithResourceRoles(anySet(), eq(RESOURCE_CRN))).thenReturn(List.of(userWithEmail("user1@example.com")));
        when(channelPreferenceConverter.convert(List.of(samplePreference))).thenReturn(List.of(samplePreferenceDto));
        DistributionListDetailsDto existingDto = new DistributionListDetailsDto(
                "dl-null",
                RESOURCE_CRN,
                RESOURCE_NAME,
                null,
                Set.of("slack-1"),
                DistributionListManagementType.SYSTEM_MANAGED.name(),
                List.of()
        );
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of(existingDto)));
        // response null
        when(grpcNotificationClient.createOrUpdateDistributionList(any())).thenReturn(null);

        CreateDistributionListRequest request = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME, List.of(samplePreference));
        Optional<DistributionList> result = underTest.createOrUpdateList(request);
        assertTrue(result.isEmpty());
    }

    @Test
    void createOrUpdateListCreateResponseNullListReturnsNull() {
        when(notificationConfig.isEnabled(any(Crn.class))).thenReturn(true);
        when(roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(ACCOUNT_ID)).thenReturn("adminRoleCrn");
        when(roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(ACCOUNT_ID)).thenReturn("ownerRoleCrn");
        when(grpcUmsClient.listUsersWithResourceRoles(anySet(), eq(RESOURCE_CRN))).thenReturn(List.of(userWithEmail("user1@example.com")));
        when(channelPreferenceConverter.convert(List.of(samplePreference))).thenReturn(List.of(samplePreferenceDto));
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of()));
        // distributionLists null
        CreateOrUpdateDistributionListResponseDto response = new CreateOrUpdateDistributionListResponseDto(null);
        when(grpcNotificationClient.createOrUpdateDistributionList(any())).thenReturn(response);

        CreateDistributionListRequest request = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME, List.of(samplePreference));
        Optional<DistributionList> result = underTest.createOrUpdateList(request);
        assertTrue(result.isEmpty());
    }

    @Test
    void createOrUpdateListsAggregatesResultsNotIncludingNull() {
        when(notificationConfig.isEnabled(any(Crn.class))).thenReturn(true);
        when(roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(ACCOUNT_ID)).thenReturn("adminRoleCrn");
        when(roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(ACCOUNT_ID)).thenReturn("ownerRoleCrn");
        when(grpcUmsClient.listUsersWithResourceRoles(anySet(), eq(RESOURCE_CRN))).thenReturn(List.of(userWithEmail("user1@example.com")));
        when(channelPreferenceConverter.convert(List.of(samplePreference))).thenReturn(List.of(samplePreferenceDto));
        // First create succeeds, second returns empty list -> null
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of()))
                .thenReturn(new ListDistributionListsResponseDto(List.of()));
        DistributionListDto responseDto1 = new DistributionListDto("dl-a", RESOURCE_CRN);
        when(grpcNotificationClient.createOrUpdateDistributionList(any()))
                .thenReturn(new CreateOrUpdateDistributionListResponseDto(List.of(responseDto1)))
                .thenReturn(new CreateOrUpdateDistributionListResponseDto(List.of()));

        CreateDistributionListRequest req1 = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME + "1", List.of(samplePreference));
        CreateDistributionListRequest req2 = new CreateDistributionListRequest(RESOURCE_CRN, RESOURCE_NAME + "2", List.of(samplePreference));
        List<DistributionList> results = underTest.createOrUpdateLists(Set.of(req1, req2));
        assertEquals(1, results.size());
        assertTrue(results.stream().anyMatch(dl -> dl != null && "dl-a".equals(dl.getExternalId())));
    }

    @Test
    void createAccountMetadataForResourceWithEmailsCallsClientWithAllowedDomains() {
        Set<String> emails = new LinkedHashSet<>();
        emails.add("User1@Example.com");
        emails.add(null);
        emails.add("invalidEmail");
        emails.add(" user2@Other.com ");
        emails.add("noAt@");
        emails.add("x@y");
        ArgumentCaptor<CreateOrUpdateAccountMetadataDto> captor = ArgumentCaptor.forClass(CreateOrUpdateAccountMetadataDto.class);

        underTest.createAccountMetadataForResource(RESOURCE_CRN, emails);

        verify(grpcNotificationClient).createOrUpdateAccountMetadata(captor.capture());
        CreateOrUpdateAccountMetadataDto dto = captor.getValue();
        assertEquals(ACCOUNT_ID, dto.accountId());
        assertEquals(Set.of("example.com", "other.com", "y"), Set.copyOf(dto.allowedDomains()));
    }

    @Test
    void createAccountMetadataForResourceWithEmptyEmailsSkipsClient() {
        underTest.createAccountMetadataForResource(RESOURCE_CRN, Set.of());
        verify(grpcNotificationClient, never()).createOrUpdateAccountMetadata(any());
    }

    @Test
    void createAccountMetadataForResourceExceptionIsCaught() {
        Set<String> emails = Set.of("user@example.com");
        doThrow(new RuntimeException("failure")).when(grpcNotificationClient).createOrUpdateAccountMetadata(any(CreateOrUpdateAccountMetadataDto.class));
        // Should swallow exception
        underTest.createAccountMetadataForResource(RESOURCE_CRN, emails);
        verify(grpcNotificationClient).createOrUpdateAccountMetadata(any());
    }

    @Test
    void listDistributionListsForResourceReturnsMappedDomainObjects() {
        DistributionListDetailsDto d1 = new DistributionListDetailsDto(
                "id1",
                RESOURCE_CRN,
                RESOURCE_NAME,
                null,
                Set.of("slack-1"),
                DistributionListManagementType.USER_MANAGED.name(),
                List.of()
        );
        DistributionListDetailsDto d2 = new DistributionListDetailsDto(
                "id2",
                RESOURCE_CRN,
                RESOURCE_NAME,
                null,
                Set.of("slack-1"),
                DistributionListManagementType.SYSTEM_MANAGED.name(),
                List.of()
        );
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of(d1, d2)));

        List<DistributionList> result = underTest.listDistributionListsForResource(RESOURCE_CRN);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dl -> "id1".equals(dl.getExternalId())
                && dl.getType() == DistributionListManagementType.USER_MANAGED));
        assertTrue(result.stream().anyMatch(dl -> "id2".equals(dl.getExternalId())
                && dl.getType() == DistributionListManagementType.SYSTEM_MANAGED));
    }

    @Test
    void listDistributionListsForResourceReturnsEmptyListWhenNoDistributionLists() {
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of()));
        List<DistributionList> result = underTest.listDistributionListsForResource(RESOURCE_CRN);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteDistributionListDeletesEach() {
        DistributionListDetailsDto d1 = new DistributionListDetailsDto(
                "del1",
                RESOURCE_CRN,
                RESOURCE_NAME,
                null,
                Set.of("slack-1"),
                DistributionListManagementType.USER_MANAGED.name(),
                List.of()
        );
        DistributionListDetailsDto d2 = new DistributionListDetailsDto(
                "del2",
                RESOURCE_CRN,
                RESOURCE_NAME,
                null,
                Set.of("slack-1"),
                DistributionListManagementType.SYSTEM_MANAGED.name(),
                List.of()
        );
        when(grpcNotificationClient.listDistributionLists(any(ListDistributionListsRequestDto.class)))
                .thenReturn(new ListDistributionListsResponseDto(List.of(d1, d2)));
        doNothing().when(grpcNotificationClient).deleteDistributionList(any(DeleteDistributionListRequestDto.class));

        underTest.deleteDistributionList(RESOURCE_CRN);

        verify(grpcNotificationClient, times(2)).deleteDistributionList(any(DeleteDistributionListRequestDto.class));
        verify(grpcNotificationClient).deleteDistributionList(argThat(req -> "del1".equals(req.distributionListId())));
        verify(grpcNotificationClient).deleteDistributionList(argThat(req -> "del2".equals(req.distributionListId())));
    }
}
