package com.sequenceiq.environment.api.v1.proxy.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;

@ExtendWith(MockitoExtension.class)
public class ProxyConfigAuthValidatorTest {
    @InjectMocks
    private ProxyConfigAuthValidator underTest;

    @Test
    public void testValidProxyConfigWithoutAuth() {
        ProxyRequest proxyRequest = new ProxyRequest();
        assertTrue(underTest.isValid(proxyRequest, null));
    }

    @Test
    public void testValidProxyConfigWithAuth() {
        ProxyRequest proxyRequest = new ProxyRequest();
        proxyRequest.setUserName("user");
        proxyRequest.setPassword("pwd");
        assertTrue(underTest.isValid(proxyRequest, null));
    }

    @ParameterizedTest
    @MethodSource("invalidUserPasswords")
    public void testProxyConfigWithInvalidUserPwd(String user, String password) {
        ProxyRequest proxyRequest = new ProxyRequest();
        proxyRequest.setUserName(user);
        proxyRequest.setPassword(password);
        assertFalse(underTest.isValid(proxyRequest, null));
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
                Arguments.of("  ", "pwd")
        );
    }
}
