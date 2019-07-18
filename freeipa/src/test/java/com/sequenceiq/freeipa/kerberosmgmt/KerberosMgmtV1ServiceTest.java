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
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
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

    private static FreeIpa freeIpa;

    private static Stack stack;

    private static Host host;

    private static Service service;

    private static Keytab keytab;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private ThreadBasedUserCrnProvider threadBaseUserCrnProvider;

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
        try {
            underTest.generateServiceKeytab(request, ACCOUNT_ID);
        } catch (RuntimeException exp) {
            Assert.assertEquals("Failed to create host.", exp.getMessage());
        }
        Mockito.when(mockIpaClient.addHost(anyString())).thenReturn(host);
        try {
            underTest.generateServiceKeytab(request, ACCOUNT_ID);
        } catch (RuntimeException exp) {
            Assert.assertEquals("Failed to create service principal.", exp.getMessage());
        }
        Mockito.when(mockIpaClient.addService(anyString())).thenReturn(service);
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
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(USERCRN);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        ServiceKeytabResponse resp = underTest.generateServiceKeytab(request, ACCOUNT_ID);
        Assert.assertEquals(resp.getKeytab(), "keytab");
        Assert.assertEquals(resp.getServicePrincipal(), SERVICE_PRINCIPAL);
    }

    @Test
    public void testGetExistingServiceKeytab() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.getExistingKeytab(anyString())).thenReturn(keytab);
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setServiceName(USERCRN);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setServerHostName(HOST);
        ServiceKeytabResponse resp = underTest.getExistingServiceKeytab(request, ACCOUNT_ID);
        Assert.assertEquals(resp.getKeytab(), KEYTAB);
    }
}
