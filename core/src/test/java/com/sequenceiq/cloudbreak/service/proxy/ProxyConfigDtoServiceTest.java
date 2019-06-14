package com.sequenceiq.cloudbreak.service.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.client.EnvironmentServiceClient;

@ExtendWith(MockitoExtension.class)
class ProxyConfigDtoServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EnvironmentServiceClient environmentServiceClient;

    @Mock
    private SecretService secretService;

    @Mock
    private ProxyEndpoint proxyEndpoint;

    @InjectMocks
    private ProxyConfigDtoService underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetWhenProxyConfigCouldBeFetched() {
        String name = "aProxyConfig";
        String host = "https://test.cloudera.com";
        Integer port = 8443;
        String decryptedSecretValue = "decrypted-secret-value";
        SecretResponse secretResponse = new SecretResponse();

        ProxyResponse proxyResponse = new ProxyResponse();
        proxyResponse.setName(name);
        proxyResponse.setHost(host);
        proxyResponse.setPort(port);
        proxyResponse.setUserName(secretResponse);
        proxyResponse.setPassword(secretResponse);

        when(environmentServiceClient.withCrn(anyString()).proxyV1Endpoint()).thenReturn(proxyEndpoint);
        when(proxyEndpoint.getByResourceCrn(anyString())).thenReturn(proxyResponse);
        when(secretService.getByResponse(any(SecretResponse.class))).thenReturn(decryptedSecretValue);

        ProxyConfig proxyConfig = underTest.getByCrn("crn:altus:environments:us-west-1:cloudera:proxyconfig:a2f0bee2-059e-433f-a9d0-2893c53419ad");

        verify(secretService, times(2)).getByResponse(secretResponse);
        assertEquals(proxyConfig.getName(), name);
        assertEquals(proxyConfig.getServerHost(), host);
        assertEquals(proxyConfig.getServerPort(), port);
        assertEquals(proxyConfig.getUserName(), decryptedSecretValue);
        assertEquals(proxyConfig.getPassword(), decryptedSecretValue);
    }

    @Test
    void testGetWhenProxyConfigCouldNotBeFetchedFromEnvironmentMS() {
        SecretResponse secretResponse = new SecretResponse();

        when(environmentServiceClient.withCrn(anyString()).proxyV1Endpoint()).thenReturn(proxyEndpoint);
        when(proxyEndpoint.getByResourceCrn(anyString())).thenThrow(new NotFoundException("The proxy config could not be found!"));


        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.getByCrn(
                "crn:altus:environments:us-west-1:cloudera:proxyconfig:a2f0bee2-059e-433f-a9d0-2893c53419ad"));

        verify(secretService, times(0)).getByResponse(secretResponse);
        assertEquals("Failed to get Proxy config from Environment service due to: 'The proxy config could not be found!' ", exception.getMessage());
    }

    @Test
    void testGetWhenSecretCouldNotBeFetchedFromVault() {
        String name = "aProxyConfig";
        String host = "https://test.cloudera.com";
        Integer port = 8443;
        String decryptedSecretValue = "decrypted-secret-value";
        SecretResponse secretResponse = new SecretResponse();

        ProxyResponse proxyResponse = new ProxyResponse();
        proxyResponse.setName(name);
        proxyResponse.setHost(host);
        proxyResponse.setPort(port);
        proxyResponse.setUserName(secretResponse);
        proxyResponse.setPassword(secretResponse);

        when(environmentServiceClient.withCrn(anyString()).proxyV1Endpoint()).thenReturn(proxyEndpoint);
        when(proxyEndpoint.getByResourceCrn(anyString())).thenReturn(proxyResponse);
        when(secretService.getByResponse(any(SecretResponse.class))).thenThrow(new VaultException("Vault token is invalid!"));


        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.getByCrn(
                "crn:altus:environments:us-west-1:cloudera:proxyconfig:a2f0bee2-059e-433f-a9d0-2893c53419ad"));

        assertEquals("Failed to get Proxy config related secret due to: 'Vault token is invalid!' ", exception.getMessage());
    }
}