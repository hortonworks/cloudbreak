package com.sequenceiq.cloudbreak.common.service.token;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import com.sequenceiq.cloudbreak.client.CaasClient;
import com.sequenceiq.cloudbreak.client.CaasUser;
import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.cloudbreak.client.IntrospectResponse;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class CachedRemoteTokenServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final String token;

    @Mock
    private IdentityClient identityClient;

    @Mock
    private CaasClient caasClient;

    public CachedRemoteTokenServiceTest() throws IOException {
        token = FileReaderUtils.readFileFromClasspath("sso_token.txt");
    }

    @Test
    public void testLoadAuthenticationWithoutAudience() throws IOException {
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("No 'aud' claim in token");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", caasClient, identityClient);
        tokenService.loadAuthentication(token);
    }

    @Test
    public void testLoadAuthentication() throws IOException {
        CaasUser caasUser = new CaasUser();
        caasUser.setPreferredUsername("admin@example.com");
        when(caasClient.getUserInfo(anyString())).thenReturn(caasUser);
        IntrospectResponse introspectResponse = new IntrospectResponse();
        introspectResponse.setActive(true);
        introspectResponse.setAud("tenant");
        introspectResponse.setSub("admin@example.com");
        introspectResponse.setExp(System.currentTimeMillis() + 10000);
        when(caasClient.introSpect(anyString())).thenReturn(introspectResponse);

        String ssoToken = FileReaderUtils.readFileFromClasspath("sso_token_mac_signed.txt");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", caasClient, identityClient);
        OAuth2Authentication oAuth2Authentication = tokenService.loadAuthentication(ssoToken);
        Object principal = oAuth2Authentication.getPrincipal();
        Assert.assertEquals("admin@example.com", principal);
    }

    @Test
    public void testLoadAuthenticationWithInactiveToken() throws IOException {
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("The specified JWT token is not active");
        IntrospectResponse introspectResponse = new IntrospectResponse();
        introspectResponse.setActive(false);
        introspectResponse.setAud("tenant");
        introspectResponse.setSub("admin@example.com");
        introspectResponse.setExp(System.currentTimeMillis() + 10000);
        when(caasClient.introSpect(anyString())).thenReturn(introspectResponse);

        String ssoToken = FileReaderUtils.readFileFromClasspath("sso_token_mac_signed.txt");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", caasClient, identityClient);
        OAuth2Authentication oAuth2Authentication = tokenService.loadAuthentication(ssoToken);
        Object principal = oAuth2Authentication.getPrincipal();
        Assert.assertEquals("admin@example.com", principal);
    }

    @Test
    public void testLoadAuthenticationWithMissingUserField() throws IOException {
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("invalid_token");
        String oauthToken = FileReaderUtils.readFileFromClasspath("oauth_token.txt");
        when(identityClient.loadAuthentication(oauthToken, "clientSecret")).thenThrow(new InvalidTokenException("invalid_token"));
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", caasClient, identityClient);
        tokenService.loadAuthentication(oauthToken);
    }

    @Test
    public void testLoadAuthenticationWithOauthValidKey() throws IOException {
        String oauthToken = FileReaderUtils.readFileFromClasspath("oauth_token.txt");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", caasClient, identityClient);
        tokenService.loadAuthentication(oauthToken);
    }

}
