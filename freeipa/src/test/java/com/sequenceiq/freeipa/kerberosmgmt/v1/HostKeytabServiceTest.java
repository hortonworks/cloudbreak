package com.sequenceiq.freeipa.kerberosmgmt.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.entity.KeytabCache;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@ExtendWith(MockitoExtension.class)
class HostKeytabServiceTest {

    private static final String ACCOUNT_ID = "ACCID";

    private static final String ENVIRONMENT_CRN = "ENVCRN";

    @Mock
    private KeytabCommonService keytabCommonService;

    @Mock
    private StringToSecretResponseConverter secretResponseConverter;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private KerberosMgmtRoleComponent roleComponent;

    @InjectMocks
    private HostKeytabService underTest;

    @Test
    public void testGenerateHostKeytabPrivilegeDoesntExist() throws FreeIpaClientException {
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setRoleRequest(new RoleRequest());
        Stack stack = new Stack();
        when(keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), ACCOUNT_ID)).thenReturn(stack);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(roleComponent.privilegesExist(request.getRoleRequest(), freeIpaClient)).thenReturn(Boolean.FALSE);

        assertThrows(BadRequestException.class, () -> underTest.generateHostKeytab(request, ACCOUNT_ID));
    }

    @Test
    public void testGenerateHostKeytabGetExisting() throws FreeIpaClientException {
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setRoleRequest(new RoleRequest());
        request.setDoNotRecreateKeytab(Boolean.TRUE);
        request.setServerHostName("asdf");
        Stack stack = new Stack();
        when(keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), ACCOUNT_ID)).thenReturn(stack);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(roleComponent.privilegesExist(request.getRoleRequest(), freeIpaClient)).thenReturn(Boolean.TRUE);
        Host host = new Host();
        host.setHasKeytab(Boolean.TRUE);
        host.setKrbprincipalname("dfdf");
        when(keytabCommonService.addHost(request.getServerHostName(), request.getRoleRequest(), freeIpaClient)).thenReturn(host);
        KeytabCache keytabCache = mock(KeytabCache.class);
        Secret keytabSecret = new Secret("keytab", "keytabSecret");
        Secret principalSecret = new Secret("principal", "principalSecret");
        when(keytabCache.getKeytab()).thenReturn(keytabSecret);
        when(keytabCache.getPrincipal()).thenReturn(principalSecret);
        when(keytabCommonService.getExistingKeytab(request.getEnvironmentCrn(), host.getKrbprincipalname(), request.getServerHostName(), freeIpaClient))
                .thenReturn(keytabCache);
        SecretResponse keytabResponse = new SecretResponse();
        keytabResponse.setSecretPath("keytabPath");
        when(secretResponseConverter.convert(keytabCache.getKeytab().getSecret())).thenReturn(keytabResponse);
        SecretResponse principalResponse = new SecretResponse();
        principalResponse.setSecretPath("principalPath");
        when(secretResponseConverter.convert(keytabCache.getPrincipal().getSecret())).thenReturn(principalResponse);

        HostKeytabResponse response = underTest.generateHostKeytab(request, ACCOUNT_ID);

        assertEquals(keytabResponse, response.getKeytab());
        assertEquals(principalResponse, response.getHostPrincipal());
    }

    @Test
    public void testGenerateHostKeytabHostDontHaveKeytab() throws FreeIpaClientException {
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setRoleRequest(new RoleRequest());
        request.setDoNotRecreateKeytab(Boolean.TRUE);
        request.setServerHostName("asdf");
        Stack stack = new Stack();
        when(keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), ACCOUNT_ID)).thenReturn(stack);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(roleComponent.privilegesExist(request.getRoleRequest(), freeIpaClient)).thenReturn(Boolean.TRUE);
        Host host = new Host();
        host.setHasKeytab(Boolean.FALSE);
        host.setKrbprincipalname("dfdf");
        when(keytabCommonService.addHost(request.getServerHostName(), request.getRoleRequest(), freeIpaClient)).thenReturn(host);
        KeytabCache keytabCache = mock(KeytabCache.class);
        Secret keytabSecret = new Secret("keytab", "keytabSecret");
        Secret principalSecret = new Secret("principal", "principalSecret");
        when(keytabCache.getKeytab()).thenReturn(keytabSecret);
        when(keytabCache.getPrincipal()).thenReturn(principalSecret);
        when(keytabCommonService.getKeytab(request.getEnvironmentCrn(), host.getKrbprincipalname(), request.getServerHostName(), freeIpaClient))
                .thenReturn(keytabCache);
        SecretResponse keytabResponse = new SecretResponse();
        keytabResponse.setSecretPath("keytabPath");
        when(secretResponseConverter.convert(keytabCache.getKeytab().getSecret())).thenReturn(keytabResponse);
        SecretResponse principalResponse = new SecretResponse();
        principalResponse.setSecretPath("principalPath");
        when(secretResponseConverter.convert(keytabCache.getPrincipal().getSecret())).thenReturn(principalResponse);

        HostKeytabResponse response = underTest.generateHostKeytab(request, ACCOUNT_ID);

        assertEquals(keytabResponse, response.getKeytab());
        assertEquals(principalResponse, response.getHostPrincipal());
    }

    @Test
    public void testGenerateHostKeytabDoNotRecreateFalse() throws FreeIpaClientException {
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setRoleRequest(new RoleRequest());
        request.setDoNotRecreateKeytab(Boolean.FALSE);
        request.setServerHostName("asdf");
        Stack stack = new Stack();
        when(keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), ACCOUNT_ID)).thenReturn(stack);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(roleComponent.privilegesExist(request.getRoleRequest(), freeIpaClient)).thenReturn(Boolean.TRUE);
        Host host = new Host();
        host.setHasKeytab(Boolean.TRUE);
        host.setKrbprincipalname("dfdf");
        when(keytabCommonService.addHost(request.getServerHostName(), request.getRoleRequest(), freeIpaClient)).thenReturn(host);
        KeytabCache keytabCache = mock(KeytabCache.class);
        Secret keytabSecret = new Secret("keytab", "keytabSecret");
        Secret principalSecret = new Secret("principal", "principalSecret");
        when(keytabCache.getKeytab()).thenReturn(keytabSecret);
        when(keytabCache.getPrincipal()).thenReturn(principalSecret);
        when(keytabCommonService.getKeytab(request.getEnvironmentCrn(), host.getKrbprincipalname(), request.getServerHostName(), freeIpaClient))
                .thenReturn(keytabCache);
        SecretResponse keytabResponse = new SecretResponse();
        keytabResponse.setSecretPath("keytabPath");
        when(secretResponseConverter.convert(keytabCache.getKeytab().getSecret())).thenReturn(keytabResponse);
        SecretResponse principalResponse = new SecretResponse();
        principalResponse.setSecretPath("principalPath");
        when(secretResponseConverter.convert(keytabCache.getPrincipal().getSecret())).thenReturn(principalResponse);

        HostKeytabResponse response = underTest.generateHostKeytab(request, ACCOUNT_ID);

        assertEquals(keytabResponse, response.getKeytab());
        assertEquals(principalResponse, response.getHostPrincipal());
    }

    @Test
    public void testGetExistingHostKeytabRoleRequestSet() {
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setRoleRequest(new RoleRequest());

        assertThrows(BadRequestException.class, () -> underTest.getExistingHostKeytab(request, ACCOUNT_ID));
    }

    @Test
    public void testGetExistingKeytab() throws FreeIpaClientException {
        HostKeytabRequest request = new HostKeytabRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setServerHostName("asdf");
        Stack stack = new Stack();
        when(keytabCommonService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), ACCOUNT_ID)).thenReturn(stack);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        Host host = new Host();
        host.setKrbprincipalname("dfdf");
        when(freeIpaClient.showHost(request.getServerHostName())).thenReturn(host);
        KeytabCache keytabCache = mock(KeytabCache.class);
        Secret keytabSecret = new Secret("keytab", "keytabSecret");
        Secret principalSecret = new Secret("principal", "principalSecret");
        when(keytabCache.getKeytab()).thenReturn(keytabSecret);
        when(keytabCache.getPrincipal()).thenReturn(principalSecret);
        when(keytabCommonService.getExistingKeytab(request.getEnvironmentCrn(), host.getKrbprincipalname(), request.getServerHostName(), freeIpaClient))
                .thenReturn(keytabCache);
        SecretResponse keytabResponse = new SecretResponse();
        keytabResponse.setSecretPath("keytabPath");
        when(secretResponseConverter.convert(keytabCache.getKeytab().getSecret())).thenReturn(keytabResponse);
        SecretResponse principalResponse = new SecretResponse();
        principalResponse.setSecretPath("principalPath");
        when(secretResponseConverter.convert(keytabCache.getPrincipal().getSecret())).thenReturn(principalResponse);

        HostKeytabResponse response = underTest.getExistingHostKeytab(request, ACCOUNT_ID);

        assertEquals(keytabResponse, response.getKeytab());
        assertEquals(principalResponse, response.getHostPrincipal());
    }
}