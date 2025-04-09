package com.sequenceiq.cloudbreak.cmtemplate.configproviders.rms;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.rms.RangerRmsRoles.RANGER_RMS_SERVER_REF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.rms.RangerRmsRoles.RANGER_RMS_SERVER_ROLE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.rms.RangerRmsRoles.RANGER_RMS_SERVICE_TYPE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupName;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class RangerRmsDatalakeConfigProviderTest {

    private static final String RMS_MINIMUM_VERSION = "7.2.18";

    private static final String RMS_UNSUPPORTED_VERSION = "7.2.16";

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    private static final String RMS_HOST_GROUP = InstanceGroupName.MASTER.getName();

    private static final String HMS_MAP_MANAGED_TABLES = "ranger_rms_hms_map_managed_tables";

    private static final String HMS_SOURCE_SERVICE_NAME = "ranger_rms_hms_source_service_name";

    private static final String HMS_SOURCE_SERVICE_NAME_VALUE = "cm_s3";

    private static final String SUPPORTED_URI_SCHEME = "ranger_rms_supported_uri_scheme";

    private static final String SUPPORTED_URI_SCHEME_VALUE = "s3a";

    private static final String HA_ENABLED = "ranger_rms_server_ha_enabled";

    private static final String SERVER_IDS = "ranger_rms_server_ids";

    private static final String SERVER_IDS_VALUE = "id1,id2";

    private final RangerRmsDatalakeConfigProvider underTest = new RangerRmsDatalakeConfigProvider();

    private final EntitlementService entitlementService = mock(EntitlementService.class);

    private final CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);

    @BeforeEach
    public void setup() {
        openMocks(this);
        ReflectionTestUtils.setField(underTest, "entitlementService", entitlementService, EntitlementService.class);
    }

    @Test
    public void getAdditionalServiceWhenRmsIsEnabled() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(true);
        when(cmTemplateProcessor.getVersion()).thenReturn(Optional.of(RMS_MINIMUM_VERSION));
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AWS, StackType.DATALAKE, true);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(cmTemplateProcessor, templatePreparationObject);
            assertTrue(additionalServices.containsKey(RMS_HOST_GROUP));
            ApiClusterTemplateService clusterTemplateService = additionalServices.get(RMS_HOST_GROUP);
            List<ApiClusterTemplateRoleConfigGroup> roleConfigGroups = clusterTemplateService.getRoleConfigGroups();
            Assertions.assertAll(
                    () -> assertEquals(1, additionalServices.size()),
                    () -> assertEquals(RangerRmsRoles.RANGER_RMS_SERVICE_TYPE, clusterTemplateService.getServiceType()),
                    () -> assertEquals(RangerRmsRoles.RANGER_RMS_SERVICE_REF_NAME, clusterTemplateService.getRefName()),
                    () -> assertEquals(RangerRmsRoles.RANGER_RMS_SERVER_REF_NAME, roleConfigGroups.get(0).getRefName()),
                    () -> assertTrue(roleConfigGroups.get(0).getBase()),
                    () -> assertEquals(RangerRmsRoles.RANGER_RMS_SERVER_ROLE_TYPE, roleConfigGroups.get(0).getRoleType())
            );
        });
    }

    @Test
    public void getAdditionalServiceWhenRmsIsDisabled() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(true);
        when(cmTemplateProcessor.getVersion()).thenReturn(Optional.of(RMS_MINIMUM_VERSION));
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AWS, StackType.DATALAKE, false);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(cmTemplateProcessor, templatePreparationObject);
            ApiClusterTemplateService clusterTemplateService = additionalServices.get(RMS_HOST_GROUP);
            assertNull(clusterTemplateService);
        });
    }

    @Test
    public void getRangerRmsRoleConfigs() {
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AWS, StackType.DATALAKE, true);
        ApiClusterTemplateService apiClusterTemplateService = mock(ApiClusterTemplateService.class);
        when(cmTemplateProcessor.getServiceByType(eq(RANGER_RMS_SERVICE_TYPE))).thenReturn(Optional.of(apiClusterTemplateService));
        ApiClusterTemplateRoleConfigGroup apiClusterTemplateRoleConfigGroup = mock(ApiClusterTemplateRoleConfigGroup.class);
        when(apiClusterTemplateService.getRoleConfigGroups()).thenReturn(List.of(apiClusterTemplateRoleConfigGroup));
        when(apiClusterTemplateRoleConfigGroup.getRoleType()).thenReturn(RANGER_RMS_SERVER_ROLE_TYPE);
        when(apiClusterTemplateRoleConfigGroup.getRefName()).thenReturn(RANGER_RMS_SERVER_REF_NAME);
        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, templatePreparationObject);
        assertEquals(1, roleConfigs.size());
        assertTrue(roleConfigs.containsKey(RANGER_RMS_SERVER_REF_NAME));
        List<ApiClusterTemplateConfig> configs = roleConfigs.get(RANGER_RMS_SERVER_REF_NAME);
        assertAll(
                () -> assertEquals(HMS_MAP_MANAGED_TABLES, configs.get(0).getName()),
                () -> assertEquals(HMS_SOURCE_SERVICE_NAME, configs.get(1).getName()),
                () -> assertEquals(SUPPORTED_URI_SCHEME, configs.get(2).getName()),
                () -> assertEquals("true", configs.get(0).getValue()),
                () -> assertEquals(HMS_SOURCE_SERVICE_NAME_VALUE, configs.get(1).getValue()),
                () -> assertEquals(SUPPORTED_URI_SCHEME_VALUE, configs.get(2).getValue())
        );
    }

    @Test
    public void getRangerRmsRoleConfigsHa() {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion(RMS_MINIMUM_VERSION);
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, 2);
        HostgroupView idbroker = new HostgroupView("idbroker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withCloudPlatform(CloudPlatform.AWS)
                .withProductDetails(cmRepo, List.of())
                .withBlueprintView(new BlueprintView("", RMS_MINIMUM_VERSION, "CDP", null, cmTemplateProcessor))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, idbroker))
                .withDataLakeView(new DatalakeView(true, null, false))
                .build();
        ApiClusterTemplateService apiClusterTemplateService = mock(ApiClusterTemplateService.class);
        when(cmTemplateProcessor.getServiceByType(eq(RANGER_RMS_SERVICE_TYPE))).thenReturn(Optional.of(apiClusterTemplateService));
        ApiClusterTemplateRoleConfigGroup apiClusterTemplateRoleConfigGroup = mock(ApiClusterTemplateRoleConfigGroup.class);
        when(apiClusterTemplateService.getRoleConfigGroups()).thenReturn(List.of(apiClusterTemplateRoleConfigGroup));
        when(apiClusterTemplateRoleConfigGroup.getRoleType()).thenReturn(RANGER_RMS_SERVER_ROLE_TYPE);
        when(apiClusterTemplateRoleConfigGroup.getRefName()).thenReturn(RANGER_RMS_SERVER_REF_NAME);
        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, templatePreparationObject);
        assertEquals(1, roleConfigs.size());
        assertTrue(roleConfigs.containsKey(RANGER_RMS_SERVER_REF_NAME));
        List<ApiClusterTemplateConfig> configs = roleConfigs.get(RANGER_RMS_SERVER_REF_NAME);
        assertAll(
                () -> assertEquals(HMS_MAP_MANAGED_TABLES, configs.get(0).getName()),
                () -> assertEquals(HMS_SOURCE_SERVICE_NAME, configs.get(1).getName()),
                () -> assertEquals(SUPPORTED_URI_SCHEME, configs.get(2).getName()),
                () -> assertEquals(HA_ENABLED, configs.get(3).getName()),
                () -> assertEquals(SERVER_IDS, configs.get(4).getName()),
                () -> assertEquals("true", configs.get(0).getValue()),
                () -> assertEquals(HMS_SOURCE_SERVICE_NAME_VALUE, configs.get(1).getValue()),
                () -> assertEquals(SUPPORTED_URI_SCHEME_VALUE, configs.get(2).getValue()),
                () -> assertEquals("true", configs.get(3).getValue()),
                () -> assertEquals(SERVER_IDS_VALUE, configs.get(4).getValue())
        );
    }

    @Test
    public void getAdditionalHostGroupForRsm() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(true);
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AWS, StackType.DATALAKE, true);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Set<String> hostGroups = underTest.getHostGroups(cmTemplateProcessor, templatePreparationObject);
            assertEquals(1, hostGroups.size());
            assertTrue(hostGroups.contains(RMS_HOST_GROUP));
        });
    }

    @Test
    public void getAdditionalHostGroupForRmsWhenItDisabled() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(true);
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AWS, StackType.DATALAKE, false);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Set<String> hostGroups = underTest.getHostGroups(cmTemplateProcessor, templatePreparationObject);
            assertTrue(hostGroups.isEmpty());
        });
    }

    @Test
    public void isRmsConfigurationNeeded() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(true);
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AWS, StackType.DATALAKE, true);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);
            assertTrue(configurationNeeded);
        });
    }

    @Test
    public void isRmsConfigurationNotNeededStackTypeWorkload() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(true);
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AWS, StackType.WORKLOAD, true);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);
            assertFalse(configurationNeeded);
        });
    }

    @Test
    public void isRmsConfigurationNotNeededRazDisabled() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(true);
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AWS, StackType.DATALAKE, false);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);
            assertFalse(configurationNeeded);
        });
    }

    @Test
    public void isRmsConfigurationNotNeededCloudPlatformAzure() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(true);
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AZURE, StackType.DATALAKE, true);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);
            assertFalse(configurationNeeded);
        });
    }

    @Test
    public void isRmsConfigurationNotNeededVersionLower() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(true);
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_UNSUPPORTED_VERSION, CloudPlatform.AWS, StackType.DATALAKE, true);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);
            assertFalse(configurationNeeded);
        });
    }

    @Test
    public void isRmsConfigurationNotNeededEntitlementDisabled() {
        when(entitlementService.isRmsEnabledOnDatalake(anyString())).thenReturn(false);
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(RMS_MINIMUM_VERSION, CloudPlatform.AWS, StackType.DATALAKE, true);
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);
            assertFalse(configurationNeeded);
        });
    }

    @Test
    public void getServiceTypeOfRms() {
        String serviceType = underTest.getServiceType();
        assertEquals(RangerRmsRoles.RANGER_RMS_SERVICE_TYPE, serviceType);
    }

    @Test
    public void getRoleTypesOfRms() {
        List<String> roleTypes = underTest.getRoleTypes();
        assertTrue(roleTypes.contains(RangerRmsRoles.RANGER_RMS_SERVER_ROLE_TYPE));
        assertEquals(1, roleTypes.size());
    }

    private TemplatePreparationObject getTemplatePreparationObject(String version, CloudPlatform platform, StackType stackType, boolean razEnabled) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion(version);
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(razEnabled);
        generalClusterConfigs.setEnableRangerRms(true);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView idbroker = new HostgroupView("idbroker", 0, InstanceGroupType.CORE, List.of());
        return TemplatePreparationObject.Builder.builder()
                .withStackType(stackType)
                .withCloudPlatform(platform)
                .withProductDetails(cmRepo, List.of())
                .withBlueprintView(new BlueprintView("", version, "CDP", null, cmTemplateProcessor))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, idbroker))
                .withDataLakeView(new DatalakeView(razEnabled, null, false))
                .build();
    }
}
