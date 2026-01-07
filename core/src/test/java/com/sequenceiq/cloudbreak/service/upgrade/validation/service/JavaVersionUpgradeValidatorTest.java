package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

@ExtendWith(MockitoExtension.class)
class JavaVersionUpgradeValidatorTest {

    @InjectMocks
    private JavaVersionUpgradeValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackDto stackDto;

    @ParameterizedTest(name = "[{index}] Target runtime version: {0} Current Java version: {1} Should permit upgrade: {2} Auto java upgrade enabled: {4}")
    @MethodSource("provideTestParameters")
    public void testValidate(String targetRuntimeVersion, Integer currentJavaVersion, boolean autoJavaUpgradeEnabled,
            boolean shouldPermitUpgrade, String containedMessage) {
        UpgradeImageInfo upgradeImageInfo = createUpgradeImageInfo(targetRuntimeVersion);
        Stack stack = createStack(currentJavaVersion);
        when(stackDto.getStack()).thenReturn(stack);
        lenient().when(stackDto.getAccountId()).thenReturn("12345678");
        lenient().when(entitlementService.isAutoJavaUpgaradeEnabled("12345678")).thenReturn(autoJavaUpgradeEnabled);
        ServiceUpgradeValidationRequest request = new ServiceUpgradeValidationRequest(stackDto, true, true, upgradeImageInfo, true);

        if (shouldPermitUpgrade) {
            assertDoesNotThrow(() -> underTest.validate(request));
        } else {
            UpgradeValidationFailedException exception = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));
            assertThat(exception.getMessage()).contains(containedMessage);
        }
    }

    private static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of("7.3.0", 8, false, true, null),
                Arguments.of("7.3.0", 8, true, true, null),
                Arguments.of("7.3.0", 11, false, true, null),
                Arguments.of("7.3.0", 11, true, true, null),
                Arguments.of("7.3.0", 17, false, true, null),
                Arguments.of("7.3.0", 17, true, true, null),
                Arguments.of("7.3.1", 8, false, true, null),
                Arguments.of("7.3.1", 8, true, true, null),
                Arguments.of("7.3.1", 11, false, false, "You cannot upgrade to 7.3.1 because your current cluster uses JDK 11"),
                Arguments.of("7.3.1", 17, false, true, null),
                Arguments.of("7.3.1", 17, true, true, null),
                Arguments.of("7.3.2", 8, false, false, "You cannot upgrade to 7.3.2 because your current cluster uses JDK 8"),
                Arguments.of("7.3.2", 8, true, true, null),
                Arguments.of("7.3.2", 11, false, false, "You cannot upgrade to 7.3.2 because your current cluster uses JDK 11"),
                Arguments.of("7.3.2", 11, true, true, null),
                Arguments.of("7.3.2", 17, false, true, null),
                Arguments.of("7.3.2", 17, true, true, null)
        );
    }

    private Stack createStack(Integer javaVersion) {
        Stack stack = new Stack();
        stack.setJavaVersion(javaVersion);
        return stack;
    }

    private UpgradeImageInfo createUpgradeImageInfo(String version) {
        return new UpgradeImageInfo(null, StatedImage.statedImage(Image.builder().withVersion(version).build(), null, null));
    }

}