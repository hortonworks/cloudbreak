package com.sequenceiq.freeipa.kerberosmgmt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.kerberosmgmt.v1.VaultPathBuilder;

public class VaultPathBuilderV1Test {
    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String CLUSTER_ID = "crn:cdp:datalake:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:datalake:54321-9876";

    private static final String HOST = "host1";

    private static final String SERVICE = "service1";

    @Test
    public void testVaultServicePrincipalPath() throws Exception {
        Assertions.assertEquals("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/host1/service1",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withClusterCrn(CLUSTER_ID)
                        .withServerHostName(HOST)
                        .withServiceName(SERVICE)
                        .build());
    }

    @Test
    public void testVaultServicePrincipalKeytabPathGenerateClusterId() throws Exception {
        Assertions.assertEquals("accountId/ServiceKeytab/keytab/12345-6789/accountId-12345-6789/host1/service1",
                new VaultPathBuilder()
                        .enableGeneratingClusterIdIfNotPresent()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withServerHostName(HOST)
                        .withServiceName(SERVICE)
                        .build());
    }

    @Test
    public void testVaultServicePrincipalPathGenerateClusterId() throws Exception {
        Assertions.assertEquals("accountId/ServiceKeytab/serviceprincipal/12345-6789/accountId-12345-6789/host1/service1",
                new VaultPathBuilder()
                        .enableGeneratingClusterIdIfNotPresent()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.SERVICE_PRINCIPAL)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withServerHostName(HOST)
                        .withServiceName(SERVICE)
                        .build());
    }

    @Test
    public void testVaultHostPrincipalPath() throws Exception {
        Assertions.assertEquals("accountId/HostKeytab/keytab/12345-6789/54321-9876/host1",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.HOST_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withClusterCrn(CLUSTER_ID)
                        .withServerHostName(HOST)
                        .build());
    }

    @Test
    public void testVaultHostPrincipalPathGenerateClusterId() throws Exception {
        Assertions.assertEquals("accountId/HostKeytab/keytab/12345-6789/accountId-12345-6789/host1",
                new VaultPathBuilder()
                        .enableGeneratingClusterIdIfNotPresent()
                        .withSecretType(VaultPathBuilder.SecretType.HOST_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withServerHostName(HOST)
                        .build());
    }

    @Test
    public void testVaultHostPath() throws Exception {
        Assertions.assertEquals("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/host1/",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withClusterCrn(CLUSTER_ID)
                        .withServerHostName(HOST)
                        .build());
    }

    @Test
    public void testVaultHostPathGenerateClusterId() throws Exception {
        Assertions.assertEquals("accountId/ServiceKeytab/keytab/12345-6789/accountId-12345-6789/host1/",
                new VaultPathBuilder()
                        .enableGeneratingClusterIdIfNotPresent()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withServerHostName(HOST)
                        .build());
    }

    @Test
    public void testVaultClusterCrnPath() throws Exception {
        Assertions.assertEquals("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withClusterCrn(CLUSTER_ID)
                        .build());
    }

    @Test
    public void testVaultClusterCrnPathGenerateClusterId() throws Exception {
        Assertions.assertEquals("accountId/ServiceKeytab/keytab/12345-6789/accountId-12345-6789/",
                new VaultPathBuilder()
                        .enableGeneratingClusterIdIfNotPresent()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .build());
    }

    @Test
    public void testVaultEnvironmentCrnPath() throws Exception {
        Assertions.assertEquals("accountId/ServiceKeytab/keytab/12345-6789/",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .build());
    }

    @Test
    public void testVaultClusterCrnPathNullClusterId() throws Exception {
        Assertions.assertEquals("accountId/ServiceKeytab/keytab/12345-6789/accountId-12345-6789/",
                new VaultPathBuilder()
                        .enableGeneratingClusterIdIfNotPresent()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withClusterCrn(null)
                        .build());
    }

    @Test
    public void testMissingRequiredConfiguration() throws Exception {
        Assertions.assertThrows(IllegalStateException.class,
                () -> new VaultPathBuilder()
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .build());
        Assertions.assertThrows(IllegalStateException.class,
                () -> new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .build());
        Assertions.assertThrows(IllegalStateException.class,
                () -> new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .build());
        Assertions.assertThrows(IllegalStateException.class,
                () -> new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .build());
    }
}