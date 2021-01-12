package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FmsUserConverterTest {
    private FmsUserConverter underTest = new FmsUserConverter();

    @Test
    public void testUserToFmsUser() {
        String firstName = "Foo";
        String lastName = "Bar";
        String workloadUsername = "foobar";
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .setWorkloadUsername(workloadUsername)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(firstName, fmsUser.getFirstName());
        assertEquals(lastName, fmsUser.getLastName());
    }

    @Test
    public void testUserToFmsUserWithSpaces() {
        String firstName = " Foo ";
        String lastName = " Bar ";
        String workloadUsername = "foobar";
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .setWorkloadUsername(workloadUsername)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals("foobar", fmsUser.getName());
        assertEquals("Foo", fmsUser.getFirstName());
        assertEquals("Bar", fmsUser.getLastName());
    }

    @Test
    public void testUserToFmsUserMissingNames() {
        String workloadUsername = "foobar";
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
    }

    @Test
    public void testUserToUserMissingWorkloadUsername() {
        String firstName = "Foo";
        String lastName = "Bar";
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(umsUser));
    }

    @Test
    public void testMachineUserToFmsUser() {
        String name = "Foo";
        String id = "Bar";
        String workloadUsername = "foobar";
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName(name)
                .setMachineUserId(id)
                .setWorkloadUsername(workloadUsername)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(name, fmsUser.getFirstName());
        assertEquals(id, fmsUser.getLastName());
    }

    @Test
    public void testMachineUserToFmsUserWithSpaces() {
        String name = " Foo ";
        String id = " Bar ";
        String workloadUsername = "foobar";
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName(name)
                .setMachineUserId(id)
                .setWorkloadUsername(workloadUsername)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals("foobar", fmsUser.getName());
        assertEquals("Foo", fmsUser.getFirstName());
        assertEquals("Bar", fmsUser.getLastName());
    }

    @Test
    public void testMachineUserToFmsUserMissingNames() {
        String workloadUsername = "foobar";
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .build();

        FmsUser fmsUser = underTest.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
    }

    @Test
    public void testMachineUserToFmsUserMissingWorkloadUsername() {
        String name = "Foo";
        String id = "Bar";
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName(name)
                .setMachineUserId(id)
                .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(umsMachineUser));
    }

    @Test
    public void testUserSyncActorDetailsToFmsUser() {
        String firstName = "Foo";
        String lastName = "Bar";
        String workloadUsername = "foobar";
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setWorkloadUsername(workloadUsername)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(firstName, fmsUser.getFirstName());
        assertEquals(lastName, fmsUser.getLastName());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserWithSpaces() {
        String firstName = " Foo ";
        String lastName = " Bar ";
        String workloadUsername = "foobar";
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setWorkloadUsername(workloadUsername)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals("foobar", fmsUser.getName());
        assertEquals("Foo", fmsUser.getFirstName());
        assertEquals("Bar", fmsUser.getLastName());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserMissingNames() {
        String workloadUsername = "foobar";
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setWorkloadUsername(workloadUsername)
                        .build();

        FmsUser fmsUser = underTest.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(underTest.NONE_STRING, fmsUser.getFirstName());
        assertEquals(underTest.NONE_STRING, fmsUser.getLastName());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserMissingWorkloadUsername() {
        String firstName = "Foo";
        String lastName = "Bar";
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(actorDetails));
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserBlankWorkloadUsername() {
        String firstName = "Foo";
        String lastName = "Bar";
        String workloadUserName = " ";
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setWorkloadUsername(workloadUserName)
                        .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.toFmsUser(actorDetails));
    }
}