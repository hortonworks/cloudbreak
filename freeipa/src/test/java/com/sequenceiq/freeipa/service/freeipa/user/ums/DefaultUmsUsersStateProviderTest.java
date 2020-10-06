package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
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
import java.util.stream.Collectors;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private DefaultUmsUsersStateProvider underTest;

    @Test
    void getEnvToUmsUsersStateMap() {
        setupMocks();

        Map<String, UmsUsersState> umsUsersStateMap = underTest.get(
                ACCOUNT_ID, ACTOR_CRN, List.of(ENVIRONMENT_CRN), Set.of(), Set.of(), Optional.empty(), true);

        verifyUmsUsersStateBuilderMap(umsUsersStateMap);
    }

    private void setupMocks() {
        doAnswer(invocation -> {
            String environmentCrn = invocation.getArgument(0, String.class);
            return new EnvironmentAccessChecker(
                    grpcUmsClient,
                    environmentCrn,
                    authorizationRightChecksFactory.create(environmentCrn));
        }).when(environmentAccessCheckerFactory).create(anyString());

        when(grpcUmsClient.listAllGroups(eq(INTERNAL_ACTOR_CRN), eq(ACCOUNT_ID), any(Optional.class)))
                .thenReturn(testData.groups);
        when(grpcUmsClient.listWorkloadAdministrationGroups(eq(INTERNAL_ACTOR_CRN),
                eq(ACCOUNT_ID), any(Optional.class)))
                .thenReturn(testData.allWags);


        when(grpcUmsClient.listAllUsers(eq(ACTOR_CRN), eq(ACCOUNT_ID), any(Optional.class)))
                .thenReturn(testData.users);

        when(grpcUmsClient.listAllMachineUsers(eq(ACTOR_CRN), eq(ACCOUNT_ID),
                eq(DefaultUmsUsersStateProvider.DONT_INCLUDE_INTERNAL_MACHINE_USERS),
                eq(DefaultUmsUsersStateProvider.INCLUDE_WORKLOAD_MACHINE_USERS),
                any(Optional.class)))
                .thenReturn(testData.machineUsers);

        doAnswer(invocation -> {
            String crn = invocation.getArgument(1, String.class);
            Map<String, Boolean> actorRights = testData.memberCrnToActorRights.get(crn);
            return UserSyncConstants.RIGHTS.stream()
                    .map(right -> actorRights.get(right))
                    .collect(Collectors.toList());
        }).when(grpcUmsClient).hasRights(eq(INTERNAL_ACTOR_CRN), anyString(), any(List.class), any(Optional.class));

        doAnswer(invocation -> {
            String memberCrn = invocation.getArgument(2, String.class);
            return testData.memberCrnToGroupMembership.get(memberCrn).entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }).when(grpcUmsClient)
                .listGroupsForMember(eq(INTERNAL_ACTOR_CRN), eq(ACCOUNT_ID), anyString(), any(Optional.class));

        doAnswer(invocation -> {
            String memberCrn = invocation.getArgument(1, String.class);
            return testData.memberCrnToWagMembership.get(memberCrn).entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }).when(grpcUmsClient)
                .listWorkloadAdministrationGroupsForMember(eq(INTERNAL_ACTOR_CRN), anyString(), any(Optional.class));

        doAnswer(invocation -> workloadCredentialConverter
                .toWorkloadCredential(
                        testData.memberCrnToWorkloadCredentials.get(invocation.getArgument(0, String.class))))
                .when(umsCredentialProvider)
                .getCredentials(anyString(), any(Optional.class));

        setupServicePrincipals();
    }
}