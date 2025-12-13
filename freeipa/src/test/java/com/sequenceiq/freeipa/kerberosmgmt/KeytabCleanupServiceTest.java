package com.sequenceiq.freeipa.kerberosmgmt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.client.model.Service;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtRoleComponent;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtVaultComponent;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KeytabCacheService;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KeytabCleanupService;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KeytabCommonService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.host.HostDeletionService;

@ExtendWith(MockitoExtension.class)
public class KeytabCleanupServiceTest {
    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String CLUSTER_ID = "crn:cdp:datalake:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:datalake:54321-9876";

    private static final String ACCOUNT_ID = "accountId";

    private static final String HOST = "host1";

    private static final String HOST_PRINCIPAL = "host1_principal";

    private static final String DOMAIN = "cloudera.com";

    private static final String REALM = DOMAIN.toUpperCase(Locale.ROOT);

    private static final String SERVICE = "service1";

    private static final String KEYTAB = "keytab";

    private static final String SERVICE_PRINCIPAL = SERVICE + "/" + HOST + "@" + REALM;

    private static final String ROLE = "role1";

    private static FreeIpa freeIpa;

    private static Stack stack;

    private static Host host;

    private static Service service;

    private static Keytab keytab;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private KerberosMgmtVaultComponent vaultComponent;

    @Mock
    private KerberosMgmtRoleComponent roleComponent;

    @Mock
    private HostDeletionService hostDeletionService;

    @Mock
    private KeytabCommonService keytabCommonService;

    @Mock
    private KeytabCacheService keytabCacheService;

    @InjectMocks
    private KeytabCleanupService underTest;

    @BeforeAll
    public static void init() {
        freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        stack = new Stack();
        host = new Host();
        host.setFqdn(HOST);
        host.setKrbprincipalname(HOST_PRINCIPAL);
        service = new Service();
        service.setKrbprincipalname(List.of(SERVICE_PRINCIPAL));
        service.setKrbcanonicalname(SERVICE_PRINCIPAL);
        keytab = new Keytab();
        keytab.setKeytab(KEYTAB);
    }

    @Test
    public void testDeleteServicePrincipal() throws Exception {
        FreeIpaClient mockIpaClient = mock(FreeIpaClient.class);
        ServicePrincipalRequest request = new ServicePrincipalRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        request.setServiceName(SERVICE);
        request.setClusterCrn(CLUSTER_ID);
        request.setRoleName(ROLE);
        when(keytabCommonService.getFreeIpaStackWithMdcContext(anyString(), anyString())).thenReturn(stack);
        when(keytabCommonService.getRealm(stack)).thenReturn(REALM);
        when(keytabCommonService.constructPrincipal(SERVICE, HOST, REALM)).thenReturn(SERVICE_PRINCIPAL);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);

        underTest.deleteServicePrincipal(request, ACCOUNT_ID);

        verify(mockIpaClient).deleteService(SERVICE_PRINCIPAL);
        verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/serviceprincipal/12345-6789/54321-9876/host1/service1");
        verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/host1/service1");
        verify(roleComponent).deleteRoleIfItIsNoLongerUsed(ROLE, mockIpaClient);
        verify(keytabCacheService).deleteByEnvironmentCrnAndPrincipal(ENVIRONMENT_ID, SERVICE_PRINCIPAL);
    }

    @Test
    public void testDeleteHost() throws Exception {
        Set<Service> services = new HashSet<>();
        services.add(service);
        FreeIpaClient mockIpaClient = mock(FreeIpaClient.class);
        HostRequest request = new HostRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        request.setClusterCrn(CLUSTER_ID);
        request.setRoleName(ROLE);
        when(freeIpaClientFactory.getFreeIpaClientByAccountAndEnvironment(anyString(), anyString())).thenReturn(mockIpaClient);
        when(mockIpaClient.findAllServiceCanonicalNamesOnly(HOST)).thenReturn(services);

        underTest.deleteHost(request, ACCOUNT_ID);

        verify(mockIpaClient).deleteService(SERVICE_PRINCIPAL);
        verify(hostDeletionService).deleteHostsWithDeleteException(mockIpaClient, Set.of(HOST));
        verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/serviceprincipal/12345-6789/54321-9876/host1/");
        verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/host1/");
        verify(vaultComponent).recursivelyCleanupVault("accountId/HostKeytab/serviceprincipal/12345-6789/54321-9876/host1");
        verify(vaultComponent).recursivelyCleanupVault("accountId/HostKeytab/keytab/12345-6789/54321-9876/host1");
        verify(roleComponent).deleteRoleIfItIsNoLongerUsed(ROLE, mockIpaClient);
        verify(keytabCacheService).deleteByEnvironmentCrnAndPrincipal(ENVIRONMENT_ID, SERVICE_PRINCIPAL);
    }

    @Test
    public void testRemoveHostRelatedKerberosConfiguration() throws Exception {
        Set<Service> services = new HashSet<>();
        services.add(service);
        FreeIpaClient mockIpaClient = mock(FreeIpaClient.class);
        HostRequest request = new HostRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        request.setClusterCrn(CLUSTER_ID);
        request.setRoleName(ROLE);

        underTest.removeHostRelatedKerberosConfiguration(request, ACCOUNT_ID, mockIpaClient, services);

        verify(mockIpaClient).deleteService(SERVICE_PRINCIPAL);
        verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/serviceprincipal/12345-6789/54321-9876/host1/");
        verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/host1/");
        verify(vaultComponent).recursivelyCleanupVault("accountId/HostKeytab/serviceprincipal/12345-6789/54321-9876/host1");
        verify(vaultComponent).recursivelyCleanupVault("accountId/HostKeytab/keytab/12345-6789/54321-9876/host1");
        verify(roleComponent).deleteRoleIfItIsNoLongerUsed(ROLE, mockIpaClient);
        verify(keytabCacheService).deleteByEnvironmentCrnAndPrincipal(ENVIRONMENT_ID, SERVICE_PRINCIPAL);
    }

    @Test
    public void testCleanupByCluster() {
        VaultCleanupRequest request = new VaultCleanupRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setClusterCrn(CLUSTER_ID);

        underTest.cleanupByCluster(request, ACCOUNT_ID);

        verify(vaultComponent).cleanupSecrets(ENVIRONMENT_ID, CLUSTER_ID, ACCOUNT_ID);
    }

    @Test
    public void testCleanupByEnvironment() throws Exception {
        underTest.cleanupByEnvironment(ENVIRONMENT_ID, ACCOUNT_ID);

        verify(vaultComponent).cleanupSecrets(ENVIRONMENT_ID, null, ACCOUNT_ID);
        verify(keytabCacheService).deleteByEnvironmentCrn(ENVIRONMENT_ID);
    }
}
