package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsGroupConverter;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsUserConverter;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.WorkloadCredentialConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(MockitoExtension.class)
public class BulkUmsUsersStateProviderTest  extends BaseUmsUsersStateProviderTest {

    @Spy
    @SuppressFBWarnings
    private UmsRightsChecksFactory umsRightsChecksFactory = new UmsRightsChecksFactory();

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
    private UmsCredentialProvider umsCredentialProvider;

    @InjectMocks
    private BulkUmsUsersStateProvider underTest;

    @Test
    void getUmsUsersStateMapBulk() {
        setupMocksForBulk();

        UserSyncOptions options = UserSyncOptions.newBuilder()
                .fullSync(true)
                .largeGroupThreshold(5)
                .largeGroupLimit(10)
                .build();

        Map<String, UmsUsersState> umsUsersStateMap = underTest.get(
                ACCOUNT_ID, List.of(ENVIRONMENT_CRN), options);

        verifyUmsUsersStateBuilderMap(umsUsersStateMap, options);
    }

    private void setupMocksForBulk() {
        List<UserManagementProto.RightsCheck> expectedRightsChecks =
                List.of(UserManagementProto.RightsCheck.newBuilder()
                        .setResourceCrn(ENVIRONMENT_CRN)
                        .addAllRight(UserSyncConstants.RIGHTS)
                        .build());

        UserManagementProto.GetUserSyncStateModelResponse.Builder builder =
                UserManagementProto.GetUserSyncStateModelResponse.newBuilder();

        builder.addAllGroup(testData.groups);
        builder.addAllWorkloadAdministrationGroup(testData.allWags);
        builder.addAllActor(Stream.concat(
                testData.users.stream()
                        .map(u ->  {
                            Map<String, Boolean> groupMembership = testData.memberCrnToGroupMembership.get(u.getCrn());
                            Map<String, Boolean> wagMembership = testData.memberCrnToWagMembership.get(u.getCrn());
                            return UserManagementProto.UserSyncActor.newBuilder()
                                    .setActorDetails(UserManagementProto.UserSyncActorDetails.newBuilder()
                                            .setCrn(u.getCrn())
                                            .setWorkloadUsername(u.getWorkloadUsername())
                                            .setFirstName(u.getFirstName())
                                            .setLastName(u.getLastName())
                                            .addAllCloudIdentity(u.getCloudIdentitiesList())
                                            .build())
                                    .addRightsCheckResult(UserManagementProto.RightsCheckResult.newBuilder()
                                            .addAllHasRight(UserSyncConstants.RIGHTS.stream()
                                                    .map(right -> testData.memberCrnToActorRights.get(u.getCrn()).get(right))
                                                    .collect(Collectors.toList()))
                                            .build())
                                    .addAllGroupIndex(IntStream.range(0, testData.groups.size())
                                            .filter(i -> groupMembership.get(testData.groups.get(i).getCrn()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .addAllWorkloadAdministrationGroupIndex(IntStream.range(0, testData.allWags.size())
                                            .filter(i -> wagMembership
                                                    .get(testData.allWags.get(i).getWorkloadAdministrationGroupName()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .setCredentials(toActorWorkloadCredentials(
                                            testData.memberCrnToWorkloadCredentials.get(u.getCrn())))
                                    .build();
                        }),
                testData.machineUsers.stream()
                        .map(u -> {
                            Map<String, Boolean> groupMembership = testData.memberCrnToGroupMembership.get(u.getCrn());
                            Map<String, Boolean> wagMembership = testData.memberCrnToWagMembership.get(u.getCrn());
                            return UserManagementProto.UserSyncActor.newBuilder()
                                    .setActorDetails(UserManagementProto.UserSyncActorDetails.newBuilder()
                                            .setCrn(u.getCrn())
                                            .setWorkloadUsername(u.getWorkloadUsername())
                                            .setFirstName(u.getMachineUserName())
                                            .setLastName(u.getMachineUserId())
                                            .addAllCloudIdentity(u.getCloudIdentitiesList())
                                            .build())
                                    .addRightsCheckResult(UserManagementProto.RightsCheckResult.newBuilder()
                                            .addAllHasRight(UserSyncConstants.RIGHTS.stream()
                                                    .map(right -> testData.memberCrnToActorRights.get(u.getCrn()).get(right))
                                                    .collect(Collectors.toList()))
                                            .build())
                                    .addAllGroupIndex(IntStream.range(0, testData.groups.size())
                                            .filter(i -> groupMembership.get(testData.groups.get(i).getCrn()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .addAllWorkloadAdministrationGroupIndex(IntStream.range(0, testData.allWags.size())
                                            .filter(i -> wagMembership
                                                    .get(testData.allWags.get(i).getWorkloadAdministrationGroupName()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .setCredentialsVersion(toActorWorkloadCredentialsVersion(
                                            testData.memberCrnToWorkloadCredentials.get(u.getCrn())))
                                    .build();
                        }))
                .collect(Collectors.toList()));

        when(grpcUmsClient.getUserSyncStateModel(eq(ACCOUNT_ID), eq(expectedRightsChecks), eq(true))).thenReturn(builder.build());

        doAnswer(invocation -> workloadCredentialConverter
                .toWorkloadCredential(
                        testData.memberCrnToWorkloadCredentials.get(invocation.getArgument(0, String.class))))
                .when(umsCredentialProvider)
                .getCredentials(anyString());

        setupServicePrincipals();
    }

    private static UserManagementProto.ActorWorkloadCredentials toActorWorkloadCredentials(
            UserManagementProto.GetActorWorkloadCredentialsResponse response) {
        return UserManagementProto.ActorWorkloadCredentials.newBuilder()
                .setPasswordHash(response.getPasswordHash())
                .setPasswordHashExpirationDate(response.getPasswordHashExpirationDate())
                .addAllKerberosKeys(response.getKerberosKeysList())
                .addAllSshPublicKey(response.getSshPublicKeyList())
                .setWorkloadCredentialsVersion(response.getWorkloadCredentialsVersion())
                .build();
    }

    private static UserManagementProto.ActorWorkloadCredentialsVersion toActorWorkloadCredentialsVersion(
            UserManagementProto.GetActorWorkloadCredentialsResponse response) {
        return UserManagementProto.ActorWorkloadCredentialsVersion.newBuilder()
                .setWorkloadCredentialsVersion(response.getWorkloadCredentialsVersion())
                .build();
    }
}
