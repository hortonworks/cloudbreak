package com.sequenceiq.environment.credential.validation;

import static org.mockito.Mockito.mock;

import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.environment.credential.validation.definition.CredentialDefinitionService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class CredentialValidatorTest {
    public static final String VALIDATOR_PRIVATE_FIELD_NAME = "enabledPlatforms";

    private static final String PLATFORM_ONE = "provider1";

    private static final String PLATFORM_TWO = "provider2";

    private CredentialValidator credentialValidatorUnderTest;

    @MockBean
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "indirectly used")
    private CredentialDefinitionService credentialDefinitionService;

    public CredentialValidatorTest() {
        credentialValidatorUnderTest = new CredentialValidator();
        credentialDefinitionService = mock(CredentialDefinitionService.class);
    }

    @Test
    public void testValidateCredentialCloudPlatformOnlyOne() {
        ReflectionTestUtils.setField(credentialValidatorUnderTest,
                VALIDATOR_PRIVATE_FIELD_NAME, Set.of(PLATFORM_ONE));
        Assertions.assertDoesNotThrow(runValidation());
    }

    @Test
    public void testValidateCredentialCloudPlatformNotIncluded() {
        ReflectionTestUtils.setField(credentialValidatorUnderTest,
                VALIDATOR_PRIVATE_FIELD_NAME, Set.of(PLATFORM_TWO));
        Assertions.assertThrows(BadRequestException.class, runValidation());
    }

    @Test
    public void testValidateCredentialCloudPlatformIncluded() {
        ReflectionTestUtils.setField(credentialValidatorUnderTest,
                VALIDATOR_PRIVATE_FIELD_NAME, Set.of(PLATFORM_TWO, PLATFORM_ONE));
        Assertions.assertDoesNotThrow(runValidation());
    }

    private Executable runValidation() {
        return () -> credentialValidatorUnderTest
                .validateCredentialCloudPlatform(PLATFORM_ONE);
    }
}
