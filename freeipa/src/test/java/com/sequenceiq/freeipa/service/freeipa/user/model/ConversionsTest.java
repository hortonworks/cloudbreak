package com.sequenceiq.freeipa.service.freeipa.user.model;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversionsTest {
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

        FmsUser fmsUser = Conversions.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(firstName, fmsUser.getFirstName());
        assertEquals(lastName, fmsUser.getLastName());
    }

    @Test
    public void testUserToFmsUserMissingNames() {
        String workloadUsername = "foobar";
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .build();

        FmsUser fmsUser = Conversions.toFmsUser(umsUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(Conversions.NONE_STRING, fmsUser.getFirstName());
        assertEquals(Conversions.NONE_STRING, fmsUser.getLastName());
    }

    @Test
    public void testUserToUserMissingWorkloadUsername() {
        String firstName = "Foo";
        String lastName = "Bar";
        UserManagementProto.User umsUser = UserManagementProto.User.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .build();

        assertThrows(IllegalArgumentException.class, () -> Conversions.toFmsUser(umsUser));
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

        FmsUser fmsUser = Conversions.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(name, fmsUser.getFirstName());
        assertEquals(id, fmsUser.getLastName());
    }

    @Test
    public void testMachineUserToFmsUserMissingNames() {
        String workloadUsername = "foobar";
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .build();

        FmsUser fmsUser = Conversions.toFmsUser(umsMachineUser);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(Conversions.NONE_STRING, fmsUser.getFirstName());
        assertEquals(Conversions.NONE_STRING, fmsUser.getLastName());
    }

    @Test
    public void testMachineUserToFmsUserMissingWorkloadUsername() {
        String name = "Foo";
        String id = "Bar";
        UserManagementProto.MachineUser umsMachineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName(name)
                .setMachineUserId(id)
                .build();

        assertThrows(IllegalArgumentException.class, () -> Conversions.toFmsUser(umsMachineUser));
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

        FmsUser fmsUser = Conversions.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(firstName, fmsUser.getFirstName());
        assertEquals(lastName, fmsUser.getLastName());
    }

    @Test
    public void testUserSyncActorDetailsToFmsUserMissingNames() {
        String workloadUsername = "foobar";
        UserManagementProto.UserSyncActorDetails actorDetails =
                UserManagementProto.UserSyncActorDetails.newBuilder()
                        .setWorkloadUsername(workloadUsername)
                        .build();

        FmsUser fmsUser = Conversions.toFmsUser(actorDetails);

        assertEquals(workloadUsername, fmsUser.getName());
        assertEquals(Conversions.NONE_STRING, fmsUser.getFirstName());
        assertEquals(Conversions.NONE_STRING, fmsUser.getLastName());
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

        assertThrows(IllegalArgumentException.class, () -> Conversions.toFmsUser(actorDetails));
    }

    @Test
    public void testUmsGroupToGroup() {
        String groupName = "foobar";
        UserManagementProto.Group umsGroup = UserManagementProto.Group.newBuilder()
                .setGroupName(groupName)
                .build();

        FmsGroup fmsGroup = Conversions.umsGroupToGroup(umsGroup);

        assertEquals(groupName, fmsGroup.getName());
    }

    @Test
    public void testUmsGroupToGroupMissingName() {
        UserManagementProto.Group umsGroup = UserManagementProto.Group.newBuilder()
                .build();

        assertThrows(IllegalArgumentException.class, () -> Conversions.umsGroupToGroup(umsGroup));
    }

    @Test
    public void testNameToGroup() {
        String groupName = "foobar";

        FmsGroup fmsGroup = Conversions.nameToGroup(groupName);

        assertEquals(groupName, fmsGroup.getName());
    }

    @Test
    public void testNameToGroupMissingGroupName() {
        assertThrows(IllegalArgumentException.class, () -> Conversions.nameToGroup(null));
    }

    @Test
    public void testActorWorkloadCredentialsToWorkloadCredential() {
        String passwordHash = "password-hash";
        List<UserManagementProto.ActorKerberosKey> kerberosKeys = List.of(
                UserManagementProto.ActorKerberosKey.newBuilder().build());
        long passwordExpiration = System.currentTimeMillis();
        List<UserManagementProto.SshPublicKey> sshPublicKeys = List.of(
                UserManagementProto.SshPublicKey.newBuilder().build());

        Optional<Instant> expectedPasswordExpiration = Optional.of(Instant.ofEpochMilli(passwordExpiration));

        UserManagementProto.ActorWorkloadCredentials actorWorkloadCredentials =
                UserManagementProto.ActorWorkloadCredentials.newBuilder()
                        .setPasswordHash(passwordHash)
                        .setPasswordHashExpirationDate(passwordExpiration)
                        .addAllKerberosKeys(kerberosKeys)
                        .addAllSshPublicKey(sshPublicKeys)
                        .build();

        WorkloadCredential workloadCredential = Conversions.toWorkloadCredential(actorWorkloadCredentials);

        assertEquals(passwordHash, workloadCredential.getHashedPassword());
        assertEquals(expectedPasswordExpiration, workloadCredential.getExpirationDate());
        assertIterableEquals(kerberosKeys, workloadCredential.getKeys());
        assertIterableEquals(sshPublicKeys, workloadCredential.getSshPublicKeys());
    }

    @Test
    public void testActorWorkloadCredentialsToWorkloadCredentialNoExpiration() {
        String passwordHash = "password-hash";
        List<UserManagementProto.ActorKerberosKey> kerberosKeys = List.of(
                UserManagementProto.ActorKerberosKey.newBuilder().build());
        long passwordExpiration = 0;
        List<UserManagementProto.SshPublicKey> sshPublicKeys = List.of(
                UserManagementProto.SshPublicKey.newBuilder().build());

        Optional<Instant> expectedPasswordExpiration = Optional.empty();

        UserManagementProto.ActorWorkloadCredentials actorWorkloadCredentials =
                UserManagementProto.ActorWorkloadCredentials.newBuilder()
                        .setPasswordHash(passwordHash)
                        .setPasswordHashExpirationDate(passwordExpiration)
                        .addAllKerberosKeys(kerberosKeys)
                        .addAllSshPublicKey(sshPublicKeys)
                        .build();

        WorkloadCredential workloadCredential = Conversions.toWorkloadCredential(actorWorkloadCredentials);

        assertEquals(passwordHash, workloadCredential.getHashedPassword());
        assertEquals(expectedPasswordExpiration, workloadCredential.getExpirationDate());
        assertIterableEquals(kerberosKeys, workloadCredential.getKeys());
        assertIterableEquals(sshPublicKeys, workloadCredential.getSshPublicKeys());
    }

    @Test
    public void testGetActorWorkloadCredentialsResponseToWorkloadCredential() {
        String passwordHash = "password-hash";
        List<UserManagementProto.ActorKerberosKey> kerberosKeys = List.of(
                UserManagementProto.ActorKerberosKey.newBuilder().build());
        long passwordExpiration = System.currentTimeMillis();
        List<UserManagementProto.SshPublicKey> sshPublicKeys = List.of(
                UserManagementProto.SshPublicKey.newBuilder().build());

        Optional<Instant> expectedPasswordExpiration = Optional.of(Instant.ofEpochMilli(passwordExpiration));

        UserManagementProto.GetActorWorkloadCredentialsResponse actorWorkloadCredentials =
                UserManagementProto.GetActorWorkloadCredentialsResponse.newBuilder()
                        .setPasswordHash(passwordHash)
                        .setPasswordHashExpirationDate(passwordExpiration)
                        .addAllKerberosKeys(kerberosKeys)
                        .addAllSshPublicKey(sshPublicKeys)
                        .build();

        WorkloadCredential workloadCredential = Conversions.toWorkloadCredential(actorWorkloadCredentials);

        assertEquals(passwordHash, workloadCredential.getHashedPassword());
        assertEquals(expectedPasswordExpiration, workloadCredential.getExpirationDate());
        assertIterableEquals(kerberosKeys, workloadCredential.getKeys());
        assertIterableEquals(sshPublicKeys, workloadCredential.getSshPublicKeys());
    }

    @Test
    public void testGetActorWorkloadCredentialsResponseToWorkloadCredentialNoExpiration() {
        String passwordHash = "password-hash";
        List<UserManagementProto.ActorKerberosKey> kerberosKeys = List.of(
                UserManagementProto.ActorKerberosKey.newBuilder().build());
        long passwordExpiration = 0;
        List<UserManagementProto.SshPublicKey> sshPublicKeys = List.of(
                UserManagementProto.SshPublicKey.newBuilder().build());

        Optional<Instant> expectedPasswordExpiration = Optional.empty();

        UserManagementProto.GetActorWorkloadCredentialsResponse actorWorkloadCredentials =
                UserManagementProto.GetActorWorkloadCredentialsResponse.newBuilder()
                        .setPasswordHash(passwordHash)
                        .setPasswordHashExpirationDate(passwordExpiration)
                        .addAllKerberosKeys(kerberosKeys)
                        .addAllSshPublicKey(sshPublicKeys)
                        .build();

        WorkloadCredential workloadCredential = Conversions.toWorkloadCredential(actorWorkloadCredentials);

        assertEquals(passwordHash, workloadCredential.getHashedPassword());
        assertEquals(expectedPasswordExpiration, workloadCredential.getExpirationDate());
        assertIterableEquals(kerberosKeys, workloadCredential.getKeys());
        assertIterableEquals(sshPublicKeys, workloadCredential.getSshPublicKeys());
    }
}