package com.sequenceiq.freeipa.kerberosmgmt.v1;

import static com.sequenceiq.freeipa.client.FreeIpaErrorCodes.DUPLICATE_ENTRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.client.model.Service;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.KeytabCache;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class KeytabCommonServiceTest {

    private static final String ACCOUNT_ID = "ACCID";

    private static final String ENVIRONMENT_CRN = "ENVCRN";

    private static final String DOMAIN = "ipa.domain";

    private static final String PRINCIPAL = "principal";

    private static final String HOST = "hostname";

    private static final String KEYTAB = "keytab";

    private static final int NOT_FOUND = 4001;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private KeytabCacheService keytabCacheService;

    @Mock
    private KerberosMgmtRoleComponent roleComponent;

    @InjectMocks
    private KeytabCommonService underTest;

    @Test
    public void testGetFreeIpaStack() {
        Stack stack = new Stack();
        when(stackService.getFreeIpaStackWithMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);

        Stack result = underTest.getFreeIpaStackWithMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertEquals(stack, result);
    }

    @Test
    public void testRealm() {
        Stack stack = new Stack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        String result = underTest.getRealm(stack);

        assertEquals(DOMAIN.toUpperCase(), result);
    }

    @Test
    public void testRealmMissingDomain() {
        Stack stack = new Stack();
        FreeIpa freeIpa = new FreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        assertThrows(KeytabCreationException.class, () -> underTest.getRealm(stack));
    }

    @Test
    public void testRealmFreeIpaMissing() {
        Stack stack = new Stack();
        when(freeIpaService.findByStack(stack)).thenThrow(new NotFoundException("expected"));

        assertThrows(KeytabCreationException.class, () -> underTest.getRealm(stack));
    }

    @Test
    public void testGetKeytab() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        Keytab keytab = new Keytab();
        keytab.setKeytab(KEYTAB);
        when(ipaClient.getKeytab(PRINCIPAL)).thenReturn(keytab);
        KeytabCache keytabCache = new KeytabCache();
        when(keytabCacheService.saveOrUpdate(ENVIRONMENT_CRN, PRINCIPAL, HOST, KEYTAB)).thenReturn(keytabCache);

        KeytabCache result = underTest.getKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient);

        assertEquals(keytabCache, result);
    }

    @Test
    public void testGetKeytabRetryable() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(ipaClient.getKeytab(PRINCIPAL)).thenThrow(new RetryableFreeIpaClientException("expected", new FreeIpaClientException("inner")));

        assertThrows(RetryableFreeIpaClientException.class, () -> underTest.getKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient));

        verifyNoInteractions(keytabCacheService);
    }

    @Test
    public void testGetKeytabNonRetryable() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(ipaClient.getKeytab(PRINCIPAL)).thenThrow(new FreeIpaClientException("expected"));

        assertThrows(KeytabCreationException.class, () -> underTest.getKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient));

        verifyNoInteractions(keytabCacheService);
    }

    @Test
    public void testGetExistingKeytabFromCache() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        KeytabCache keytabCache = new KeytabCache();
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.of(keytabCache));

        KeytabCache result = underTest.getExistingKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient);

        assertEquals(keytabCache, result);

        verifyNoInteractions(ipaClient);
    }

    @Test
    public void testGetExistingKeytabFromIpaClient() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        KeytabCache keytabCache = new KeytabCache();
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.empty());
        Keytab keytab = new Keytab();
        keytab.setKeytab(KEYTAB);
        when(ipaClient.getExistingKeytab(PRINCIPAL)).thenReturn(keytab);
        when(keytabCacheService.saveOrUpdate(ENVIRONMENT_CRN, PRINCIPAL, HOST, KEYTAB)).thenReturn(keytabCache);

        KeytabCache result = underTest.getExistingKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient);

        assertEquals(keytabCache, result);
    }

    @Test
    public void testGetExistingKeytabRetryableException() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.empty());
        when(ipaClient.getExistingKeytab(PRINCIPAL)).thenThrow(new RetryableFreeIpaClientException("expected", new FreeIpaClientException("inner")));

        assertThrows(RetryableFreeIpaClientException.class, () -> underTest.getExistingKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient));
    }

    @Test
    public void testGetExistingKeytabFreeIpaClientException() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.empty());
        when(ipaClient.getExistingKeytab(PRINCIPAL)).thenThrow(new FreeIpaClientException("expected"));

        assertThrows(KeytabCreationException.class, () -> underTest.getExistingKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient));
    }

    @Test
    public void testAddHostAlreadyExists() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        RoleRequest roleRequest = new RoleRequest();
        Host host = new Host();
        when(ipaClient.showHost(HOST)).thenReturn(host);

        Host result = underTest.addHost(HOST, roleRequest, ipaClient);

        verify(ipaClient).allowHostKeytabRetrieval(HOST, FreeIpaClientFactory.ADMIN_USER);
        ArgumentCaptor<Optional<Service>> serviceCaptor = ArgumentCaptor.forClass(Optional.class);
        ArgumentCaptor<Optional<Host>> hostCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(roleComponent).addRoleAndPrivileges(serviceCaptor.capture(), hostCaptor.capture(), eq(roleRequest), eq(ipaClient));
        assertTrue(serviceCaptor.getValue().isEmpty());
        assertEquals(host, hostCaptor.getValue().get());
        assertEquals(host, result);
    }

    @Test
    public void testAddHost() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        RoleRequest roleRequest = new RoleRequest();
        Host host = new Host();
        when(ipaClient.showHost(HOST)).thenThrow(new FreeIpaClientException("notfound", new JsonRpcClientException(NOT_FOUND, "notfound", null)));
        when(ipaClient.addHost(HOST)).thenReturn(host);

        Host result = underTest.addHost(HOST, roleRequest, ipaClient);

        verify(ipaClient).allowHostKeytabRetrieval(HOST, FreeIpaClientFactory.ADMIN_USER);
        ArgumentCaptor<Optional<Service>> serviceCaptor = ArgumentCaptor.forClass(Optional.class);
        ArgumentCaptor<Optional<Host>> hostCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(roleComponent).addRoleAndPrivileges(serviceCaptor.capture(), hostCaptor.capture(), eq(roleRequest), eq(ipaClient));
        assertTrue(serviceCaptor.getValue().isEmpty());
        assertEquals(host, hostCaptor.getValue().get());
        assertEquals(host, result);
    }

    @Test
    public void testAddHostDuplicateEntry() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        RoleRequest roleRequest = new RoleRequest();
        Host host = new Host();
        when(ipaClient.showHost(HOST))
                .thenThrow(new FreeIpaClientException("notfound", new JsonRpcClientException(NOT_FOUND, "notfound", null)))
                .thenReturn(host);
        when(ipaClient.addHost(HOST))
                .thenThrow(new FreeIpaClientException("duplicate", new JsonRpcClientException(DUPLICATE_ENTRY.getValue(), "duplicate", null)));

        Host result = underTest.addHost(HOST, roleRequest, ipaClient);

        verify(ipaClient).allowHostKeytabRetrieval(HOST, FreeIpaClientFactory.ADMIN_USER);
        ArgumentCaptor<Optional<Service>> serviceCaptor = ArgumentCaptor.forClass(Optional.class);
        ArgumentCaptor<Optional<Host>> hostCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(roleComponent).addRoleAndPrivileges(serviceCaptor.capture(), hostCaptor.capture(), eq(roleRequest), eq(ipaClient));
        assertTrue(serviceCaptor.getValue().isEmpty());
        assertEquals(host, hostCaptor.getValue().get());
        assertEquals(host, result);
    }

    @Test
    public void testAddHostFreeIpaException() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        RoleRequest roleRequest = new RoleRequest();
        when(ipaClient.showHost(HOST)).thenThrow(new FreeIpaClientException("expected"));

        assertThrows(KeytabCreationException.class, () -> underTest.addHost(HOST, roleRequest, ipaClient));
    }

    @Test
    public void testAddHostRetryableException() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        RoleRequest roleRequest = new RoleRequest();
        when(ipaClient.showHost(HOST)).thenThrow(new RetryableFreeIpaClientException("expected", new FreeIpaClientException("inner")));

        assertThrows(RetryableFreeIpaClientException.class, () -> underTest.addHost(HOST, roleRequest, ipaClient));
    }

    @Test
    public void testAddHostALlowKeytabRetrievalError() throws FreeIpaClientException {
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        RoleRequest roleRequest = new RoleRequest();
        Host host = new Host();
        when(ipaClient.showHost(HOST)).thenReturn(host);
        doThrow(new FreeIpaClientException("expected")).when(ipaClient).allowHostKeytabRetrieval(HOST, FreeIpaClientFactory.ADMIN_USER);

        assertThrows(KeytabCreationException.class, () -> underTest.addHost(HOST, roleRequest, ipaClient));
    }

    @Test
    public void testConstructPrincipal() {
        String result = underTest.constructPrincipal("service", HOST, "realm");

        assertEquals("service/" + HOST + "@realm", result);
    }
}