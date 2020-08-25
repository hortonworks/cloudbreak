package com.sequenceiq.cloudbreak.service.template;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
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
                //  enabledPlatforms              cloudPlatform   azureEnabled    gcpEnabled    validExpected
                { ENABLED_PLATFORMS_AWS_AZURE,    AWS,            false,          false,        true },
                { ENABLED_PLATFORMS_AWS_AZURE,    AZURE,          false,          false,        false },
                { ENABLED_PLATFORMS_AWS_AZURE,    GCP,            false,          false,        false },
                { ENABLED_PLATFORMS_AWS_AZURE,    FOO,            false,          false,        false },
                { ENABLED_PLATFORMS_AWS_AZURE,    AWS,            true,           false,        true },
                { ENABLED_PLATFORMS_AWS_AZURE,    AZURE,          true,           false,        true },
                { ENABLED_PLATFORMS_AWS_AZURE,    GCP,            true,           false,        false },
                { ENABLED_PLATFORMS_AWS_AZURE,    FOO,            true,           false,        false },

                { ENABLED_PLATFORMS_AZURE_GCP,    AWS,            false,          false,        false },
                { ENABLED_PLATFORMS_AZURE_GCP,    AZURE,          false,          false,        false },
                { ENABLED_PLATFORMS_AZURE_GCP,    GCP,            false,          false,        false },
                { ENABLED_PLATFORMS_AZURE_GCP,    FOO,            false,          false,        false },
                { ENABLED_PLATFORMS_AZURE_GCP,    AWS,            true,           false,        false },
                { ENABLED_PLATFORMS_AZURE_GCP,    AZURE,          true,           false,        true },
                { ENABLED_PLATFORMS_AZURE_GCP,    GCP,            true,           false,        false },
                { ENABLED_PLATFORMS_AZURE_GCP,    FOO,            true,           false,        false },

                { ENABLED_PLATFORMS_AWS_GCP,      AZURE,          false,          false,        false },
                { ENABLED_PLATFORMS_AWS_GCP,      AZURE,          true,           false,        false },
                { ENABLED_PLATFORMS_AWS_GCP,      GCP,            false,          false,        false },
                { ENABLED_PLATFORMS_AWS_GCP,      GCP,            false,          true,         true },
                { Set.of(),                       AZURE,          true,           false,        true },
                { Set.of(),                       GCP,            false,          true,         true },
                { Set.of(),                       GCP,            false,          false,        false },

        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "#{index} :\n"
            + "When enabled platforms are {0} and customer wants to start {1} resource => \n"
            + "Azure entitlement status: {2} \n"
            + "GCP entitlement status: {3} \n"
            + "Then CDP can start resources: {4}")
    @MethodSource("validateClusterTemplateCloudPlatformDataProvider")
    void testIsClusterTemplateCloudPlatformValid(Set<String> enabledPlatforms, String cloudPlatform, boolean azureEnabled,
        boolean gcpEnabled, boolean validExpected) {
        setUpUnderTest(enabledPlatforms);
        lenient().when(entitlementService.azureEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(azureEnabled);
        lenient().when(entitlementService.gcpEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(gcpEnabled);

        assertThat(underTest.isClusterTemplateCloudPlatformValid(cloudPlatform, ACCOUNT_ID)).isEqualTo(validExpected);
    }

}