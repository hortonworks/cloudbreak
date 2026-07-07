package com.sequenceiq.cloudbreak.cmtemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class CMRepositoryVersionUtilTest {

    @ParameterizedTest(name = "{0} {1} with CM {2} should be supported")
    @MethodSource("supportedRazConfigurations")
    @DisplayName("isRazConfigurationSupported returns true for supported configurations")
    void isRazConfigurationSupportedReturnsTrue(CloudPlatform cloudPlatform, StackType stackType, String cmVersion) {
        assertTrue(CMRepositoryVersionUtil.isRazConfigurationSupported(cmVersion, cloudPlatform, stackType));
    }

    @ParameterizedTest(name = "{0} {1} with CM {2} should NOT be supported")
    @MethodSource("unsupportedRazConfigurations")
    @DisplayName("isRazConfigurationSupported returns false for unsupported configurations")
    void isRazConfigurationSupportedReturnsFalse(CloudPlatform cloudPlatform, StackType stackType, String cmVersion) {
        assertFalse(CMRepositoryVersionUtil.isRazConfigurationSupported(cmVersion, cloudPlatform, stackType));
    }

    static Stream<Arguments> supportedRazConfigurations() {
        return Stream.of(
                // AWS Datalake - minimum 7.2.2
                Arguments.of(CloudPlatform.AWS, StackType.DATALAKE, "7.2.2"),
                Arguments.of(CloudPlatform.AWS, StackType.DATALAKE, "7.11.0"),

                // Azure Datalake - minimum 7.2.2
                Arguments.of(CloudPlatform.AZURE, StackType.DATALAKE, "7.2.2"),
                Arguments.of(CloudPlatform.AZURE, StackType.DATALAKE, "7.13.2.20000"),

                // GCP Datalake - minimum 7.11.0
                Arguments.of(CloudPlatform.GCP, StackType.DATALAKE, "7.11.0"),
                Arguments.of(CloudPlatform.GCP, StackType.DATALAKE, "7.13.2.20000"),

                // AWS DataHub (WORKLOAD) - minimum 7.2.2
                Arguments.of(CloudPlatform.AWS, StackType.WORKLOAD, "7.2.2"),
                Arguments.of(CloudPlatform.AWS, StackType.WORKLOAD, "7.13.2.20000"),

                // Azure DataHub (WORKLOAD) - minimum 7.2.2
                Arguments.of(CloudPlatform.AZURE, StackType.WORKLOAD, "7.2.2"),
                Arguments.of(CloudPlatform.AZURE, StackType.WORKLOAD, "7.11.0"),

                // GCP DataHub (WORKLOAD) - minimum 7.13.2.20000
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, "7.13.2.20000"),
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, "7.14.0")
        );
    }

    static Stream<Arguments> unsupportedRazConfigurations() {
        return Stream.of(
                // AWS Datalake - below minimum 7.2.2
                Arguments.of(CloudPlatform.AWS, StackType.DATALAKE, "7.2.1"),
                Arguments.of(CloudPlatform.AWS, StackType.DATALAKE, "7.1.0"),

                // Azure Datalake - below minimum 7.2.2
                Arguments.of(CloudPlatform.AZURE, StackType.DATALAKE, "7.2.1"),

                // GCP Datalake - below minimum 7.11.0
                Arguments.of(CloudPlatform.GCP, StackType.DATALAKE, "7.9.0"),
                Arguments.of(CloudPlatform.GCP, StackType.DATALAKE, "7.2.2"),

                // AWS DataHub - below minimum 7.2.2
                Arguments.of(CloudPlatform.AWS, StackType.WORKLOAD, "7.2.1"),

                // Azure DataHub - below minimum 7.2.2
                Arguments.of(CloudPlatform.AZURE, StackType.WORKLOAD, "7.2.1"),

                // GCP DataHub - below minimum 7.13.2.20000
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, "7.11.0"),
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, "7.13.2.10000"),
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, "7.9.0"),

                // Unsupported cloud platform
                Arguments.of(CloudPlatform.YARN, StackType.DATALAKE, "7.13.2.20000"),
                Arguments.of(CloudPlatform.YARN, StackType.WORKLOAD, "7.13.2.20000")
        );
    }

    @Test
    @DisplayName("GCP DataHub requires higher CM version than GCP Datalake")
    void gcpDatahubRequiresHigherVersionThanDatalake() {
        String gcpDatalakeMinVersion = "7.11.0";
        // GCP Datalake is supported at 7.11.0
        assertTrue(CMRepositoryVersionUtil.isRazConfigurationSupported(gcpDatalakeMinVersion, CloudPlatform.GCP, StackType.DATALAKE));
        // GCP DataHub is NOT supported at 7.11.0
        assertFalse(CMRepositoryVersionUtil.isRazConfigurationSupported(gcpDatalakeMinVersion, CloudPlatform.GCP, StackType.WORKLOAD));
        // GCP DataHub IS supported at 7.13.2.20000
        assertTrue(CMRepositoryVersionUtil.isRazConfigurationSupported("7.13.2.20000", CloudPlatform.GCP, StackType.WORKLOAD));
    }
}
