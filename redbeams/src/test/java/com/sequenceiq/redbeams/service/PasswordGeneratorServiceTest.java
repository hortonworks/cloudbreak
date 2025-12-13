package com.sequenceiq.redbeams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@ExtendWith(MockitoExtension.class)
class PasswordGeneratorServiceTest {

    @Mock
    private UuidGeneratorService uuidGeneratorService;

    @InjectMocks
    private PasswordGeneratorService underTest;

    @Test
    void testAwsPassword() {
        when(uuidGeneratorService.uuidVariableParts(PasswordGeneratorService.AWS_MAX_LENGTH)).thenReturn("uuid");

        assertEquals("uuid", underTest.generatePassword(Optional.of(CloudPlatform.AWS)));

        verify(uuidGeneratorService).uuidVariableParts(PasswordGeneratorService.AWS_MAX_LENGTH);
    }

    @Test
    void testAzurePassword() {
        when(uuidGeneratorService.randomUuid()).thenReturn("3EF3A2E5-EFB2-472A-A33E-C31D989D49F6");

        String password = underTest.generatePassword(Optional.of(CloudPlatform.AZURE));

        assertTrue(password.length() <= PasswordGeneratorService.AZURE_MAX_LENGTH);
        assertTrue(PasswordGeneratorService.passesAzureCharacterRules(password));
    }

    @Test
    void testAzurePasswordMultiplePasses() {
        when(uuidGeneratorService.randomUuid())
            .thenReturn("11111111-1111-1111-1111-111111111111")
            .thenReturn("3EF3A2E5-EFB2-472A-A33E-C31D989D49F6");

        String password = underTest.generatePassword(Optional.of(CloudPlatform.AZURE));

        assertTrue(password.length() <= PasswordGeneratorService.AZURE_MAX_LENGTH);
        assertTrue(PasswordGeneratorService.passesAzureCharacterRules(password));
        assertTrue(password.startsWith("3"));
    }

    @Test
    void testPassesAzureCharacterRules() {
        // 2/2 (letters and digits)
        assertTrue(PasswordGeneratorService.passesAzureCharacterRules("Aa1"));
        assertTrue(PasswordGeneratorService.passesAzureCharacterRules("aa1"));
        assertTrue(PasswordGeneratorService.passesAzureCharacterRules("AA1"));

        // 1/2 (letters xor digits)
        assertFalse(PasswordGeneratorService.passesAzureCharacterRules("AaA"));
        assertFalse(PasswordGeneratorService.passesAzureCharacterRules("111"));
        assertFalse(PasswordGeneratorService.passesAzureCharacterRules("AAA"));
        assertFalse(PasswordGeneratorService.passesAzureCharacterRules("aaa"));

        // 0/2 (no letters or digits)
        assertFalse(PasswordGeneratorService.passesAzureCharacterRules("---"));
    }

    @Test
    void testGeneralPassword() {
        when(uuidGeneratorService.randomUuid()).thenReturn("uuid");

        assertEquals("uuid", underTest.generatePassword(Optional.empty()));

        verify(uuidGeneratorService).randomUuid();
    }

}
