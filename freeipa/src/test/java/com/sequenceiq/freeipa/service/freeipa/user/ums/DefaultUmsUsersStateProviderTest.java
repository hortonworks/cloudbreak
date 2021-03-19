package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncRequestFilter;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsGroupConverter;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsUserConverter;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.WorkloadCredentialConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultUmsUsersStateProviderTest extends BaseUmsUsersStateProviderTest {

    @Mock
    private UmsCredentialProvider umsCredentialProvider;

    @Spy
    @SuppressFBWarnings
    private AuthorizationRightChecksFactory authorizationRightChecksFactory = new AuthorizationRightChecksFactory();

    @Mock
    private EnvironmentAccessCheckerFactory environmentAccessCheckerFactory;

    @Spy
    @SuppressFBWarnings
    private FmsUserConverter fmsUserConverter = new FmsUserConverter();

    @Spy
    @SuppressFBWarnings
    private FmsGroupConverter fmsGroupConverter = new FmsGroupConverter();

    @Spy
    @SuppressFBWarnings
    private WorkloadCredentialConverter workloadCredentialConverter = new WorkloadCredentialConverter();

    @Mock
    private UserRetriever userRetriever;

    @Mock
    private MachineUserRetriever machineUserRetriever;

    @InjectMocks
    private DefaultUmsUsersStateProvider underTest;

    @Test
    void getEnvToUmsUsersStateMap() {
        setupMocks();
        UserSyncRequestFilter requestFilter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
        Multimap<String, String> warnings = ArrayListMultimap.create();

        Map<String, UmsUsersState> umsUsersStateMap = underTest.get(
                testData.getAccountId(), testData.getActorCrn(), List.of(testData.getEnvironmentCrn()),
                requestFilter, Optional.empty(), warnings::put);

        verifyUmsUsersStateBuilderMap(umsUsersStateMap);
        assertTrue(warnings.isEmpty());
    }

    private void setupMocks() {
        doAnswer(invocation -> {
            String environmentCrn = invocation.getArgument(0, String.class);
            return new EnvironmentAccessChecker(
                    grpcUmsClient,
                    environmentCrn,
                    authorizationRightChecksFactory.create(environmentCrn));
        }).when(environmentAccessCheckerFactory).create(anyString());

        when(grpcUmsClient.listAllGroups(eq(INTERNAL_ACTOR_CRN), eq(testData.getAccountId()), any(Optional.class)))
                .thenReturn(testData.getGroups());
        when(grpcUmsClient.listWorkloadAdministrationGroups(eq(INTERNAL_ACTOR_CRN),
                eq(testData.getAccountId()), any(Optional.class)))
                .thenReturn(testData.getAllWags());


        when(userRetriever.getUsers(eq(testData.getActorCrn()), eq(testData.getAccountId()),
                any(Optional.class), anyBoolean(), anySet(), any(BiConsumer.class)))
                .thenReturn(testData.getUsers());

        when(machineUserRetriever.getMachineUsers(eq(testData.getActorCrn()), eq(testData.getAccountId()),
                any(Optional.class), anyBoolean(), anySet(), any(BiConsumer.class)))
                .thenReturn(testData.getMachineUsers());

        doAnswer(invocation -> {
            String crn = invocation.getArgument(1, String.class);
            Map<String, Boolean> actorRights = testData.getMemberCrnToActorRights().get(crn);
            return UserSyncConstants.RIGHTS.stream()
                    .map(right -> actorRights.get(right))
                    .collect(Collectors.toList());
        }).when(grpcUmsClient).hasRightsNoCache(eq(INTERNAL_ACTOR_CRN), anyString(), any(List.class), any(Optional.class));

        doAnswer(invocation -> {
            String memberCrn = invocation.getArgument(2, String.class);
            return testData.getMemberCrnToGroupMembership().get(memberCrn).entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }).when(grpcUmsClient)
                .listGroupsForMember(eq(INTERNAL_ACTOR_CRN), eq(testData.getAccountId()), anyString(), any(Optional.class));

        doAnswer(invocation -> {
            String memberCrn = invocation.getArgument(1, String.class);
            return testData.getMemberCrnToWagMembership().get(memberCrn).entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }).when(grpcUmsClient)
                .listWorkloadAdministrationGroupsForMember(eq(INTERNAL_ACTOR_CRN), anyString(), any(Optional.class));

        doAnswer(invocation -> workloadCredentialConverter
                .toWorkloadCredential(
                        testData.getMemberCrnToWorkloadCredentials().get(invocation.getArgument(0, String.class))))
                .when(umsCredentialProvider)
                .getCredentials(anyString(), any(Optional.class));

        setupServicePrincipals();
    }
}