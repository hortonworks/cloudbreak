package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

@ExtendWith(MockitoExtension.class)
public class RangerRazBaseConfigProviderTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    @InjectMocks
    private TestRangerRazBaseConfigProvider underTest;

    @Test
    public void getRoleConfigWheAWSAnd7210ShouldAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.10"));
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.10", "CDH", null, blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.AWS)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .withAccountMappingView(getAccountMappingView())
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(1, roleConfigs.size());
        assertEquals("ranger-raz-conf/ranger-raz-site.xml_role_safety_valve", roleConfigs.get(0).getName());
        assertEquals("<property><name>ranger.raz.bootstrap.servicetypes</name><value>s3</value></property>", roleConfigs.get(0).getValue());
    }

    @Test
    public void getRoleConfigWheAzureAnd7210ShouldAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.10"));
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.10", "CDH", null, blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .withAccountMappingView(getAccountMappingView())
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(1, roleConfigs.size());
        assertEquals("ranger-raz-conf/ranger-raz-site.xml_role_safety_valve", roleConfigs.get(0).getName());
        assertEquals("<property><name>ranger.raz.bootstrap.servicetypes</name><value>adls</value></property>", roleConfigs.get(0).getValue());
    }

    @Test
    public void getRoleConfigWhenGcpDataLakeShouldAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.10"));
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.11.0");

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withBlueprintView(new BlueprintView("", "7.2.10", "CDH", null, blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.GCP)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .withAccountMappingView(getAccountMappingView())
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(1, roleConfigs.size());
        assertEquals("ranger-raz-conf/ranger-raz-site.xml_role_safety_valve", roleConfigs.get(0).getName());
        assertEquals("<property><name>ranger.raz.bootstrap.servicetypes</name><value>gs</value></property>" +
                "<property><name>ranger.raz.gs.service.account</name>" +
                "<value>rangerrazauthorizer@gcp-dev-cloudbreak.iam.gserviceaccount.com</value></property>", roleConfigs.get(0).getValue());
    }

    @Test
    public void getRoleConfigWhenGcpDataLakeAndRazRoleisNullShouldAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.10"));
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.9.0");

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withBlueprintView(new BlueprintView("", "7.2.10", "CDH", null, blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.GCP)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(1, roleConfigs.size());
        assertEquals("ranger-raz-conf/ranger-raz-site.xml_role_safety_valve", roleConfigs.get(0).getName());
        assertEquals("<property><name>ranger.raz.bootstrap.servicetypes</name><value>gs</value></property>", roleConfigs.get(0).getValue());
    }

    @Test
    public void getRoleConfigWhenGcpDataHubAnd7210ShouldNotAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.10"));
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.16");

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.10", "CDH", null, blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.GCP)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .withAccountMappingView(getAccountMappingView())
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(0, roleConfigs.size());
    }

    @Test
    public void getRoleConfigWheAzureAnd729ShouldNOTAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.9"));
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.9", "CDH", null, blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .withAccountMappingView(getAccountMappingView())
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(0, roleConfigs.size());
    }

    @Test
    public void getRoleConfigWheAAWSAnd729ShouldNOTAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.9"));
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.9", "CDH", null, blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.AWS)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .withAccountMappingView(getAccountMappingView())
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(0, roleConfigs.size());
    }

    @Test
    public void getRoleConfigWhenGcpDataLakeAnd729ShouldNOTAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.9"));
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.16");

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.DATALAKE)
                .withBlueprintView(new BlueprintView("", "7.2.9", "CDH", null, blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.GCP)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .withAccountMappingView(getAccountMappingView())
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(0, roleConfigs.size());
    }

    @Test
    public void getRoleConfigWheAYarnAnd7210ShouldNOTAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.10"));
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion("7.2.0");

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.10", "CDH", null, blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.YARN)
                .withProductDetails(cmRepo, List.of())
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false, DATALAKE_CRN, false))
                .withAccountMappingView(getAccountMappingView())
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(0, roleConfigs.size());
    }

    private AccountMappingView getAccountMappingView() {
        Map<String, String> userMappingView = new HashMap<>() {
            {
                put("rangerraz", "rangerrazauthorizer@gcp-dev-cloudbreak.iam.gserviceaccount.com");
            }
        };
        return new AccountMappingView(null, userMappingView);
    }
}