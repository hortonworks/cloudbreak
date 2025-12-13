package com.sequenceiq.environment.environment.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.validation.validators.PublicKeyValidator;

class AuthenticationDtoConverterTest {

    public static final String LOGIN = "login";

    public static final String PUBLIC_KEY = "ssh-rsa public-key";

    public static final String PUBLIC_KEY_ID = "id";

    private final PublicKeyValidator publicKeyValidator = mock(PublicKeyValidator.class);

    private final AuthenticationDtoConverter underTest = new AuthenticationDtoConverter(publicKeyValidator);

    @BeforeEach
    void setUp() {
        when(publicKeyValidator.validatePublicKey(anyString())).thenReturn(ValidationResult.empty());
    }

    @Test
    void testDtoToAuthentication() {
        AuthenticationDto dto = AuthenticationDto.builder()
                .withLoginUserName(LOGIN)
                .withPublicKey(PUBLIC_KEY)
                .withPublicKeyId(PUBLIC_KEY_ID)
                .withManagedKey(true)
                .build();

        EnvironmentAuthentication result = underTest.dtoToAuthentication(dto);

        verify(publicKeyValidator, times(1)).validatePublicKey(anyString());
        assertEquals(dto.getLoginUserName(), result.getLoginUserName());
        assertEquals("ssh-rsa public-key login", result.getPublicKey());
        assertEquals(dto.getPublicKeyId(), result.getPublicKeyId());
        assertEquals(dto.isManagedKey(), result.isManagedKey());
    }

    @Test
    void testAuthenticationToDto() {
        EnvironmentAuthentication environment = new EnvironmentAuthentication();
        environment.setId(123L);
        environment.setLoginUserName(LOGIN);
        environment.setPublicKey(PUBLIC_KEY);
        environment.setPublicKeyId(PUBLIC_KEY_ID);
        environment.setManagedKey(true);

        AuthenticationDto result = underTest.authenticationToDto(environment);

        verify(publicKeyValidator, times(0)).validatePublicKey(anyString());
        assertEquals(environment.getLoginUserName(), result.getLoginUserName());
        assertEquals(environment.getPublicKey(), result.getPublicKey());
        assertEquals(environment.getPublicKeyId(), result.getPublicKeyId());
        assertEquals(environment.isManagedKey(), result.isManagedKey());
    }

    @Test
    void testSshKeyCreation() {
        String testKey = "ssh-rsa AAAASASFAS3532== banana@apple.com";
        AuthenticationDto dto = AuthenticationDto.builder()
                .withLoginUserName(LOGIN)
                .withPublicKey(testKey)
                .withPublicKeyId(PUBLIC_KEY_ID)
                .withManagedKey(true)
                .build();

        EnvironmentAuthentication environmentAuthentication = underTest.dtoToAuthentication(dto);

        verify(publicKeyValidator, times(1)).validatePublicKey(anyString());
        assertEquals("ssh-rsa AAAASASFAS3532== login", environmentAuthentication.getPublicKey());
    }

    @Test
    void testDtoToSshUpdatedAuthenticationWhenValidSshKey() {
        String testKey = "ssh-rsa AAAASASFAS3532== banana@apple.com";
        AuthenticationDto dto = AuthenticationDto.builder()
                .withLoginUserName(LOGIN)
                .withPublicKey(testKey)
                .withPublicKeyId(PUBLIC_KEY_ID)
                .withManagedKey(true)
                .build();

        EnvironmentAuthentication actual = underTest.dtoToSshUpdatedAuthentication(dto);

        assertEquals("ssh-rsa AAAASASFAS3532== login", actual.getPublicKey());
        assertNull(actual.getLoginUserName());
        assertNull(actual.getPublicKeyId());
    }
}
