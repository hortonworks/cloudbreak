package com.sequenceiq.cloudbreak.cloud.aws.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AwsNativeSecretEncryptionValidatorTest {

    private static final String ACCOUNT_ID = "test-account-id";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AwsNativeSecretEncryptionValidator underTest;

    static Stream<Arguments> testValidateArguments() {
        // secretEncryptionEnabled, commercialEntitlement, expectException
        return Stream.of(
                // Secret encryption disabled — no check needed
                Arguments.of(false, false, false),
                Arguments.of(false, true, false),
                // Secret encryption enabled + entitled — passes
                Arguments.of(true, true, false),
                // Secret encryption enabled + NOT entitled — throws
                Arguments.of(true, false, true)
        );
    }

    @MethodSource("testValidateArguments")
    @ParameterizedTest
    void testValidate(boolean secretEncryptionEnabled, boolean secretEncryptionCommercialEntitlement, boolean expectException) {
        CloudCredential cloudCredential = new CloudCredential("id", "name", ACCOUNT_ID);
        AuthenticatedContext ac = mock();
        when(ac.getCloudCredential()).thenReturn(cloudCredential);
        CloudStack cloudStack = CloudStack.builder()
                .parameters(Map.of(PlatformParametersConsts.SECRET_ENCRYPTION_ENABLED, Boolean.toString(secretEncryptionEnabled)))
                .build();

        when(entitlementService.isSecretEncryptionForCommercialAwsEnabled(ACCOUNT_ID)).thenReturn(secretEncryptionCommercialEntitlement);

        if (expectException) {
            assertThrows(CloudConnectorException.class, () -> underTest.validate(ac, cloudStack));
        } else {
            assertDoesNotThrow(() -> underTest.validate(ac, cloudStack));
        }
    }

    @Test
    void testValidateDoesNotCheckEntitlementWhenSecretEncryptionDisabled() {
        CloudCredential cloudCredential = new CloudCredential("id", "name", ACCOUNT_ID);
        AuthenticatedContext ac = mock();
        when(ac.getCloudCredential()).thenReturn(cloudCredential);
        CloudStack cloudStack = CloudStack.builder()
                .parameters(Map.of(PlatformParametersConsts.SECRET_ENCRYPTION_ENABLED, "false"))
                .build();

        underTest.validate(ac, cloudStack);

        verifyNoInteractions(entitlementService);
    }
}
