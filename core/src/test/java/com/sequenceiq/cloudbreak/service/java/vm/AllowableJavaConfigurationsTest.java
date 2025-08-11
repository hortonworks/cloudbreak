package com.sequenceiq.cloudbreak.service.java.vm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class AllowableJavaConfigurationsTest {

    private static final int JAVA_VERSION = 17;

    @InjectMocks
    private AllowableJavaConfigurations allowableJavaConfigurations;

    private static Stream<Arguments> runtimeVersionCombinations() {
        return Stream.of(
                Arguments.of("7.2.18", null, "7.3.1", true),
                Arguments.of("7.2.18", "7.3.1", "7.3.1", true),
                Arguments.of("7.2.18", "7.3.1.400", "7.3.1", false),
                Arguments.of("7.2.18", "7.3.1.500", "7.3.1", false),
                Arguments.of("7.2.18", "7.3.2", "7.3.1", true),
                // in production this is an invalid combination, from 7.3.1, all of the images should have the release-version tag (4 digit version number)
                Arguments.of("7.2.18", "7.3.2.500", "7.3.1", false),
                Arguments.of("7.2.18", "7.2.18", "7.3.1", false),
                Arguments.of("7.3.1", "7.3.1", "7.3.1", true),
                Arguments.of("7.3.1.500", "7.3.2", "7.3.1", false),
                Arguments.of("7.3.1.500", "7.3.1.500", "7.3.1", false),
                Arguments.of("7.3.2", "7.3.2", "7.3.1", false),
                Arguments.of("7.3.2.500", "7.3.3", "7.3.1", false),

                Arguments.of("7.2.18", null, "7.3.1.500", true),
                Arguments.of("7.2.18", "7.3.1", "7.3.1.500", true),
                Arguments.of("7.2.18", "7.3.1.400", "7.3.1.500", false),
                Arguments.of("7.2.18", "7.3.1.500", "7.3.1.500", true),
                Arguments.of("7.2.18", "7.3.2", "7.3.1.500", true),
                Arguments.of("7.2.18", "7.3.2.500", "7.3.1.500", true),
                Arguments.of("7.2.18", "7.2.18.500", "7.3.1.500", false),
                Arguments.of("7.3.1", "7.3.1", "7.3.1.500", true),
                Arguments.of("7.3.1.500", "7.3.1", "7.3.1.500", true),
                Arguments.of("7.3.1.500", "7.3.2", "7.3.1.500", true),
                Arguments.of("7.3.1.500", "7.3.1.500", "7.3.1.500", true),
                Arguments.of("7.3.2", "7.3.2", "7.3.1.500", false),
                Arguments.of("7.3.2.500", "7.3.2", "7.3.1.500", false)
        );
    }

    @ParameterizedTest
    @MethodSource("runtimeVersionCombinations")
    void testCheckValidConfigurationCombinations(String minRuntimeVersion, String maxRuntimeVersion, String runtimeVersion, boolean valid) {
        JavaConfiguration java17Config = new JavaConfiguration();
        java17Config.setVersion(JAVA_VERSION);
        java17Config.setMinRuntimeVersion(minRuntimeVersion);
        java17Config.setMaxRuntimeVersion(maxRuntimeVersion);
        allowableJavaConfigurations.setJavaVersions(List.of(java17Config));

        if (valid) {
            assertDoesNotThrow(() -> allowableJavaConfigurations.checkValidConfiguration(JAVA_VERSION, runtimeVersion));
        } else {
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> allowableJavaConfigurations.checkValidConfiguration(JAVA_VERSION, runtimeVersion));
            assertEquals(String.format("The requested Java version %s is not compatible with the runtime version %s",
                    JAVA_VERSION, runtimeVersion), exception.getMessage());
        }
    }

    @Test
    void testCheckValidConfigurations() {
        JavaConfiguration java11Config = new JavaConfiguration();
        java11Config.setVersion(11);
        java11Config.setMinRuntimeVersion("7.2.1");
        JavaConfiguration java17Config = new JavaConfiguration();
        java17Config.setVersion(17);
        java17Config.setMinRuntimeVersion("7.3.1");
        allowableJavaConfigurations.setJavaVersions(List.of(java11Config, java17Config));

        allowableJavaConfigurations.checkValidConfiguration(11, "7.3.1");
        allowableJavaConfigurations.checkValidConfiguration(17, "7.3.1");
        allowableJavaConfigurations.checkValidConfiguration(17, "7.3.2");
        allowableJavaConfigurations.checkValidConfiguration(17, "7.5.2");
        allowableJavaConfigurations.checkValidConfiguration(11, "7.3.1.500");
        allowableJavaConfigurations.checkValidConfiguration(17, "7.3.1.500");
        allowableJavaConfigurations.checkValidConfiguration(17, "7.3.2.500");
        allowableJavaConfigurations.checkValidConfiguration(17, "7.5.2.500");
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> allowableJavaConfigurations.checkValidConfiguration(17, "7.2.18"));
        assertEquals("The requested Java version 17 is not compatible with the runtime version 7.2.18", badRequestException.getMessage());
        badRequestException = assertThrows(BadRequestException.class,
                () -> allowableJavaConfigurations.checkValidConfiguration(17, "7.1.0"));
        assertEquals("The requested Java version 17 is not compatible with the runtime version 7.1.0", badRequestException.getMessage());
        badRequestException = assertThrows(BadRequestException.class,
                () -> allowableJavaConfigurations.checkValidConfiguration(8, "7.3.1"));
        assertEquals("The requested Java version 8 is not compatible with the runtime version 7.3.1", badRequestException.getMessage());
    }
}