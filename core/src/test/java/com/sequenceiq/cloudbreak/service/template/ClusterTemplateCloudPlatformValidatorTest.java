package com.sequenceiq.cloudbreak.service.template;

import static com.sequenceiq.cloudbreak.service.template.ClusterTemplateCloudPlatformValidator.IAM_INTERNAL_ACTOR_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class ClusterTemplateCloudPlatformValidatorTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String AWS = "AWS";

    private static final String AZURE = "AZURE";

    private static final String FOO = "FOO";

    private static final String GCP = "GCP";

    private static final String AZURE_DISABLED = " & Azure disabled";

    private static final String AZURE_ENABLED = " & Azure enabled";

    private static final Set<String> ENABLED_PLATFORMS_AWS_AZURE = Set.of(AWS, AZURE);

    private static final String ENABLED_PLATFORMS_AWS_AZURE_S = "Enabled platforms {AWS, AZURE} & ";

    private static final Set<String> ENABLED_PLATFORMS_AZURE_GCP = Set.of(AZURE, GCP);

    private static final String ENABLED_PLATFORMS_AZURE_GCP_S = "Enabled platforms {AZURE, GCP} & ";

    private static final Set<String> ENABLED_PLATFORMS_AWS_GCP = Set.of(AWS, GCP);

    private static final String ENABLED_PLATFORMS_AWS_GCP_S = "Enabled platforms {AWS, GCP} & ";

    private static final String ENABLED_PLATFORMS_IS_EMPTY = "Enabled platforms is empty";

    @Mock
    private EntitlementService entitlementService;

    private ClusterTemplateCloudPlatformValidator underTest;

    private void setUpUnderTest(Set<String> enabledPlatforms) {
        underTest = new ClusterTemplateCloudPlatformValidator(enabledPlatforms, entitlementService);
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] validateClusterTemplateCloudPlatformDataProvider() {
        return new Object[][] {
                // testCaseName                                             enabledPlatforms                cloudPlatform   azureEnabled    validExpected
                { ENABLED_PLATFORMS_AWS_AZURE_S + AWS + AZURE_DISABLED,     ENABLED_PLATFORMS_AWS_AZURE,    AWS,            false,          true },
                { ENABLED_PLATFORMS_AWS_AZURE_S + AZURE + AZURE_DISABLED,   ENABLED_PLATFORMS_AWS_AZURE,    AZURE,          false,          false },
                { ENABLED_PLATFORMS_AWS_AZURE_S + GCP + AZURE_DISABLED,     ENABLED_PLATFORMS_AWS_AZURE,    GCP,            false,          false },
                { ENABLED_PLATFORMS_AWS_AZURE_S + FOO + AZURE_DISABLED,     ENABLED_PLATFORMS_AWS_AZURE,    FOO,            false,          false },
                { ENABLED_PLATFORMS_AWS_AZURE_S + AWS + AZURE_ENABLED,      ENABLED_PLATFORMS_AWS_AZURE,    AWS,            true,           true },
                { ENABLED_PLATFORMS_AWS_AZURE_S + AZURE + AZURE_ENABLED,    ENABLED_PLATFORMS_AWS_AZURE,    AZURE,          true,           true },
                { ENABLED_PLATFORMS_AWS_AZURE_S + GCP + AZURE_ENABLED,      ENABLED_PLATFORMS_AWS_AZURE,    GCP,            true,           false },
                { ENABLED_PLATFORMS_AWS_AZURE_S + FOO + AZURE_ENABLED,      ENABLED_PLATFORMS_AWS_AZURE,    FOO,            true,           false },

                { ENABLED_PLATFORMS_AZURE_GCP_S + AWS + AZURE_DISABLED,     ENABLED_PLATFORMS_AZURE_GCP,    AWS,            false,          false },
                { ENABLED_PLATFORMS_AZURE_GCP_S + AZURE + AZURE_DISABLED,   ENABLED_PLATFORMS_AZURE_GCP,    AZURE,          false,          false },
                { ENABLED_PLATFORMS_AZURE_GCP_S + GCP + AZURE_DISABLED,     ENABLED_PLATFORMS_AZURE_GCP,    GCP,            false,          true },
                { ENABLED_PLATFORMS_AZURE_GCP_S + FOO + AZURE_DISABLED,     ENABLED_PLATFORMS_AZURE_GCP,    FOO,            false,          false },
                { ENABLED_PLATFORMS_AZURE_GCP_S + AWS + AZURE_ENABLED,      ENABLED_PLATFORMS_AZURE_GCP,    AWS,            true,           false },
                { ENABLED_PLATFORMS_AZURE_GCP_S + AZURE + AZURE_ENABLED,    ENABLED_PLATFORMS_AZURE_GCP,    AZURE,          true,           true },
                { ENABLED_PLATFORMS_AZURE_GCP_S + GCP + AZURE_ENABLED,      ENABLED_PLATFORMS_AZURE_GCP,    GCP,            true,           true },
                { ENABLED_PLATFORMS_AZURE_GCP_S + FOO + AZURE_ENABLED,      ENABLED_PLATFORMS_AZURE_GCP,    FOO,            true,           false },

                { ENABLED_PLATFORMS_AWS_GCP_S + AZURE + AZURE_DISABLED,     ENABLED_PLATFORMS_AWS_GCP,      AZURE,          false,          false },
                { ENABLED_PLATFORMS_AWS_GCP_S + AZURE + AZURE_ENABLED,      ENABLED_PLATFORMS_AWS_GCP,      AZURE,          true,           false },
                { ENABLED_PLATFORMS_IS_EMPTY,                               Set.of(),                       AZURE,          true,           true },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("validateClusterTemplateCloudPlatformDataProvider")
    void testIsClusterTemplateCloudPlatformValid(String testCaseName, Set<String> enabledPlatforms, String cloudPlatform, boolean azureEnabled,
            boolean validExpected) {
        setUpUnderTest(enabledPlatforms);
        if (AZURE.equalsIgnoreCase(cloudPlatform)) {
            lenient().when(entitlementService.azureEnabled(IAM_INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(azureEnabled);
        }
        assertThat(underTest.isClusterTemplateCloudPlatformValid(cloudPlatform, ACCOUNT_ID)).isEqualTo(validExpected);
    }

}