package com.sequenceiq.cloudbreak.rotation.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;

@ExtendWith(MockitoExtension.class)
class SecretTypeConverterTest {

    @Test
    void mapSecretTypesShouldFailWhenInvalidEnumValue() {
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> SecretTypeConverter.mapSecretTypes(List.of("TEST", "INVALID")));
        assertEquals("Invalid secret type, cannot map secrets [TEST, INVALID].",
                cloudbreakServiceException.getMessage());
    }

    @Test
    void mapSecretTypesShouldSucceed() {
        List<SecretType> secretTypes =
                SecretTypeConverter.mapSecretTypes(List.of("TEST", "TEST_2"));
        assertThat(secretTypes).containsExactly(TestSecretType.TEST, TestSecretType.TEST_2);
    }
}