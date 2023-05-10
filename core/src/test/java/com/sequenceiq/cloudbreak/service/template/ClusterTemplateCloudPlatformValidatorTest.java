package com.sequenceiq.cloudbreak.service.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterTemplateCloudPlatformValidatorTest {

    private static final String AWS = "AWS";

    private static final String AZURE = "AZURE";

    private static final String FOO = "FOO";

    private static final String GCP = "GCP";

    private static final Set<String> ENABLED_PLATFORMS_AWS_AZURE = Set.of(AWS, AZURE);

    private static final Set<String> ENABLED_PLATFORMS_AZURE_GCP = Set.of(AZURE, GCP);

    private static final Set<String> ENABLED_PLATFORMS_AWS_GCP = Set.of(AWS, GCP);

    private ClusterTemplateCloudPlatformValidator underTest;

    private void setUpUnderTest(Set<String> enabledPlatforms) {
        underTest = new ClusterTemplateCloudPlatformValidator(enabledPlatforms);
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] validateClusterTemplateCloudPlatformDataProvider() {
        return new Object[][] {
                //  enabledPlatforms              cloudPlatform     validExpected
                { ENABLED_PLATFORMS_AWS_AZURE,    AWS,              true },
                { ENABLED_PLATFORMS_AWS_AZURE,    AZURE,            true },
                { ENABLED_PLATFORMS_AWS_AZURE,    GCP,              false },
                { ENABLED_PLATFORMS_AWS_AZURE,    FOO,              false },
                { ENABLED_PLATFORMS_AWS_AZURE,    AWS,              true },
                { ENABLED_PLATFORMS_AWS_AZURE,    AZURE,            true },
                { ENABLED_PLATFORMS_AWS_AZURE,    GCP,              false },
                { ENABLED_PLATFORMS_AWS_AZURE,    FOO,              false },

                { ENABLED_PLATFORMS_AZURE_GCP,    AWS,              false },
                { ENABLED_PLATFORMS_AZURE_GCP,    AZURE,            true },
                { ENABLED_PLATFORMS_AZURE_GCP,    GCP,              true },
                { ENABLED_PLATFORMS_AZURE_GCP,    FOO,              false },
                { ENABLED_PLATFORMS_AZURE_GCP,    AWS,              false },
                { ENABLED_PLATFORMS_AZURE_GCP,    AZURE,            true },
                { ENABLED_PLATFORMS_AZURE_GCP,    GCP,              true },
                { ENABLED_PLATFORMS_AZURE_GCP,    FOO,              false },

                { ENABLED_PLATFORMS_AWS_GCP,      AZURE,            false },
                { ENABLED_PLATFORMS_AWS_GCP,      GCP,              true },
                { Set.of(),                       AZURE,            true },
                { Set.of(),                       GCP,              true },

        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "#{index} :\n"
            + "When enabled platforms are {0} and customer wants to start {1} resource => \n"
            + "Then CDP can start resources: {3}")
    @MethodSource("validateClusterTemplateCloudPlatformDataProvider")
    void testIsClusterTemplateCloudPlatformValid(Set<String> enabledPlatforms, String cloudPlatform, boolean validExpected) {
        setUpUnderTest(enabledPlatforms);
        assertThat(underTest.isClusterTemplateCloudPlatformValid(cloudPlatform)).isEqualTo(validExpected);
    }

}