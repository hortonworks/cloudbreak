package com.sequenceiq.freeipa.kerberosmgmt.v1;

import static com.sequenceiq.freeipa.client.FreeIpaErrorCodes.DUPLICATE_ENTRY;
import static com.sequenceiq.freeipa.client.FreeIpaErrorCodes.EXECUTION_ERROR;
import static com.sequenceiq.freeipa.client.FreeIpaErrorCodes.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Service;
import com.sequenceiq.freeipa.entity.KeytabCache;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@ExtendWith(MockitoExtension.class)
class ServiceKeytabServiceTest {

    private static final String ACCOUNT_ID = "ACCID";

    private static final String ENVIRONMENT_CRN = "ENVCRN";

    private static final String REALM = "realm";

    private static final String SERVICE_NAME = "serviceName";

    private static final String HOST = "host";

    private static final String PRINCIPAL = "principal";

    private static final String ALIAS = "alias";

    private static final String ALIAS_PRINCIPAL = "aliasPrincipal";

    @Mock
    private KeytabCommonService keytabCommonService;

    @Mock
    private StringToSecretResponseConverter secretResponseConverter;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private KerberosMgmtRoleComponent roleComponent;

    @Mock
    private KeytabCacheService keytabCacheService;

    private final Secret keytab = new Secret("keytab", "keytabSecret");

    private final Secret principal = new Secret("principal", "principalSecret");

    private final KeytabCache keytabCache = mock(KeytabCache.class);

    private final Stack stack = new Stack();

    private final SecretResponse keytabResponse = new SecretResponse("eng", "keytabPath");

    private final SecretResponse principalResponse = new SecretResponse("eng", "principalPath");

    @InjectMocks
    private ServiceKeytabService underTest;

    @BeforeEach
    public void init() {
        lenient().when(keytabCache.getKeytab()).thenReturn(keytab);
        lenient().when(keytabCache.getPrincipal()).thenReturn(principal);
        lenient().when(keytabCommonService.getFreeIpaStackWithMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        lenient().when(keytabCommonService.getRealm(stack)).thenReturn(REALM);
        lenient().when(keytabCommonService.constructPrincipal(SERVICE_NAME, HOST, REALM)).thenReturn(PRINCIPAL);
        lenient().when(secretResponseConverter.convert(keytab.getSecret())).thenReturn(keytabResponse);
        lenient().when(secretResponseConverter.convert(principal.getSecret())).thenReturn(principalResponse);
    }

    @Test
    public void testGetExistingCached() throws FreeIpaClientException {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.of(keytabCache));

        ServiceKeytabResponse result = underTest.getExistingServiceKeytab(request, ACCOUNT_ID);

        assertEquals(keytabResponse, result.getKeytab());
        assertEquals(principalResponse, result.getServicePrincipal());
    }

    @Test
    public void testGetExistingFromIpa() throws FreeIpaClientException {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.empty());
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(ipaClient);
        when(keytabCommonService.getExistingKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient)).thenReturn(keytabCache);

        ServiceKeytabResponse result = underTest.getExistingServiceKeytab(request, ACCOUNT_ID);

        assertEquals(keytabResponse, result.getKeytab());
        assertEquals(principalResponse, result.getServicePrincipal());
    }

    @Test
    public void testGetExistingWithRoleRequest() {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        request.setRoleRequest(new RoleRequest());

        assertThrows(KeytabCreationException.class, () -> underTest.getExistingServiceKeytab(request, ACCOUNT_ID));
    }

    @Test
    public void testGenerateExistingCached() throws FreeIpaClientException {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        request.setDoNotRecreateKeytab(Boolean.TRUE);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.of(keytabCache));

        ServiceKeytabResponse result = underTest.generateServiceKeytab(request, ACCOUNT_ID);

        assertEquals(keytabResponse, result.getKeytab());
        assertEquals(principalResponse, result.getServicePrincipal());
    }

    @Test
    public void testGenerateExistingCachedDoNotRecreateFalse() throws FreeIpaClientException {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        request.setDoNotRecreateKeytab(Boolean.FALSE);
        request.setServerHostNameAlias(ALIAS);
        RoleRequest roleRequest = new RoleRequest();
        request.setRoleRequest(roleRequest);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.of(keytabCache));
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(ipaClient);
        when(roleComponent.privilegesExist(roleRequest, ipaClient)).thenReturn(Boolean.TRUE);
        Service service = new Service();
        service.setKrbcanonicalname(PRINCIPAL);
        when(ipaClient.showService(PRINCIPAL)).thenReturn(service);
        when(keytabCommonService.constructPrincipal(SERVICE_NAME, ALIAS, REALM)).thenReturn(ALIAS_PRINCIPAL);
        when(keytabCommonService.getKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient)).thenReturn(keytabCache);

        ServiceKeytabResponse result = underTest.generateServiceKeytab(request, ACCOUNT_ID);

        verify(keytabCommonService).addHost(eq(HOST), isNull(), eq(ipaClient));
        verify(ipaClient).addServiceAlias(PRINCIPAL, ALIAS_PRINCIPAL);
        verify(roleComponent).addRoleAndPrivileges(Optional.of(service), Optional.empty(), roleRequest, ipaClient);
        assertEquals(keytabResponse, result.getKeytab());
        assertEquals(principalResponse, result.getServicePrincipal());
    }

    @Test
    public void testGenerateExistingNotCachedDoNotRecreateTrue() throws FreeIpaClientException {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        request.setDoNotRecreateKeytab(Boolean.TRUE);
        request.setServerHostNameAlias(ALIAS);
        RoleRequest roleRequest = new RoleRequest();
        request.setRoleRequest(roleRequest);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.empty());
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(ipaClient);
        when(roleComponent.privilegesExist(roleRequest, ipaClient)).thenReturn(Boolean.TRUE);
        Service service = new Service();
        service.setKrbcanonicalname(PRINCIPAL);
        service.setHasKeytab(Boolean.TRUE);
        when(ipaClient.showService(PRINCIPAL)).thenReturn(service);
        when(keytabCommonService.constructPrincipal(SERVICE_NAME, ALIAS, REALM)).thenReturn(ALIAS_PRINCIPAL);
        when(keytabCommonService.getExistingKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient)).thenReturn(keytabCache);

        ServiceKeytabResponse result = underTest.generateServiceKeytab(request, ACCOUNT_ID);

        verify(ipaClient).addServiceAlias(PRINCIPAL, ALIAS_PRINCIPAL);
        verify(roleComponent).addRoleAndPrivileges(Optional.of(service), Optional.empty(), roleRequest, ipaClient);
        assertEquals(keytabResponse, result.getKeytab());
        assertEquals(principalResponse, result.getServicePrincipal());
    }

    @Test
    public void testGenerateExistingNotCachedServiceMissing() throws FreeIpaClientException {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        request.setDoNotRecreateKeytab(Boolean.TRUE);
        request.setServerHostNameAlias(ALIAS);
        RoleRequest roleRequest = new RoleRequest();
        request.setRoleRequest(roleRequest);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.empty());
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(ipaClient);
        when(roleComponent.privilegesExist(roleRequest, ipaClient)).thenReturn(Boolean.TRUE);
        Service service = new Service();
        service.setKrbcanonicalname(PRINCIPAL);
        service.setHasKeytab(Boolean.TRUE);
        when(ipaClient.showService(PRINCIPAL))
                .thenThrow(new FreeIpaClientException("notfound", new JsonRpcClientException(NOT_FOUND.getValue(), "notfound", null)));
        when(ipaClient.addService(PRINCIPAL)).thenReturn(service);
        when(keytabCommonService.constructPrincipal(SERVICE_NAME, ALIAS, REALM)).thenReturn(ALIAS_PRINCIPAL);
        when(keytabCommonService.getExistingKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient)).thenReturn(keytabCache);

        ServiceKeytabResponse result = underTest.generateServiceKeytab(request, ACCOUNT_ID);

        verify(ipaClient).addServiceAlias(PRINCIPAL, ALIAS_PRINCIPAL);
        verify(roleComponent).addRoleAndPrivileges(Optional.of(service), Optional.empty(), roleRequest, ipaClient);
        assertEquals(keytabResponse, result.getKeytab());
        assertEquals(principalResponse, result.getServicePrincipal());
    }

    @Test
    public void testGenerateExistingNotCachedServiceMissingAddThrowDuplicate() throws FreeIpaClientException {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        request.setDoNotRecreateKeytab(Boolean.TRUE);
        request.setServerHostNameAlias(ALIAS);
        RoleRequest roleRequest = new RoleRequest();
        request.setRoleRequest(roleRequest);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.empty());
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(ipaClient);
        when(roleComponent.privilegesExist(roleRequest, ipaClient)).thenReturn(Boolean.TRUE);
        Service service = new Service();
        service.setKrbcanonicalname(PRINCIPAL);
        service.setHasKeytab(Boolean.TRUE);
        when(ipaClient.showService(PRINCIPAL))
                .thenThrow(new FreeIpaClientException("notfound", new JsonRpcClientException(NOT_FOUND.getValue(), "notfound", null)))
                .thenReturn(service);
        when(ipaClient.addService(PRINCIPAL))
                .thenThrow(new FreeIpaClientException("notfound", new JsonRpcClientException(DUPLICATE_ENTRY.getValue(), "notfound", null)));
        when(keytabCommonService.constructPrincipal(SERVICE_NAME, ALIAS, REALM)).thenReturn(ALIAS_PRINCIPAL);
        when(keytabCommonService.getExistingKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient)).thenReturn(keytabCache);

        ServiceKeytabResponse result = underTest.generateServiceKeytab(request, ACCOUNT_ID);

        verify(ipaClient).addServiceAlias(PRINCIPAL, ALIAS_PRINCIPAL);
        verify(roleComponent).addRoleAndPrivileges(Optional.of(service), Optional.empty(), roleRequest, ipaClient);
        assertEquals(keytabResponse, result.getKeytab());
        assertEquals(principalResponse, result.getServicePrincipal());
    }

    @Test
    public void testGenerateExistingNotCachedDoNotRecreateTrueAliasExists() throws FreeIpaClientException {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        request.setDoNotRecreateKeytab(Boolean.TRUE);
        request.setServerHostNameAlias(ALIAS);
        RoleRequest roleRequest = new RoleRequest();
        request.setRoleRequest(roleRequest);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.empty());
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(ipaClient);
        when(roleComponent.privilegesExist(roleRequest, ipaClient)).thenReturn(Boolean.TRUE);
        Service service = new Service();
        service.setKrbcanonicalname(PRINCIPAL);
        service.setHasKeytab(Boolean.TRUE);
        when(ipaClient.showService(PRINCIPAL)).thenReturn(service);
        when(keytabCommonService.constructPrincipal(SERVICE_NAME, ALIAS, REALM)).thenReturn(ALIAS_PRINCIPAL);
        when(keytabCommonService.getExistingKeytab(ENVIRONMENT_CRN, PRINCIPAL, HOST, ipaClient)).thenReturn(keytabCache);
        doThrow(new FreeIpaClientException("notfound", new JsonRpcClientException(EXECUTION_ERROR.getValue(), "notfound", null))).
                when(ipaClient).addServiceAlias(PRINCIPAL, ALIAS_PRINCIPAL);

        ServiceKeytabResponse result = underTest.generateServiceKeytab(request, ACCOUNT_ID);

        verify(roleComponent).addRoleAndPrivileges(Optional.of(service), Optional.empty(), roleRequest, ipaClient);
        assertEquals(keytabResponse, result.getKeytab());
        assertEquals(principalResponse, result.getServicePrincipal());
    }

    @Test
    public void testGenerateNotCachedDoNotRecreateFalsePrivilegeMissing() throws FreeIpaClientException {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServiceName(SERVICE_NAME);
        request.setServerHostName(HOST);
        request.setDoNotRecreateKeytab(Boolean.FALSE);
        request.setServerHostNameAlias(ALIAS);
        RoleRequest roleRequest = new RoleRequest();
        request.setRoleRequest(roleRequest);
        when(keytabCacheService.findByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, PRINCIPAL)).thenReturn(Optional.of(keytabCache));
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(ipaClient);
        when(roleComponent.privilegesExist(roleRequest, ipaClient)).thenReturn(Boolean.FALSE);

        assertThrows(KeytabCreationException.class, () -> underTest.generateServiceKeytab(request, ACCOUNT_ID));

        verify(keytabCommonService, times(0)).addHost(eq(HOST), isNull(), eq(ipaClient));
    }
}