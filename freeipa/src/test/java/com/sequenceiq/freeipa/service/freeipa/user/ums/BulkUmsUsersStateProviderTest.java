package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsGroupConverter;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsUserConverter;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.WorkloadCredentialConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private BulkUmsUsersStateProvider underTest;

    @Test
    void getUmsUsersStateMapBulk() {
        setupMocksForBulk();

        Map<String, UmsUsersState> umsUsersStateMap = underTest.get(
                testData.getAccountId(), List.of(testData.getEnvironmentCrn()), Optional.empty());

        verifyUmsUsersStateBuilderMap(umsUsersStateMap);
    }

    private void setupMocksForBulk() {
        List<UserManagementProto.RightsCheck> expectedRightsChecks =
                List.of(UserManagementProto.RightsCheck.newBuilder()
                        .setResourceCrn(testData.getEnvironmentCrn())
                        .addAllRight(UserSyncConstants.RIGHTS)
                        .build());

        UserManagementProto.GetUserSyncStateModelResponse.Builder builder =
                UserManagementProto.GetUserSyncStateModelResponse.newBuilder();

        builder.addAllGroup(testData.getGroups());
        builder.addAllWorkloadAdministrationGroup(testData.getAllWags());
        builder.addAllActor(Stream.concat(
                testData.getUsers().stream()
                        .map(u ->  {
                            Map<String, Boolean> groupMembership = testData.getMemberCrnToGroupMembership().get(u.getCrn());
                            Map<String, Boolean> wagMembership = testData.getMemberCrnToWagMembership().get(u.getCrn());
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
                                                    .map(right -> testData.getMemberCrnToActorRights().get(u.getCrn()).get(right))
                                                    .collect(Collectors.toList()))
                                            .build())
                                    .addAllGroupIndex(IntStream.range(0, testData.getGroups().size())
                                            .filter(i -> groupMembership.get(testData.getGroups().get(i).getCrn()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .addAllWorkloadAdministrationGroupIndex(IntStream.range(0, testData.getAllWags().size())
                                            .filter(i -> wagMembership
                                                    .get(testData.getAllWags().get(i).getWorkloadAdministrationGroupName()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .setCredentials(toActorWorkloadCredentials(
                                            testData.getMemberCrnToWorkloadCredentials().get(u.getCrn())))
                                    .build();
                        }),
                testData.getMachineUsers().stream()
                        .map(u -> {
                            Map<String, Boolean> groupMembership = testData.getMemberCrnToGroupMembership().get(u.getCrn());
                            Map<String, Boolean> wagMembership = testData.getMemberCrnToWagMembership().get(u.getCrn());
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
                                                    .map(right -> testData.getMemberCrnToActorRights().get(u.getCrn()).get(right))
                                                    .collect(Collectors.toList()))
                                            .build())
                                    .addAllGroupIndex(IntStream.range(0, testData.getGroups().size())
                                            .filter(i -> groupMembership.get(testData.getGroups().get(i).getCrn()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .addAllWorkloadAdministrationGroupIndex(IntStream.range(0, testData.getAllWags().size())
                                            .filter(i -> wagMembership
                                                    .get(testData.getAllWags().get(i).getWorkloadAdministrationGroupName()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .setCredentials(toActorWorkloadCredentials(
                                            testData.getMemberCrnToWorkloadCredentials().get(u.getCrn())))
                                    .build();
                        }))
                .collect(Collectors.toList()));

        when(grpcUmsClient.getUserSyncStateModel(
                eq(INTERNAL_ACTOR_CRN), eq(testData.getAccountId()), eq(expectedRightsChecks), any(Optional.class)))
                .thenReturn(builder.build());
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
}
