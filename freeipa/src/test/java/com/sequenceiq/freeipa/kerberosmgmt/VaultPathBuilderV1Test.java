package com.sequenceiq.freeipa.kerberosmgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.freeipa.kerberosmgmt.v1.VaultPathBuilder;

public class VaultPathBuilderV1Test {
    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String CLUSTER_ID = "crn:cdp:datalake:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:datalake:54321-9876";

    private static final String INVALID_CRN = "Invalid Crn";

    private static final String HOST = "host1";

    private static final String SERVICE = "service1";

    @Test
    public void testVaultServicePrincipalPath() throws Exception {
        assertEquals("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/host1/service1",
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
    public void testVaultServicePrincipalKeytabPathWithoutClusterId() throws Exception {
        assertEquals("accountId/ServiceKeytab/keytab/12345-6789/host1/service1",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withServerHostName(HOST)
                        .withServiceName(SERVICE)
                        .build());
    }

    @Test
    public void testVaultServicePrincipalPathWithoutClusterId() throws Exception {
        assertEquals("accountId/ServiceKeytab/serviceprincipal/12345-6789/host1/service1",
                new VaultPathBuilder()
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
        assertEquals("accountId/HostKeytab/keytab/12345-6789/54321-9876/host1",
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
    public void testVaultHostPrincipalPathWithoutClusterId() throws Exception {
        assertEquals("accountId/HostKeytab/keytab/12345-6789/host1",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.HOST_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withServerHostName(HOST)
                        .build());
    }

    @Test
    public void testVaultHostPath() throws Exception {
        assertEquals("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/host1/",
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
    public void testVaultHostPathWithoutClusterId() throws Exception {
        assertEquals("accountId/ServiceKeytab/keytab/12345-6789/host1/",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withServerHostName(HOST)
                        .build());
    }

    @Test
    public void testVaultClusterCrnPath() throws Exception {
        assertEquals("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withClusterCrn(CLUSTER_ID)
                        .build());
    }

    @Test
    public void testVaultEnvironmentCrnPath() throws Exception {
        assertEquals("accountId/ServiceKeytab/keytab/12345-6789/",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .build());
    }

    @Test
    public void testVaultClusterCrnPathNullClusterId() throws Exception {
        assertEquals("accountId/ServiceKeytab/keytab/12345-6789/",
                new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .withClusterCrn(null)
                        .build());
    }

    @Test
    public void testMissingRequiredConfiguration() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> new VaultPathBuilder()
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .build());
        assertThrows(IllegalStateException.class,
                () -> new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .build());
        assertThrows(IllegalStateException.class,
                () -> new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withEnvironmentCrn(ENVIRONMENT_ID)
                        .build());
        assertThrows(IllegalStateException.class,
                () -> new VaultPathBuilder()
                        .withSecretType(VaultPathBuilder.SecretType.SERVICE_KEYTAB)
                        .withAccountId(ACCOUNT_ID)
                        .withSubType(VaultPathBuilder.SecretSubType.KEYTAB)
                        .build());
    }

    @Test
    public void testInvalidCrn() throws Exception {
        assertThrows(CrnParseException.class,
                () -> new VaultPathBuilder()
                        .withEnvironmentCrn(INVALID_CRN));
        assertThrows(CrnParseException.class,
                () -> new VaultPathBuilder()
                        .withEnvironmentCrn(INVALID_CRN));
    }
}