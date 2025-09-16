package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_12_0_500;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@ExtendWith(MockitoExtension.class)
class CommonCoreConfigProviderTest {

    private static final String TEST_RESOURCE_CRN = "test-resource_crn";

    private static final String TEST_ENVIRONMENT_CRN = "test-environment_crn";

    private static final String TEST_ACCOUNT_ID = "test-account_id";

    private static final String TEST_CLOUD_PROVIDER = "AWS";

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    @InjectMocks
    private CommonCoreConfigProvider underTest;

    @ParameterizedTest
    @ValueSource(strings = {"7.12.0.500", "7.12.0.500-58279810", "7.12.0.600"})
    void testGetServiceConfigsShouldContainClusterSpecificProperties(String cmGbn) {
        when(source.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getResourceCrn()).thenReturn(TEST_RESOURCE_CRN);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of(TEST_ACCOUNT_ID));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn(TEST_ENVIRONMENT_CRN);
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.valueOf(TEST_CLOUD_PROVIDER));
        when(cmTemplateProcessor.getCmVersion()).thenReturn(Optional.of(cmGbn));

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(serviceConfigs)
                .extracting(ApiClusterTemplateConfig::getName)
                .containsExactly("environment_crn", "resource_crn", "environment_account_id", "environment_cloud_provider");
        assertThat(serviceConfigs)
                .extracting(ApiClusterTemplateConfig::getValue)
                .containsExactly(TEST_ENVIRONMENT_CRN, TEST_RESOURCE_CRN, TEST_ACCOUNT_ID, TEST_CLOUD_PROVIDER);
    }

    @Test
    void testGetServiceConfigsWhenAccountIdIsEmptyAndCloudProviderNull() {
        when(source.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn(TEST_ENVIRONMENT_CRN);
        when(generalClusterConfigs.getResourceCrn()).thenReturn(TEST_RESOURCE_CRN);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.empty());
        when(cmTemplateProcessor.getCmVersion()).thenReturn(Optional.ofNullable(CLOUDERAMANAGER_VERSION_7_12_0_500.getVersion()));

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, source);

        assertThat(serviceConfigs)
                .extracting(ApiClusterTemplateConfig::getName)
                .containsExactly("environment_crn", "resource_crn", "environment_account_id", "environment_cloud_provider");
        assertThat(serviceConfigs)
                .extracting(ApiClusterTemplateConfig::getValue)
                .containsExactly(TEST_ENVIRONMENT_CRN, TEST_RESOURCE_CRN, "UNKNOWN", null);
    }

    @Test
    void testIsConfigurationRequiredReturnsTrue() {
        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    void testFilterByHostGroupViewTypeReturnsCorrectPredicate() {
        assertTrue(underTest.filterByHostGroupViewType().test(mock(HostgroupView.class)));
        assertTrue(underTest.filterByHostGroupViewType().test(null));
    }

    @ValueSource(strings = {"7.12.0", "7.11.0", "7.12.0.400", ""})
    @ParameterizedTest
    void testShouldNotReturnAnyClusterConfigIfCMVersionIsNotApplicable(String cmVersion) {
        when(cmTemplateProcessor.getCmVersion()).thenReturn(Optional.ofNullable(cmVersion));
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, source);
        assertEquals(0, serviceConfigs.size());
    }

    static Stream<Arguments> testIsServiceConfigUpdateNeededForUpgradeArguments() {
        return Stream.of(
                Arguments.of("7.12.0.500", true),
                Arguments.of("7.12.0.500-58279810", true),
                Arguments.of("7.12.0.600", true),
                Arguments.of("7.11.0", false),
                Arguments.of("7.12.0", false),
                Arguments.of("7.12.0.400", false),
                Arguments.of("", false)
        );
    }

    @MethodSource("testIsServiceConfigUpdateNeededForUpgradeArguments")
    @ParameterizedTest
    void testIsServiceConfigUpdateNeededForUpgrade(String toCmVersion, boolean expected) {
        assertEquals(expected, underTest.isServiceConfigUpdateNeededForUpgrade(null, toCmVersion));
    }

    @Test
    void testGetUpdatedServiceConfigForUpgrade() {
        when(source.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getResourceCrn()).thenReturn(TEST_RESOURCE_CRN);
        when(generalClusterConfigs.getAccountId()).thenReturn(Optional.of(TEST_ACCOUNT_ID));
        when(generalClusterConfigs.getEnvironmentCrn()).thenReturn(TEST_ENVIRONMENT_CRN);
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.valueOf(TEST_CLOUD_PROVIDER));

        Map<String, String> serviceConfigs = underTest.getUpdatedServiceConfigForUpgrade(cmTemplateProcessor, source);

        assertThat(serviceConfigs).containsExactlyInAnyOrderEntriesOf(Map.of(
                "environment_crn", TEST_ENVIRONMENT_CRN,
                "resource_crn", TEST_RESOURCE_CRN,
                "environment_account_id", TEST_ACCOUNT_ID,
                "environment_cloud_provider", TEST_CLOUD_PROVIDER
        ));
    }
}