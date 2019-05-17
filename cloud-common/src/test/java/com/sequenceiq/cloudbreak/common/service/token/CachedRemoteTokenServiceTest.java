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
import com.sequenceiq.cloudbreak.client.IdentityClient;
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
    private GrpcUmsClient umsClient;

    public CachedRemoteTokenServiceTest() throws IOException {
        token = FileReaderUtils.readFileFromClasspath("sso_token.txt");
    }

    @Test
    public void whenInvalidCrnIsProvidedThrowInvalidTokenException() {
        when(umsClient.getUserDetails(anyString(), anyString(), any(Optional.class))).thenThrow(new NullPointerException());
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("Invalid CRN provided");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, identityClient);
        tokenService.loadAuthentication(crn);
    }

    @Test
    public void whenNullTokenIsProvidedThrowInvalidTokenException() {
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("Invalid JWT token");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, identityClient);
        tokenService.loadAuthentication(null);
    }

    @Test
    public void whenValidCrnIsProvidedLoadAuthentication() {
        UserManagementProto.User user = UserManagementProto.User.newBuilder()
                .setCrn(crn)
                .setEmail("hansolo@cloudera.com").build();
        when(umsClient.getUserDetails(anyString(), anyString(), any(Optional.class))).thenReturn(user);
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, identityClient);
        OAuth2Authentication authentication = (OAuth2Authentication) tokenService.loadAuthentication(crn);
        assertEquals("hansolo@cloudera.com", authentication.getPrincipal());
    }

    @Test
    public void testCrnBasedAuth() {
        when(umsClient.getUserDetails(anyString(), anyString(), any(Optional.class))).thenThrow(new NullPointerException());
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("Invalid CRN provided");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, identityClient);
        tokenService.loadAuthentication(crn);
    }

    @Test
    public void testLoadAuthenticationWithMissingUserField() throws IOException {
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage("invalid_token");
        String oauthToken = FileReaderUtils.readFileFromClasspath("oauth_token.txt");
        when(identityClient.loadAuthentication(oauthToken, "clientSecret")).thenThrow(new InvalidTokenException("invalid_token"));
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, identityClient);
        tokenService.loadAuthentication(oauthToken);
    }

    @Test
    public void testLoadAuthenticationWithOauthValidKey() throws IOException {
        String oauthToken = FileReaderUtils.readFileFromClasspath("oauth_token.txt");
        CachedRemoteTokenService tokenService = new CachedRemoteTokenService("clientId", "clientSecret",
                "http://localhost:8089", umsClient, identityClient);
        tokenService.loadAuthentication(oauthToken);
    }

}
