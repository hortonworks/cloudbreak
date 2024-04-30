package com.sequenceiq.cloudbreak.service.secret.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.service.secret.SecretOperationException;

class VaultSecretInputValidatorTest {

    private VaultSecretInputValidator underTest = new VaultSecretInputValidator();

    @BeforeEach
    public void setup() {
        //set maxSecretPathLength
        Field maxSecretPathLengthField = ReflectionUtils.findField(VaultSecretInputValidator.class, "maxSecretPathLength");
        ReflectionUtils.makeAccessible(maxSecretPathLengthField);
        ReflectionUtils.setField(maxSecretPathLengthField, underTest, 18);
    }

    @Test
    void testValidateShortPath() {
        underTest.validate("enginePath", "s");
    }

    @Test
    void testValidateWithEmptyEnginePath() {
        SecretOperationException exc = assertThrows(SecretOperationException.class, () -> underTest.validate("", "fullPath"));
        assertEquals("EnginePath and and secretPath cannot be null or enginePath:[], fullPath [fullPath]", exc.getMessage());
    }

    @Test
    void testValidateWithEmptyFullPath() {
        SecretOperationException exc = assertThrows(SecretOperationException.class, () -> underTest.validate("enginePath", ""));
        assertEquals("EnginePath and and secretPath cannot be null or enginePath:[enginePath], fullPath []", exc.getMessage());
    }

    @Test
    void testValidateWithEmptyEnginePathAndFullPath() {
        SecretOperationException exc = assertThrows(SecretOperationException.class, () -> underTest.validate("", ""));
        assertEquals("EnginePath and and secretPath cannot be null or enginePath:[], fullPath []", exc.getMessage());
    }

    @Test
    void testValidateWithLongSecretPath() {
        SecretOperationException exc =  assertThrows(SecretOperationException.class, () -> underTest.validate("enginePath", "fullPath"));
        assertEquals("Secret path size [19] is greater than [18]", exc.getMessage());
    }
}