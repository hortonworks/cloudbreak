package com.sequenceiq.cloudbreak.cloud;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@ExtendWith(MockitoExtension.class)
class CommonSecretEncryptionValidatorTest {

    private final CommonSecretEncryptionValidator underTest = new CommonSecretEncryptionValidator();

    static Stream<Arguments> testValidateArguments() {
        return Stream.of(
                Arguments.of(Map.of(), false),
                Arguments.of(Map.of(PlatformParametersConsts.SECRET_ENCRYPTION_ENABLED, "false"), false),
                Arguments.of(Map.of(PlatformParametersConsts.SECRET_ENCRYPTION_ENABLED, "true"), true)
        );
    }

    @MethodSource("testValidateArguments")
    @ParameterizedTest
    void testValidate(Map<String, String> parameters, boolean expectException) {
        CloudStack cloudStack = CloudStack.builder()
                .parameters(parameters)
                .build();

        if (expectException) {
            assertThrows(CloudConnectorException.class, () -> underTest.validate(null, cloudStack));
        } else {
            assertDoesNotThrow(() -> underTest.validate(null, cloudStack));
        }
    }
}
