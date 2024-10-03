package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private StackDto stackDto;

    @ParameterizedTest(name = "[{index}] Target runtime version: {0} Current Java version: {1} Should permit upgrade: {2}")
    @MethodSource("provideTestParameters")
    public void testValidate(String targetRuntimeVersion, Integer currentJavaVersion, boolean shouldPermitUpgrade) {
        UpgradeImageInfo upgradeImageInfo = createUpgradeImageInfo(targetRuntimeVersion);
        Stack stack = createStack(currentJavaVersion);
        when(stackDto.getStack()).thenReturn(stack);
        ServiceUpgradeValidationRequest request = new ServiceUpgradeValidationRequest(stackDto, true, true, upgradeImageInfo, true);

        if (shouldPermitUpgrade) {
            assertDoesNotThrow(() -> underTest.validate(request));
        } else {
            assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));
        }
    }

    private static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of("7.3.0", 8, true),
                Arguments.of("7.3.0", 11, true),
                Arguments.of("7.3.0", 17, true),
                Arguments.of("7.3.1", 8, true),
                Arguments.of("7.3.1", 11, false),
                Arguments.of("7.3.1", 17, true),
                Arguments.of("7.3.2", 8, true),
                Arguments.of("7.3.2", 11, false),
                Arguments.of("7.3.2", 17, true)
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