package com.sequenceiq.cloudbreak.cloud.aws.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@ExtendWith(MockitoExtension.class)
class AwsNativeSecretEncryptionValidatorTest {

    private static final String ACCOUNT_ID = "test-account-id";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AwsNativeSecretEncryptionValidator underTest;

    static Stream<Arguments> testValidateArguments() {
        return Stream.of(
                Arguments.of(false, false, false, false),
                Arguments.of(false, true, false, false),
                Arguments.of(false, false, true, false),
                Arguments.of(false, true, true, false),
                Arguments.of(true, false, false, true),
                Arguments.of(true, true, false, true),
                Arguments.of(true, false, true, true),
                Arguments.of(true, true, true, false)
        );
    }

    @MethodSource("testValidateArguments")
    @ParameterizedTest
    void testValidate(boolean secretEncryptionEnabled, boolean secretEncryptionEntitlement, boolean secretEncryptionCommercialEntitlement,
            boolean expectException) {
        CloudCredential cloudCredential = mock();
        lenient().when(cloudCredential.getAccountId()).thenReturn(ACCOUNT_ID);
        AuthenticatedContext ac = mock();
        lenient().when(ac.getCloudCredential()).thenReturn(cloudCredential);
        CloudStack cloudStack = CloudStack.builder()
                .parameters(Map.of(PlatformParametersConsts.SECRET_ENCRYPTION_ENABLED, Boolean.toString(secretEncryptionEnabled)))
                .build();

        lenient().when(entitlementService.isSecretEncryptionEnabled(ACCOUNT_ID)).thenReturn(secretEncryptionEntitlement);
        lenient().when(entitlementService.isSecretEncryptionForCommercialAwsEnabled(ACCOUNT_ID)).thenReturn(secretEncryptionCommercialEntitlement);

        if (expectException) {
            assertThrows(CloudConnectorException.class, () -> underTest.validate(ac, cloudStack));
        } else {
            assertDoesNotThrow(() -> underTest.validate(ac, cloudStack));
        }
    }
}
