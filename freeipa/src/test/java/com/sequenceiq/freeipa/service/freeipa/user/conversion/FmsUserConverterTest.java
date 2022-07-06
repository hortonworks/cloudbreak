package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FmsUserConverterTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final int UNRECOGNIZED_STATE_VALUE = 99;

    private FmsUserConverter underTest = new FmsUserConverter();

    @Test
    public void testUserToFmsUser() {
        String firstName = "Foo";
        String lastName = "Bar";
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(firstName, fmsUser.getFirstName());
        assertEquals(lastName, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserToFmsUserWithSpaces() {
        String firstName = " Foo ";
        String lastName = " Bar ";
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals("foobar", fmsUser.getName());
        assertEquals("Foo", fmsUser.getFirstName());
        assertEquals("Bar", fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserToFmsUserMissingNames() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserToFmsUserDeactivatedState() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.DEACTIVATED)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.DISABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserToFmsUserDeletingState() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.DELETING)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserToFmsUserControlPlaneLockedOutState() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.CONTROL_PLANE_LOCKED_OUT)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserToFmsUserUnrecognizedState() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setStateValue(UNRECOGNIZED_STATE_VALUE)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserToFmsUserMissingState() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserToUserMissingWorkloadUsername() {
        String firstName = "Foo";
        String lastName = "Bar";
        String crn = createUserCrn();
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(umsUser));
    }

    @Test
    public void testUserToUserMissingCrn() {
        String workloadUsername = "foobar";
        String firstName = "Foo";
        String lastName = "Bar";
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(umsUser));
    }

    @Test
    public void testUserToUserCrnFormat() {
        String workloadUsername = "foobar";
        String firstName = "Foo";
        String lastName = "Bar";
        String crn = "crn:notacrn";
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(umsUser));
    }

    @Test
    public void testMachineUserToFmsUser() {
        String name = "Foo";
        String id = "Bar";
        String workloadUsername = "foobar";
        String crn = createMachineUserCrn();
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName(name)
                .setMachineUserId(id)
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(name, fmsUser.getFirstName());
        assertEquals(id, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testMachineUserToFmsUserWithSpaces() {
        String name = " Foo ";
        String id = " Bar ";
        String workloadUsername = "foobar";
        String crn = createMachineUserCrn();
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName(name)
                .setMachineUserId(id)
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals("foobar", fmsUser.getName());
        assertEquals("Foo", fmsUser.getFirstName());
        assertEquals("Bar", fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testMachineUserToFmsUserMissingNames() {
        String workloadUsername = "foobar";
        String crn = createMachineUserCrn();
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testMachineUserToFmsUserDeactivatedState() {
        String workloadUsername = "foobar";
        String crn = createMachineUserCrn();
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.DEACTIVATED)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.DISABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testMachineUserToFmsUserDeletingState() {
        String workloadUsername = "foobar";
        String crn = createMachineUserCrn();
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setState(UserManagementProto.ActorState.Value.DELETING)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testMachineUserToFmsUserUnrecognizedState() {
        String workloadUsername = "foobar";
        String crn = createMachineUserCrn();
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setStateValue(UNRECOGNIZED_STATE_VALUE)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testMachineUserToFmsUserMissingState() {
        String workloadUsername = "foobar";
        String crn = createMachineUserCrn();
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setCrn(crn)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testMachineUserToFmsUserMissingWorkloadUsername() {
        String name = "Foo";
        String id = "Bar";
        String crn = createMachineUserCrn();
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName(name)
                .setMachineUserId(id)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(umsMachineUser));
    }

    @Test
    public void testMachineUserToFmsUserCrnFormat() {
        String workloadUsername = "foobar";
        String name = "Foo";
        String id = "Bar";
        String crn = "crn:notacrn";
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setMachineUserName(name)
                .setMachineUserId(id)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .setCrn(crn)
                .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(umsMachineUser));
    }

    @Test
    public void testMachineUserToFmsUserMissingCrn() {
        String workloadUsername = "foobar";
        String name = "Foo";
        String id = "Bar";
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setMachineUserName(name)
                .setMachineUserId(id)
                .setState(UserManagementProto.ActorState.Value.ACTIVE)
                .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(umsMachineUser));
    }

    @Test
    public void testUserSyncActorDetailsToFmsUser() {
        String firstName = "Foo";
        String lastName = "Bar";
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setWorkloadUsername(workloadUsername)
                        .setState(UserManagementProto.ActorState.Value.ACTIVE)
                        .setCrn(crn)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(firstName, fmsUser.getFirstName());
        assertEquals(lastName, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserWithSpaces() {
        String firstName = " Foo ";
        String lastName = " Bar ";
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setWorkloadUsername(workloadUsername)
                        .setState(UserManagementProto.ActorState.Value.ACTIVE)
                        .setCrn(crn)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals("foobar", fmsUser.getName());
        assertEquals("Foo", fmsUser.getFirstName());
        assertEquals("Bar", fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserMissingNames() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setWorkloadUsername(workloadUsername)
                        .setState(UserManagementProto.ActorState.Value.ACTIVE)
                        .setCrn(crn)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserDeactivatedState() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setWorkloadUsername(workloadUsername)
                        .setState(UserManagementProto.ActorState.Value.DEACTIVATED)
                        .setCrn(crn)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.DISABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserDeletingState() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setWorkloadUsername(workloadUsername)
                        .setState(UserManagementProto.ActorState.Value.DELETING)
                        .setCrn(crn)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserUnrecognizedState() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setWorkloadUsername(workloadUsername)
                        .setStateValue(UNRECOGNIZED_STATE_VALUE)
                        .setCrn(crn)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserMissingState() {
        String workloadUsername = "foobar";
        String crn = createUserCrn();
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setWorkloadUsername(workloadUsername)
                        .setCrn(crn)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
        assertEquals(FmsUser.State.ENABLED, fmsUser.getState());
        assertEquals(crn, fmsUser.getCrn());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserMissingWorkloadUsername() {
        String firstName = "Foo";
        String lastName = "Bar";
        String crn = createUserCrn();
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setState(UserManagementProto.ActorState.Value.ACTIVE)
                        .setCrn(crn)
                        .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(actorDetails));
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserBlankWorkloadUsername() {
        String firstName = "Foo";
        String lastName = "Bar";
        String workloadUserName = " ";
        String crn = createUserCrn();
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setWorkloadUsername(workloadUserName)
                        .setState(UserManagementProto.ActorState.Value.ACTIVE)
                        .setCrn(crn)
                        .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(actorDetails));
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserMissingCrn() {
        String firstName = "Foo";
        String lastName = "Bar";
        String workloadUserName = "foobar";
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setWorkloadUsername(workloadUserName)
                        .setState(UserManagementProto.ActorState.Value.ACTIVE)
                        .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(actorDetails));
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserCrnFormat() {
        String firstName = "Foo";
        String lastName = "Bar";
        String workloadUserName = "foobar";
        String crn = "crn:notacrn";
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setWorkloadUsername(workloadUserName)
                        .setState(UserManagementProto.ActorState.Value.ACTIVE)
                        .setCrn(crn)
                        .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(actorDetails));
    }

    private String createUserCrn() {
        return CrnTestUtil.getUserCrnBuilder()
                .setAccountId(ACCOUNT_ID)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    private String createMachineUserCrn() {
        return CrnTestUtil.getMachineUserCrnBuilder()
                .setAccountId(ACCOUNT_ID)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }
}