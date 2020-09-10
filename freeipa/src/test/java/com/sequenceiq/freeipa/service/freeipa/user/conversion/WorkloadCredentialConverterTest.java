package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class WorkloadCredentialConverterTest {
    private WorkloadCredentialConverter underTest = new WorkloadCredentialConverter();

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

        WorkloadCredential workloadCredential = underTest.toWorkloadCredential(actorWorkloadCredentials);

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

        WorkloadCredential workloadCredential = underTest.toWorkloadCredential(actorWorkloadCredentials);

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

        WorkloadCredential workloadCredential = underTest.toWorkloadCredential(actorWorkloadCredentials);

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

        WorkloadCredential workloadCredential = underTest.toWorkloadCredential(actorWorkloadCredentials);

        assertEquals(passwordHash, workloadCredential.getHashedPassword());
        assertEquals(expectedPasswordExpiration, workloadCredential.getExpirationDate());
        assertIterableEquals(kerberosKeys, workloadCredential.getKeys());
        assertIterableEquals(sshPublicKeys, workloadCredential.getSshPublicKeys());
    }
}