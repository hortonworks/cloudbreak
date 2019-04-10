package com.sequenceiq.cloudbreak.common.service.token;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
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

    private String crn = "crn:altus:iam:us-west-1:9d74eee4-cad1-45d7-b645-7ccf9edbb73d:user:f3b8abcde-e712-4f89-1234-be07183720d3";

    @Mock
    private IdentityClient identityClient;

    @Mock
    private CaasClient caasClient;

    @Mock
    private GrpcUmsClient umsClient;

    public CachedRemoteTokenServiceTest() throws IOException {
        token = FileReaderUtils.readFileFromClasspath("sso_token.txt");
    }

    @Test
    public void testLoadAuthenticationWithoutAudience() {
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("No 'tenant_name' claim in token");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, caasClient, identityClient);
        tokenService.loadAuthentication(token);
    }

    @Test
    public void whenInvalidCrnIsProvidedThrowInvalidTokenException() {
        when(umsClient.isUmsUsable(crn)).thenReturn(true);
        when(umsClient.getUserDetails(anyString(), anyString(), any(Optional.class))).thenThrow(new NullPointerException());
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("Invalid CRN provided");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, caasClient, identityClient);
        tokenService.loadAuthentication(crn);
    }

    @Test
    public void whenValidCrnIsProvidedLoadAuthentication() {
        when(umsClient.isUmsUsable(crn)).thenReturn(true);
        UserManagementProto.User user = UserManagementProto.User.newBuilder()
                .setCrn(crn)
                .setEmail("hansolo@cloudera.com").build();
        when(umsClient.getUserDetails(anyString(), anyString(), any(Optional.class))).thenReturn(user);
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, caasClient, identityClient);
        OAuth2Authentication authentication = (OAuth2Authentication) tokenService.loadAuthentication(crn);
        assertEquals("hansolo@cloudera.com", authentication.getPrincipal());
    }

    @Test
    public void testCrnBasedAuth() {
        when(umsClient.isUmsUsable(crn)).thenReturn(true);
        when(umsClient.getUserDetails(anyString(), anyString(), any(Optional.class))).thenThrow(new NullPointerException());
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("Invalid CRN provided");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, caasClient, identityClient);
        tokenService.loadAuthentication(crn);
    }

    @Test
    public void testLoadAuthentication() throws IOException {
        CaasUser caasUser = new CaasUser();
        caasUser.setPreferredUsername("admin@example.com");
        when(caasClient.getUserInfo(anyString())).thenReturn(caasUser);
        IntrospectResponse introspectResponse = new IntrospectResponse();
        introspectResponse.setActive(true);
        introspectResponse.setTenantName("tenant");
        introspectResponse.setSub("admin@example.com");
        introspectResponse.setExp(System.currentTimeMillis() + 10000);
        when(caasClient.introSpect(anyString())).thenReturn(introspectResponse);

        String ssoToken = FileReaderUtils.readFileFromClasspath("sso_token_mac_signed.txt");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, caasClient, identityClient);
        OAuth2Authentication oAuth2Authentication = tokenService.loadAuthentication(ssoToken);
        Object principal = oAuth2Authentication.getPrincipal();
        assertEquals("admin@example.com", principal);
    }

    @Test
    public void testLoadAuthenticationWithInactiveToken() throws IOException {
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("The specified JWT token is not active");
        IntrospectResponse introspectResponse = new IntrospectResponse();
        introspectResponse.setActive(false);
        introspectResponse.setTenantName("tenant");
        introspectResponse.setSub("admin@example.com");
        introspectResponse.setExp(System.currentTimeMillis() + 10000);
        when(caasClient.introSpect(anyString())).thenReturn(introspectResponse);

        String ssoToken = FileReaderUtils.readFileFromClasspath("sso_token_mac_signed.txt");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, caasClient, identityClient);
        OAuth2Authentication oAuth2Authentication = tokenService.loadAuthentication(ssoToken);
        Object principal = oAuth2Authentication.getPrincipal();
        assertEquals("admin@example.com", principal);
    }

    @Test
    public void testLoadAuthenticationWithMissingUserField() throws IOException {
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("invalid_token");
        String oauthToken = FileReaderUtils.readFileFromClasspath("oauth_token.txt");
        when(identityClient.loadAuthentication(oauthToken, "clientSecret")).thenThrow(new InvalidTokenException("invalid_token"));
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, caasClient, identityClient);
        tokenService.loadAuthentication(oauthToken);
    }

    @Test
    public void testLoadAuthenticationWithOauthValidKey() throws IOException {
        String oauthToken = FileReaderUtils.readFileFromClasspath("oauth_token.txt");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, caasClient, identityClient);
        tokenService.loadAuthentication(oauthToken);
    }

}
