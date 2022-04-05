package com.sequenceiq.cloudbreak.service.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.client.EnvironmentServiceCrnClient;

@ExtendWith(MockitoExtension.class)
class ProxyConfigDtoServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EnvironmentServiceCrnClient environmentServiceCrnClient;

    @Mock
    private SecretService secretService;

    @Mock
    private ProxyEndpoint proxyEndpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

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
        String noProxyList = "noproxy.com";
        SecretResponse secretResponse = new SecretResponse();

        ProxyResponse proxyResponse = new ProxyResponse();
        proxyResponse.setName(name);
        proxyResponse.setHost(host);
        proxyResponse.setPort(port);
        proxyResponse.setUserName(secretResponse);
        proxyResponse.setPassword(secretResponse);
        proxyResponse.setNoProxyHosts(noProxyList);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentServiceCrnClient.withCrn(anyString()).proxyV1Endpoint()).thenReturn(proxyEndpoint);
        when(proxyEndpoint.getByResourceCrn(anyString())).thenReturn(proxyResponse);
        when(secretService.getByResponse(any(SecretResponse.class))).thenReturn(decryptedSecretValue);

        ProxyConfig proxyConfig = underTest.getByCrn("crn:cdp:environments:us-west-1:cloudera:proxyconfig:a2f0bee2-059e-433f-a9d0-2893c53419ad");

        verify(secretService, times(2)).getByResponse(secretResponse);
        assertEquals(proxyConfig.getName(), name);
        assertEquals(proxyConfig.getServerHost(), host);
        assertEquals(proxyConfig.getServerPort(), port);
        assertTrue(proxyConfig.getProxyAuthentication().isPresent());
        assertEquals(proxyConfig.getProxyAuthentication().get().getUserName(), decryptedSecretValue);
        assertEquals(proxyConfig.getProxyAuthentication().get().getPassword(), decryptedSecretValue);
        assertEquals(proxyConfig.getNoProxyHosts(), noProxyList);
    }

    @Test
    void testGetWhenProxyConfigCouldNotBeFetchedFromEnvironmentMS() {
        SecretResponse secretResponse = new SecretResponse();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentServiceCrnClient.withCrn(anyString()).proxyV1Endpoint()).thenReturn(proxyEndpoint);
        when(proxyEndpoint.getByResourceCrn(anyString())).thenThrow(new NotFoundException("The proxy config could not be found!"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.getByCrn(
                "crn:cdp:environments:us-west-1:cloudera:proxyconfig:a2f0bee2-059e-433f-a9d0-2893c53419ad"));

        verify(secretService, times(0)).getByResponse(secretResponse);
        assertEquals("Failed to get Proxy config from Environment service due to: 'The proxy config could not be found!' ", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidUserPasswords")
    void testGetWhenProxyConfigUserPasswordEmpty(String user, String password) {
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentServiceCrnClient.withCrn(anyString()).proxyV1Endpoint()).thenReturn(proxyEndpoint);
        when(proxyEndpoint.getByResourceCrn(anyString())).thenReturn(proxyResponse);
        when(secretService.getByResponse(any(SecretResponse.class))).thenReturn(user).thenReturn(password);

        ProxyConfig proxyConfig = underTest.getByCrn("crn:cdp:environments:us-west-1:cloudera:proxyconfig:a2f0bee2-059e-433f-a9d0-2893c53419ad");

        assertFalse(proxyConfig.getProxyAuthentication().isPresent());
    }

    @ParameterizedTest
    @MethodSource("invalidUserPasswordSecrets")
    void testGetWhenProxyConfigUserPasswordSecretsAreEmpty(SecretResponse user, SecretResponse password) {
        String name = "aProxyConfig";
        String host = "https://test.cloudera.com";
        Integer port = 8443;

        ProxyResponse proxyResponse = new ProxyResponse();
        proxyResponse.setName(name);
        proxyResponse.setHost(host);
        proxyResponse.setPort(port);
        proxyResponse.setUserName(user);
        proxyResponse.setPassword(password);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentServiceCrnClient.withCrn(anyString()).proxyV1Endpoint()).thenReturn(proxyEndpoint);
        when(proxyEndpoint.getByResourceCrn(anyString())).thenReturn(proxyResponse);

        ProxyConfig proxyConfig = underTest.getByCrn("crn:cdp:environments:us-west-1:cloudera:proxyconfig:a2f0bee2-059e-433f-a9d0-2893c53419ad");

        assertFalse(proxyConfig.getProxyAuthentication().isPresent());
    }

    @Test
    void testGetWhenSecretCouldNotBeFetchedFromVault() {
        String name = "aProxyConfig";
        String host = "https://test.cloudera.com";
        Integer port = 8443;
        SecretResponse secretResponse = new SecretResponse();

        ProxyResponse proxyResponse = new ProxyResponse();
        proxyResponse.setName(name);
        proxyResponse.setHost(host);
        proxyResponse.setPort(port);
        proxyResponse.setUserName(secretResponse);
        proxyResponse.setPassword(secretResponse);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(environmentServiceCrnClient.withCrn(anyString()).proxyV1Endpoint()).thenReturn(proxyEndpoint);
        when(proxyEndpoint.getByResourceCrn(anyString())).thenReturn(proxyResponse);
        when(secretService.getByResponse(any(SecretResponse.class))).thenThrow(new VaultException("Vault token is invalid!"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.getByCrn(
                "crn:cdp:environments:us-west-1:cloudera:proxyconfig:a2f0bee2-059e-433f-a9d0-2893c53419ad"));

        assertEquals("Failed to get Proxy config related secret due to: 'Vault token is invalid!' ", exception.getMessage());
    }

    private static Stream<Arguments> invalidUserPasswords() {
        return Stream.of(
                Arguments.of("   ", "  "),
                Arguments.of("   ", null),
                Arguments.of(null, "\t"),
                Arguments.of("user", ""),
                Arguments.of("user", null),
                Arguments.of("user", "   "),
                Arguments.of("", "pwd"),
                Arguments.of(null, "pwd"),
                Arguments.of("  ", "pwd"),
                Arguments.of("", ""),
                Arguments.of(null, null));
    }

    private static Stream<Arguments> invalidUserPasswordSecrets() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(null, new SecretResponse()),
                Arguments.of(new SecretResponse(), null));
    }
}
