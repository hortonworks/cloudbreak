package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.constant.GcpConstants.RAZ_AUTHENTICATION_TYPE_CAB;
import static com.sequenceiq.cloudbreak.constant.GcpConstants.RAZ_AUTHENTICATION_TYPE_HMAC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

class CMRepositoryVersionUtilTest {

    @ParameterizedTest(name = "{0} {1} with CM {2} should be supported")
    @MethodSource("supportedRazConfigurations")
    @DisplayName("isRazConfigurationSupported returns true for supported configurations")
    void isRazConfigurationSupportedReturnsTrue(CloudPlatform cloudPlatform, StackType stackType, String cmVersion) {
        assertTrue(CMRepositoryVersionUtil.isRazConfigurationSupported(RAZ_AUTHENTICATION_TYPE_CAB, cmVersion, cloudPlatform, stackType));
    }

    @ParameterizedTest(name = "{0} {1} with CM {2} should NOT be supported")
    @MethodSource("unsupportedRazConfigurations")
    @DisplayName("isRazConfigurationSupported returns false for unsupported configurations")
    void isRazConfigurationSupportedReturnsFalse(CloudPlatform cloudPlatform, StackType stackType, String cmVersion) {
        assertFalse(CMRepositoryVersionUtil.isRazConfigurationSupported(RAZ_AUTHENTICATION_TYPE_CAB, cmVersion, cloudPlatform, stackType));
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
                Arguments.of(CloudPlatform.GCP, StackType.DATALAKE, "7.13.2.30000"),

                // AWS DataHub (WORKLOAD) - minimum 7.2.2
                Arguments.of(CloudPlatform.AWS, StackType.WORKLOAD, "7.2.2"),
                Arguments.of(CloudPlatform.AWS, StackType.WORKLOAD, "7.13.2.20000"),

                // Azure DataHub (WORKLOAD) - minimum 7.2.2
                Arguments.of(CloudPlatform.AZURE, StackType.WORKLOAD, "7.2.2"),
                Arguments.of(CloudPlatform.AZURE, StackType.WORKLOAD, "7.11.0"),

                // GCP DataHub (WORKLOAD) - minimum 7.13.2.20000
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, "7.13.2.30000"),
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
    @DisplayName("GCP DataHub requires higher CM version than GCP Data Lake")
    void gcpDatahubRequiresHigherVersionThanDatalake() {
        String gcpDatalakeMinVersion = "7.11.0";
        // GCP Datalake is supported at 7.11.0
        assertTrue(CMRepositoryVersionUtil.isRazConfigurationSupported(RAZ_AUTHENTICATION_TYPE_CAB, gcpDatalakeMinVersion, CloudPlatform.GCP,
                StackType.DATALAKE));
        // GCP DataHub is NOT supported at 7.11.0
        assertFalse(CMRepositoryVersionUtil.isRazConfigurationSupported(RAZ_AUTHENTICATION_TYPE_CAB, gcpDatalakeMinVersion, CloudPlatform.GCP,
                StackType.WORKLOAD));
        // GCP DataHub IS supported at 7.13.2.20000
        assertTrue(CMRepositoryVersionUtil.isRazConfigurationSupported(RAZ_AUTHENTICATION_TYPE_CAB, "7.13.2.30000", CloudPlatform.GCP, StackType.WORKLOAD));
    }

    @ParameterizedTest(name = "{0} {1} with Data Lake Raz authentication type {2} should be supported: {3}")
    @MethodSource("razConfigurationDatalakeAuthenticationTypes")
    @DisplayName("isRazConfigurationSupported checks the Data Lake Raz authentication type required for Raz in Data Hub")
    void isRazConfigurationSupportedByDatalakeAuthenticationType(CloudPlatform cloudPlatform, StackType stackType, String datalakeRazAuthenticationType,
            boolean expected) {
        assertEquals(expected, CMRepositoryVersionUtil.isRazConfigurationSupported(datalakeRazAuthenticationType, "7.13.2.30000", cloudPlatform, stackType));
    }

    static Stream<Arguments> razConfigurationDatalakeAuthenticationTypes() {
        return Stream.of(
                // AWS and Azure DataHub do not depend on the Data Lake Raz authentication type
                Arguments.of(CloudPlatform.AWS, StackType.WORKLOAD, null, true),
                Arguments.of(CloudPlatform.AWS, StackType.WORKLOAD, RAZ_AUTHENTICATION_TYPE_HMAC, true),
                Arguments.of(CloudPlatform.AZURE, StackType.WORKLOAD, null, true),

                // GCP DataHub requires a Data Lake with CAB Raz authentication type
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, RAZ_AUTHENTICATION_TYPE_CAB, true),
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, RAZ_AUTHENTICATION_TYPE_HMAC, false),
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, null, false),

                // Datalake does not depend on its own stored Raz authentication type
                Arguments.of(CloudPlatform.AWS, StackType.DATALAKE, null, true),
                Arguments.of(CloudPlatform.AZURE, StackType.DATALAKE, null, true),
                Arguments.of(CloudPlatform.GCP, StackType.DATALAKE, RAZ_AUTHENTICATION_TYPE_HMAC, true),
                Arguments.of(CloudPlatform.GCP, StackType.DATALAKE, null, true),

                // Unsupported cloud platform is not enabled by the Data Lake Raz authentication type
                Arguments.of(CloudPlatform.YARN, StackType.WORKLOAD, RAZ_AUTHENTICATION_TYPE_CAB, false)
        );
    }

    @ParameterizedTest(name = "{0} {1} with Data Lake Raz authentication type {2} should be supported: {3}")
    @MethodSource("razConfigurationDatalakeAuthenticationTypes")
    @DisplayName("isRazConfigurationSupported reads the Data Lake view, CM version, cloud platform and stack type from the template preparation object")
    void isRazConfigurationSupportedByTemplatePreparationObject(CloudPlatform cloudPlatform, StackType stackType, String datalakeRazAuthenticationType,
            boolean expected) {
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .withDataLakeView(new DatalakeView(true, datalakeRazAuthenticationType, null, "datalakeCrn", false))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.13.2.30000"), List.of())
                .withCloudPlatform(cloudPlatform)
                .withStackType(stackType)
                .build();

        assertEquals(expected, CMRepositoryVersionUtil.isRazConfigurationSupported(source));
    }

    @Test
    @DisplayName("isRazConfigurationSupported with template preparation object treats a missing Data Lake view as not CAB for GCP Data Hub")
    void isRazConfigurationSupportedByTemplatePreparationObjectWhenDatalakeViewIsMissing() {
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.13.2.30000"), List.of())
                .withCloudPlatform(CloudPlatform.GCP)
                .withStackType(StackType.WORKLOAD)
                .build();

        assertFalse(CMRepositoryVersionUtil.isRazConfigurationSupported(source));
    }

    @Test
    @DisplayName("isRazConfigurationSupported with template preparation object checks the CM version")
    void isRazConfigurationSupportedByTemplatePreparationObjectWhenCmVersionIsTooOld() {
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .withDataLakeView(new DatalakeView(true, RAZ_AUTHENTICATION_TYPE_CAB, null, "datalakeCrn", false))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.11.0"), List.of())
                .withCloudPlatform(CloudPlatform.GCP)
                .withStackType(StackType.WORKLOAD)
                .build();

        assertFalse(CMRepositoryVersionUtil.isRazConfigurationSupported(source));
    }

    @ParameterizedTest(name = "{0} {1} with Data Lake Raz authentication type {2} needs Raz role configuration: {3}")
    @MethodSource("razRoleConfigurationDatalakeAuthenticationTypes")
    @DisplayName("isRazConfigurationForRazRoleNeeded is only true for GCP and checks the Data Lake Raz authentication type for Data Hub")
    void isRazConfigurationForRazRoleNeeded(CloudPlatform cloudPlatform, StackType stackType, String datalakeRazAuthenticationType, boolean expected) {
        assertEquals(expected,
                CMRepositoryVersionUtil.isRazConfigurationForRazRoleNeeded(datalakeRazAuthenticationType, "7.13.2.30000", cloudPlatform, stackType));
    }

    static Stream<Arguments> razRoleConfigurationDatalakeAuthenticationTypes() {
        return Stream.of(
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, RAZ_AUTHENTICATION_TYPE_CAB, true),
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, RAZ_AUTHENTICATION_TYPE_HMAC, false),
                Arguments.of(CloudPlatform.GCP, StackType.WORKLOAD, null, false),
                Arguments.of(CloudPlatform.GCP, StackType.DATALAKE, null, true),
                Arguments.of(CloudPlatform.AWS, StackType.WORKLOAD, RAZ_AUTHENTICATION_TYPE_CAB, false),
                Arguments.of(CloudPlatform.AZURE, StackType.DATALAKE, null, false)
        );
    }

    @ParameterizedTest(name = "CM {0} supports hosts start roles: {1}")
    @CsvSource({"7.6.0, false", "7.8.1, false", "7.9.0, true", "7.13.1.706, true"})
    void isHostsStartRolesSupported(String cmVersion, boolean expected) {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion(cmVersion);

        assertEquals(expected, CMRepositoryVersionUtil.isHostsStartRolesSupported(clouderaManagerRepo));
    }
}
