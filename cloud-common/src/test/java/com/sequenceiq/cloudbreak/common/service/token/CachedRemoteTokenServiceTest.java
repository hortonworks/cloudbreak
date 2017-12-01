package com.sequenceiq.cloudbreak.common.service.token;

import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.jwt.crypto.sign.InvalidSignatureException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;

import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class CachedRemoteTokenServiceTest {

    private CachedRemoteTokenService cachedRemoteTokenService;

    private final String token;

    @Mock
    private IdentityClient identityClient;

    public CachedRemoteTokenServiceTest() throws IOException {
        token = FileReaderUtils.readFileFromClasspath("sso_token.txt");
    }

    @Test
    public void testLoadAuthenticationForInvalidPublicKey() throws IOException {
        String publicKey = FileReaderUtils.readFileFromClasspath("invalid_token_key.pub");
        cachedRemoteTokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", publicKey, identityClient);
        try {
            cachedRemoteTokenService.loadAuthentication(token);
        } catch (InvalidSignatureException e) {
            Assert.assertEquals("RSA Signature did not match content", e.getMessage());
        }
    }

    @Test
    public void testLoadAuthenticationForValidPublicKey() throws IOException {
        String publicKey = FileReaderUtils.readFileFromClasspath("valid_token_key.pub");
        cachedRemoteTokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", publicKey, identityClient);
        try {
            cachedRemoteTokenService.loadAuthentication(token);
        } catch (InvalidTokenException e) {
            Assert.assertEquals("The token has expired", e.getMessage());
        }
    }

    @Test
    public void testLoadAuthenticationWhenNoPublicKeyProvided() {
        when(identityClient.loadAuthentication(token, "clientSecret")).thenThrow(new InvalidTokenException("invalid_token"));
        cachedRemoteTokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", null, identityClient);
        try {
            cachedRemoteTokenService.loadAuthentication(token);
        } catch (InvalidTokenException e) {
            Assert.assertEquals("invalid_token", e.getMessage());
        }
    }

    @Test
    public void testLoadAuthenticationWithMissingUserField() throws IOException {
        String oauthToken = FileReaderUtils.readFileFromClasspath("oauth_token.txt");
        when(identityClient.loadAuthentication(oauthToken, "clientSecret")).thenThrow(new InvalidTokenException("invalid_token"));
        String publicKey = FileReaderUtils.readFileFromClasspath("valid_token_key.pub");
        cachedRemoteTokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", publicKey, identityClient);
        try {
            cachedRemoteTokenService.loadAuthentication(oauthToken);
        } catch (InvalidTokenException e) {
            Assert.assertEquals("invalid_token", e.getMessage());
        }
    }

    @Test
    public void testLoadAuthenticationWithOauthValidKey() throws IOException {
        String oauthToken = FileReaderUtils.readFileFromClasspath("oauth_token.txt");
        String publicKey = FileReaderUtils.readFileFromClasspath("valid_token_key.pub");
        cachedRemoteTokenService = new CachedRemoteTokenService("clientId", "clientSecret", "http://localhost:8089", publicKey, identityClient);
        cachedRemoteTokenService.loadAuthentication(oauthToken);
    }

}
