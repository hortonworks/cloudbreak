package com.sequenceiq.freeipa.kerberosmgmt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
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
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class KerberosMgmtV1ServiceTest {
    private static final String ENVIRONMENT_ID = "environmentId";

    private static final String ACCOUNT_ID = "accountId";

    private static final String USERCRN = "user1";

    private static final String HOST = "host1";

    private static final String DOMAIN = "cloudera.com";

    private static final String KEYTAB = "keytab";

    private static final String SERVICE_PRINCIPAL = "principal";

    private static final String SECRET = "secret";

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

    @InjectMocks
    private KerberosMgmtV1Service underTest;

    @BeforeClass
    public static void init() {

        freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        stack = new Stack();
        host = new Host();
        host.setFqdn(HOST);
        service = new Service();
        service.setKrbprincipalname(SERVICE_PRINCIPAL);
        keytab = new Keytab();
        keytab.setKeytab(KEYTAB);
        ipaClientException = new FreeIpaClientException("failure");
    }

    @Test
    public void testExistingServiceKeytabFailure() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(USERCRN);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenThrow(new NotFoundException("Stack not found"));
        try {
            underTest.getExistingServiceKeytab(request, ACCOUNT_ID);
        } catch (NotFoundException exp) {
            Assert.assertEquals("Stack not found", exp.getMessage());
        }
        Mockito.reset(stackService);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenThrow(new NotFoundException("Stack not found"));
        try {
            underTest.getExistingServiceKeytab(request, ACCOUNT_ID);
        } catch (KeytabCreationException exp) {
            Assert.assertEquals("Failed to create service as realm was empty.", exp.getMessage());
        }
        Mockito.reset(freeIpaService);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.getExistingKeytab(anyString())).thenReturn(new Keytab());
        try {
            underTest.getExistingServiceKeytab(request, ACCOUNT_ID);
        } catch (RuntimeException exp) {
            Assert.assertEquals("Failed to fetch keytab.", exp.getMessage());
        }
    }

    @Test
    public void testGenerateServiceKeytabFailure() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(USERCRN);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        try {
            Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenThrow(new NotFoundException("Stack not found"));
            underTest.generateServiceKeytab(request, ACCOUNT_ID);
        } catch (NotFoundException exp) {
            Assert.assertEquals("Stack not found", exp.getMessage());
        }
        Mockito.reset(stackService);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenThrow(new NotFoundException("Stack not found"));
        try {
            underTest.generateServiceKeytab(request, ACCOUNT_ID);
        } catch (KeytabCreationException exp) {
            Assert.assertEquals("Failed to create service as realm was empty.", exp.getMessage());
        }
        Mockito.reset(freeIpaService);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.addHost(anyString())).thenThrow(ipaClientException);
        try {

            underTest.generateServiceKeytab(request, ACCOUNT_ID);
        } catch (RuntimeException exp) {
            Assert.assertEquals("Failed to create host.", exp.getMessage());
        }
        Mockito.reset(mockIpaClient);
        Mockito.when(mockIpaClient.addHost(anyString())).thenReturn(host);
        Mockito.when(mockIpaClient.addService(anyString())).thenThrow(ipaClientException);
        try {
            underTest.generateServiceKeytab(request, ACCOUNT_ID);
        } catch (RuntimeException exp) {
            Assert.assertEquals("Failed to create service principal.", exp.getMessage());
        }
        Mockito.reset(mockIpaClient);
        Mockito.when(mockIpaClient.addService(anyString())).thenReturn(service);
        Mockito.when(mockIpaClient.getKeytab(anyString())).thenThrow(ipaClientException);
        try {
            underTest.generateServiceKeytab(request, ACCOUNT_ID);
        } catch (RuntimeException exp) {
            Assert.assertEquals("Failed to create keytab.", exp.getMessage());
        }
    }

    @Test
    public void testGenerateServiceKeytab() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.addHost(anyString())).thenReturn(host);
        Mockito.when(mockIpaClient.addService(anyString())).thenReturn(service);
        Mockito.when(mockIpaClient.getKeytab(anyString())).thenReturn(keytab);
        Mockito.when(secretService.put(anyString(), anyString())).thenReturn(SECRET);
        Mockito.when(stringToSecretResponseConverter.convert(SECRET)).thenReturn(new SecretResponse());
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(USERCRN);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        ServiceKeytabResponse resp = underTest.generateServiceKeytab(request, ACCOUNT_ID);
        Assert.assertNotNull(resp.getKeytab());
        Assert.assertNotNull(resp.getServicePrincipal());
    }

    @Test
    public void testGetExistingServiceKeytab() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.getExistingKeytab(anyString())).thenReturn(keytab);
        Mockito.when(secretService.put(anyString(), anyString())).thenReturn(SECRET);
        Mockito.when(stringToSecretResponseConverter.convert(SECRET)).thenReturn(new SecretResponse());
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(USERCRN);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        ServiceKeytabResponse resp = underTest.getExistingServiceKeytab(request, ACCOUNT_ID);
        Assert.assertNotNull(resp.getKeytab());
        Assert.assertNotNull(resp.getServicePrincipal());
    }

    @Test
    public void testExistingHostKeytabFailure() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.getExistingKeytab(anyString())).thenReturn(new Keytab());
        try {
            underTest.getExistingHostKeytab(request, ACCOUNT_ID);
        } catch (RuntimeException exp) {
            Assert.assertEquals("Failed to fetch keytab.", exp.getMessage());
        }
    }

    @Test
    public void testGenerateHostKeytabFailure() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.addHost(anyString())).thenThrow(ipaClientException);
        try {
            underTest.generateHostKeytab(request, ACCOUNT_ID);
        } catch (RuntimeException exp) {
            Assert.assertEquals("Failed to create host.", exp.getMessage());
        }
    }

    @Test
    public void testGenerateHostKeytab() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.addHost(anyString())).thenReturn(host);
        Mockito.when(mockIpaClient.getKeytab(anyString())).thenReturn(keytab);
        Mockito.when(secretService.put(anyString(), anyString())).thenReturn(SECRET);
        Mockito.when(stringToSecretResponseConverter.convert(SECRET)).thenReturn(new SecretResponse());
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        HostKeytabResponse resp = underTest.generateHostKeytab(request, ACCOUNT_ID);
        Assert.assertNotNull(resp.getKeytab());
        Assert.assertNotNull(resp.getHostPrincipal());
    }

    @Test
    public void testGetExistingHostKeytab() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.getExistingKeytab(anyString())).thenReturn(keytab);
        Mockito.when(secretService.put(anyString(), anyString())).thenReturn(SECRET);
        Mockito.when(stringToSecretResponseConverter.convert(SECRET)).thenReturn(new SecretResponse());
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        HostKeytabResponse resp = underTest.getExistingHostKeytab(request, ACCOUNT_ID);
        Assert.assertNotNull(resp.getKeytab());
        Assert.assertNotNull(resp.getHostPrincipal());
    }
}
