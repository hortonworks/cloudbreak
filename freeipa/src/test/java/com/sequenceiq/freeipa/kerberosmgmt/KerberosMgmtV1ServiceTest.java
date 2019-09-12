package com.sequenceiq.freeipa.kerberosmgmt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.client.model.Service;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtV1Service;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtVaultComponent;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtRoleComponent;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class KerberosMgmtV1ServiceTest {
    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String CLUSTER_ID = "crn:cdp:datalake:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:datalake:54321-9876";

    private static final String ACCOUNT_ID = "accountId";

    private static final String HOST = "host1";

    private static final String HOST_PRINCIPAL = "host1_principal";

    private static final String DOMAIN = "cloudera.com";

    private static final String SERVICE = "service1";

    private static final String KEYTAB = "keytab";

    private static final String SERVICE_PRINCIPAL = SERVICE + "/" + HOST + "@CLOUDERA.COM";

    private static final String SECRET = "secret";

    private static final String ADMIN = "admin";

    private static final String ROLE = "role1";

    private static final String PRIVILEGE = "privilege1";

    private static FreeIpa freeIpa;

    private static Stack stack;

    private static Host host;

    private static Service service;

    private static Keytab keytab;

    private static FreeIpaClientException ipaClientException;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private ThreadBasedUserCrnProvider threadBaseUserCrnProvider;

    @Mock
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @Mock
    private SecretService secretService;

    @Mock
    private KerberosMgmtVaultComponent vaultComponent;

    @Mock
    private KerberosMgmtRoleComponent roleComponent;

    @InjectMocks
    private KerberosMgmtV1Service underTest;

    @BeforeAll
    public static void init() {
        freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        stack = new Stack();
        host = new Host();
        host.setFqdn(HOST);
        host.setKrbprincipalname(HOST_PRINCIPAL);
        service = new Service();
        service.setKrbprincipalname(SERVICE_PRINCIPAL);
        service.setKrbcanonicalname(SERVICE_PRINCIPAL);
        keytab = new Keytab();
        keytab.setKeytab(KEYTAB);
        ipaClientException = new FreeIpaClientException("failure");
    }

    @Test
    public void testExistingServiceKeytabFailure() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(SERVICE);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenThrow(new NotFoundException("Stack not found"));
        Exception e = Assertions.assertThrows(NotFoundException.class,
                () -> underTest.getExistingServiceKeytab(request, ACCOUNT_ID));
        Assertions.assertEquals("Stack not found", e.getMessage());
        Mockito.reset(stackService);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenThrow(new NotFoundException("Stack not found"));
        e = Assertions.assertThrows(KeytabCreationException.class,
                () -> underTest.getExistingServiceKeytab(request, ACCOUNT_ID));
        Assertions.assertEquals("Failed to create service as realm was empty.", e.getMessage());
        Mockito.reset(freeIpaService);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.getExistingKeytab(anyString())).thenThrow(ipaClientException);
        Assertions.assertThrows(KeytabCreationException.class,
                () -> underTest.getExistingServiceKeytab(request, ACCOUNT_ID));
    }

    @Test
    public void testGenerateServiceKeytabFailure() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(SERVICE);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenThrow(new NotFoundException("Stack not found"));
        Exception e = Assertions.assertThrows(NotFoundException.class,
                () -> underTest.generateServiceKeytab(request, ACCOUNT_ID));
        Assertions.assertEquals("Stack not found", e.getMessage());
        Mockito.reset(stackService);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenThrow(new NotFoundException("Stack not found"));
        e = Assertions.assertThrows(KeytabCreationException.class,
                () -> underTest.generateServiceKeytab(request, ACCOUNT_ID));
        Assertions.assertEquals("Failed to create service as realm was empty.", e.getMessage());
        Mockito.reset(freeIpaService);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.addHost(anyString())).thenThrow(ipaClientException);
        e = Assertions.assertThrows(KeytabCreationException.class,
                () -> underTest.generateServiceKeytab(request, ACCOUNT_ID));
        Assertions.assertEquals("Failed to create host.", e.getMessage());
        Mockito.reset(mockIpaClient);
        Mockito.when(mockIpaClient.addHost(anyString())).thenReturn(host);
        Mockito.when(mockIpaClient.addService(anyString())).thenThrow(ipaClientException);
        e = Assertions.assertThrows(RuntimeException.class,
                () -> underTest.generateServiceKeytab(request, ACCOUNT_ID));
        Assertions.assertEquals("Failed to create service principal.", e.getMessage());
        Mockito.reset(mockIpaClient);
        Mockito.when(mockIpaClient.addHost(anyString())).thenReturn(host);
        Mockito.when(mockIpaClient.addService(anyString())).thenReturn(service);
        Mockito.when(mockIpaClient.getKeytab(anyString())).thenThrow(ipaClientException);
        e = Assertions.assertThrows(KeytabCreationException.class,
                () -> underTest.generateServiceKeytab(request, ACCOUNT_ID));
        Assertions.assertEquals("Failed to create keytab.", e.getMessage());
    }

    @Test
    public void testGenerateServiceKeytab() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        SecretResponse expectedSecret = new SecretResponse();
        Set<String> privileges = new HashSet<>();
        privileges.add(PRIVILEGE);
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setRoleName(ROLE);
        roleRequest.setPrivileges(privileges);
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(SERVICE);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        request.setRoleRequest(roleRequest);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(freeIpaClientFactory.getAdminUser()).thenReturn(ADMIN);
        Mockito.when(mockIpaClient.addHost(anyString())).thenReturn(host);
        Mockito.when(mockIpaClient.addService(anyString())).thenReturn(service);
        Mockito.when(mockIpaClient.getKeytab(anyString())).thenReturn(keytab);
        Mockito.when(vaultComponent.getSecretResponseForKeytab(any(ServiceKeytabRequest.class), anyString(), anyString())).thenReturn(expectedSecret);
        Mockito.when(vaultComponent.getSecretResponseForPrincipal(any(ServiceKeytabRequest.class), anyString(), anyString())).thenReturn(expectedSecret);
        ServiceKeytabResponse resp = underTest.generateServiceKeytab(request, ACCOUNT_ID);
        Assertions.assertEquals(expectedSecret, resp.getKeytab());
        Assertions.assertEquals(expectedSecret, resp.getServicePrincipal());
        Mockito.verify(mockIpaClient).addHost(HOST);
        Mockito.verify(mockIpaClient).addService(SERVICE_PRINCIPAL);
        Mockito.verify(mockIpaClient).allowServiceKeytabRetrieval(SERVICE_PRINCIPAL, ADMIN);
        Mockito.verify(mockIpaClient).getKeytab(SERVICE_PRINCIPAL);
        Mockito.verify(roleComponent).addRoleAndPrivileges(Optional.of(service), Optional.empty(), roleRequest, mockIpaClient);
    }

    @Test
    public void testGetExistingServiceKeytab() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        SecretResponse expectedSecret = new SecretResponse();
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(SERVICE);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.getExistingKeytab(anyString())).thenReturn(keytab);
        Mockito.when(vaultComponent.getSecretResponseForKeytab(any(ServiceKeytabRequest.class), anyString(), anyString())).thenReturn(expectedSecret);
        Mockito.when(vaultComponent.getSecretResponseForPrincipal(any(ServiceKeytabRequest.class), anyString(), anyString())).thenReturn(expectedSecret);
        ServiceKeytabResponse resp = underTest.getExistingServiceKeytab(request, ACCOUNT_ID);
        Assertions.assertEquals(expectedSecret, resp.getKeytab());
        Assertions.assertEquals(expectedSecret, resp.getServicePrincipal());
        Mockito.verify(mockIpaClient).getExistingKeytab(SERVICE_PRINCIPAL);
    }

    @Test
    public void testExistingHostKeytabFailure() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.showHost(anyString())).thenReturn(host);
        Mockito.when(mockIpaClient.getExistingKeytab(anyString())).thenThrow(ipaClientException);
        Exception e = Assertions.assertThrows(KeytabCreationException.class,
                () -> underTest.getExistingHostKeytab(request, ACCOUNT_ID));
        Assertions.assertEquals("Failed to fetch keytab.", e.getMessage());
    }

    @Test
    public void testGenerateHostKeytabFailure() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.addHost(anyString())).thenThrow(ipaClientException);
        Exception e = Assertions.assertThrows(KeytabCreationException.class,
                () -> underTest.generateHostKeytab(request, ACCOUNT_ID));
        Assertions.assertEquals("Failed to create host.", e.getMessage());
    }

    @Test
    public void testGenerateHostKeytab() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        SecretResponse expectedSecret = new SecretResponse();
        Set<String> privileges = new HashSet<>();
        privileges.add(PRIVILEGE);
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setRoleName(ROLE);
        roleRequest.setPrivileges(privileges);
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        request.setRoleRequest(roleRequest);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(freeIpaClientFactory.getAdminUser()).thenReturn(ADMIN);
        Mockito.when(mockIpaClient.addHost(anyString())).thenReturn(host);
        Mockito.when(mockIpaClient.getKeytab(anyString())).thenReturn(keytab);
        Mockito.when(vaultComponent.getSecretResponseForKeytab(any(HostKeytabRequest.class), anyString(), anyString())).thenReturn(expectedSecret);
        Mockito.when(vaultComponent.getSecretResponseForPrincipal(any(HostKeytabRequest.class), anyString(), anyString())).thenReturn(expectedSecret);
        HostKeytabResponse resp = underTest.generateHostKeytab(request, ACCOUNT_ID);
        Assertions.assertEquals(expectedSecret, resp.getKeytab());
        Assertions.assertEquals(expectedSecret, resp.getHostPrincipal());
        Mockito.verify(mockIpaClient).addHost(HOST);
        Mockito.verify(mockIpaClient).allowHostKeytabRetrieval(HOST, ADMIN);
        Mockito.verify(mockIpaClient).getKeytab(HOST_PRINCIPAL);
        Mockito.verify(roleComponent).addRoleAndPrivileges(Optional.empty(), Optional.of(host), roleRequest, mockIpaClient);
    }

    @Test
    public void testGetExistingHostKeytab() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        SecretResponse expectedSecret = new SecretResponse();
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.showHost(anyString())).thenReturn(host);
        Mockito.when(mockIpaClient.getExistingKeytab(anyString())).thenReturn(keytab);
        Mockito.when(vaultComponent.getSecretResponseForKeytab(any(HostKeytabRequest.class), anyString(), anyString())).thenReturn(expectedSecret);
        Mockito.when(vaultComponent.getSecretResponseForPrincipal(any(HostKeytabRequest.class), anyString(), anyString())).thenReturn(expectedSecret);
        HostKeytabResponse resp = underTest.getExistingHostKeytab(request, ACCOUNT_ID);
        Assertions.assertEquals(expectedSecret, resp.getKeytab());
        Assertions.assertEquals(expectedSecret, resp.getHostPrincipal());
        Mockito.verify(mockIpaClient).getExistingKeytab(HOST_PRINCIPAL);
    }

    @Test
    public void testDeleteServicePrincipal() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        ServicePrincipalRequest request = new ServicePrincipalRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        request.setServiceName(SERVICE);
        request.setClusterCrn(CLUSTER_ID);
        request.setRoleName(ROLE);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        underTest.deleteServicePrincipal(request, ACCOUNT_ID);
        Mockito.verify(mockIpaClient).deleteService(SERVICE_PRINCIPAL);
        Mockito.verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/serviceprincipal/12345-6789/54321-9876/host1/service1");
        Mockito.verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/host1/service1");
        Mockito.verify(roleComponent).deleteRoleIfItIsNoLongerUsed(ROLE, mockIpaClient);
    }

    @Test
    public void testDeleteHost() throws Exception {
        Set<Service> services = new HashSet<>();
        services.add(service);
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        HostRequest request = new HostRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        request.setClusterCrn(CLUSTER_ID);
        request.setRoleName(ROLE);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.findAllService()).thenReturn(services);
        underTest.deleteHost(request, ACCOUNT_ID);
        Mockito.verify(mockIpaClient).deleteService(SERVICE_PRINCIPAL);
        Mockito.verify(mockIpaClient).deleteHost(HOST);
        Mockito.verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/serviceprincipal/12345-6789/54321-9876/host1/");
        Mockito.verify(vaultComponent).recursivelyCleanupVault("accountId/ServiceKeytab/keytab/12345-6789/54321-9876/host1/");
        Mockito.verify(vaultComponent).recursivelyCleanupVault("accountId/HostKeytab/serviceprincipal/12345-6789/54321-9876/host1");
        Mockito.verify(vaultComponent).recursivelyCleanupVault("accountId/HostKeytab/keytab/12345-6789/54321-9876/host1");
        Mockito.verify(roleComponent).deleteRoleIfItIsNoLongerUsed(ROLE, mockIpaClient);
    }

    @Test
    public void testCleanupByCluster() throws Exception {
        VaultCleanupRequest request = new VaultCleanupRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setClusterCrn(CLUSTER_ID);
        underTest.cleanupByCluster(request, ACCOUNT_ID);
        Mockito.verify(vaultComponent).cleanupSecrets(ENVIRONMENT_ID, CLUSTER_ID, ACCOUNT_ID);
    }

    @Test
    public void testCleanupByEnvironment() throws Exception {
        underTest.cleanupByEnvironment(ENVIRONMENT_ID, ACCOUNT_ID);
        Mockito.verify(vaultComponent).cleanupSecrets(ENVIRONMENT_ID, null, ACCOUNT_ID);
    }

}
