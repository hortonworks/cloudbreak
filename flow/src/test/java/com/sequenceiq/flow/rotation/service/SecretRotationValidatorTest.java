package com.sequenceiq.flow.rotation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.flow.rotation.TestSecretType;

@ExtendWith(MockitoExtension.class)
class SecretRotationValidatorTest {

    @InjectMocks
    private SecretRotationValidator underTest;

    @Test
    void mapSecretTypesShouldFailWhenDuplicatedSecretTypes() {
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.mapSecretTypes(List.of("TEST", "TEST"), TestSecretType.class));
        assertEquals("There is at least one duplication in the request!", cloudbreakServiceException.getMessage());
    }

    @Test
    void mapSecretTypesShouldFailWhenInvalidEnumValue() {
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.mapSecretTypes(List.of("TEST", "INVALID"), TestSecretType.class));
        assertEquals("Invalid secret type, cannot map secrets [TEST, INVALID] to TestSecretType",
                cloudbreakServiceException.getMessage());
    }

    @Test
    void mapSecretTypesShouldSucceed() {
        List<SecretType> secretTypes =
                underTest.mapSecretTypes(List.of("TEST", "TEST2"), TestSecretType.class);
        assertThat(secretTypes).containsExactly(TestSecretType.TEST, TestSecretType.TEST2);
    }
}