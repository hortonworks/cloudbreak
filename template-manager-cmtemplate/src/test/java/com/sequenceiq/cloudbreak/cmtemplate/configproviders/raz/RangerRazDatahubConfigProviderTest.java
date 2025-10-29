package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class RangerRazDatahubConfigProviderTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @InjectMocks
    private RangerRazDatahubConfigProvider configProvider;

    static Object[][] razCloudPlatformDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform
                {"CloudPlatform.AWS", CloudPlatform.AWS},
                {"CloudPlatform.AZURE", CloudPlatform.AZURE},

        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    @DisplayName("CM 7.2.0 DH is used but Raz is not requested, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsNotEnabled(String testCaseName, CloudPlatform cloudPlatform) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(cloudPlatform)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    @DisplayName("CM 7.1.0 DH is used and Raz is requested, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledWithCm710(String testCaseName, CloudPlatform cloudPlatform) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.1.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(cloudPlatform)
                .withProductDetails(cmRepo, List.of())
                .withDataLakeView(new DatalakeView(true, DATALAKE_CRN, false))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    @DisplayName("CM 7.2.0 DH is used and Raz is requested, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledWithCm720(String testCaseName, CloudPlatform cloudPlatform) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(cloudPlatform)
                .withProductDetails(cmRepo, List.of())
                .withDataLakeView(new DatalakeView(true, DATALAKE_CRN, false))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    @DisplayName("CM 7.2.1 DH is used and Raz is requested, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledWithCm721(String testCaseName, CloudPlatform cloudPlatform) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.1");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(cloudPlatform)
                .withProductDetails(cmRepo, List.of())
                .withDataLakeView(new DatalakeView(true, DATALAKE_CRN, false))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    @DisplayName("CM 7.2.2 DH is used and Raz is requested, Raz service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledWithCm722(String testCaseName, CloudPlatform cloudPlatform) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.2");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(cloudPlatform)
                .withProductDetails(cmRepo, List.of())
                .withDataLakeView(new DatalakeView(true, DATALAKE_CRN, false))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        ApiClusterTemplateService service = additionalServices.get("master");
        List<ApiClusterTemplateRoleConfigGroup> roleConfigGroups = service.getRoleConfigGroups();
        Assertions.assertAll(
                () -> assertEquals(1, additionalServices.size()),
                () -> assertEquals("RANGER_RAZ", service.getServiceType()),
                () -> assertEquals("ranger-RANGER_RAZ", service.getRefName()),
                () -> assertEquals("RANGER_RAZ_SERVER", roleConfigGroups.get(0).getRoleType()),
                () -> assertEquals("ranger-RANGER_RAZ_SERVER", roleConfigGroups.get(0).getRefName())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    @DisplayName("CM 7.2.2 DL is used and Raz is requested, no additional service needs to be added to the template")
    void getAdditionalServicesForDataLakeWhenRazIsEnabledWithCm722(String testCaseName, CloudPlatform cloudPlatform) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.2");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withCloudPlatform(cloudPlatform)
                .withProductDetails(cmRepo, List.of())
                .withDataLakeView(new DatalakeView(true, DATALAKE_CRN, false))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @Test
    void getAdditionalServicesWhenRazIsEnabledForGcpWithCm790() {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.9.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(GCP)
                .withProductDetails(cmRepo, List.of())
                .withDataLakeView(new DatalakeView(true, DATALAKE_CRN, false))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @Test
    @DisplayName("CM 7.2.2 DH is used and Raz is requested, but YARN, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledButNotSupportedCloud() {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.2");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(CloudPlatform.YARN)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    @DisplayName("CM 7.2.0 DH is used and Raz is requested, but Data Lake, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledAndDataLake(String testCaseName, CloudPlatform cloudPlatform) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView idbroker = new HostgroupView("idbroker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withCloudPlatform(cloudPlatform)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, idbroker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    @DisplayName("CM 7.2.2 DH is used and product details contains null as CM details at template generation before datahub cluster creations")
    void getAdditionalServicesWhenCmDetailsIsNullInProductDetails(String testCaseName, CloudPlatform cloudPlatform) {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(cloudPlatform)
                .withProductDetails(null, null)
                .withDataLakeView(new DatalakeView(true, DATALAKE_CRN, false))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    @DisplayName("CM 7.2.2 DH is used and product details is missing at template generation before datahub cluster creations")
    void getAdditionalServicesWhenProductDetailsIsMissing(String testCaseName, CloudPlatform cloudPlatform) {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(cloudPlatform)
                .withDataLakeView(new DatalakeView(true, DATALAKE_CRN, false))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }
}