package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class RangerRazDatalakeConfigProviderTest {

    private final RangerRazDatalakeConfigProvider configProvider = new RangerRazDatalakeConfigProvider();

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Test
    @DisplayName("CM 7.2.0 DL is used but Raz is not requested, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsNotEnabled() {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(false);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView idbroker = new HostgroupView("idbroker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, idbroker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @Test
    @DisplayName("CM 7.1.0 DL is used and Raz is requested, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledWithCm710() {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.1.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView idbroker = new HostgroupView("idbroker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, idbroker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @Test
    @DisplayName("CM 7.2.0 DL is used and Raz is requested, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledWithCm720() {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView idbroker = new HostgroupView("idbroker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, idbroker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @Test
    @DisplayName("CM 7.2.1 DL is used and Raz is requested, Raz service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledWithCm721() {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.1");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView idbroker = new HostgroupView("idbroker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, idbroker))
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

    @Test
    @DisplayName("CM 7.2.0 DL is used and Raz is requested, but Data Hub, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledAndDataHub() {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView idbroker = new HostgroupView("idbroker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, idbroker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

    @Test
    @DisplayName("CM 7.2.0 DL is used and Raz is requested, but AWS, no additional service needs to be added to the template")
    void getAdditionalServicesWhenRazIsEnabledAndAWS() {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, List.of());
        HostgroupView idbroker = new HostgroupView("idbroker", 0, InstanceGroupType.CORE, List.of());
        TemplatePreparationObject preparationObject = Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withCloudPlatform(CloudPlatform.AWS)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, idbroker))
                .build();
        Map<String, ApiClusterTemplateService> additionalServices = configProvider.getAdditionalServices(cmTemplateProcessor, preparationObject);

        assertEquals(0, additionalServices.size());
    }

}