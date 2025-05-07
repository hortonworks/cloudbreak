package com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_1_300;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.clo.CLOServiceRoles.CLO_SERVER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.clo.CLOServiceRoles.CLO_SERVICE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.dlm.DLMServiceRoles.DLM_SERVER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.dlm.DLMServiceRoles.DLM_SERVICE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.MeteringV2ServiceRoles.METERINGV2_SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class MeteringV2ConfigProviderTest {

    @Mock
    private CmTemplateProcessor mockTemplateProcessor;

    @Mock
    private AltusDatabusConfiguration altusDatabusConfiguration;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @InjectMocks
    private MeteringV2ConfigProvider underTest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "dbusAppName", "manowar_dev-mow-MeteringV2");
        ReflectionTestUtils.setField(underTest, "dbusStreamName", "manowar_dev-mow-MeteringV2");
    }

    @Test
    void getAdditionalServicesButDLMIsNotPresent() {
        ApiClusterTemplateService apiClusterTemplateService = mock(ApiClusterTemplateService.class);
        // This will cause isConfigurationNeeded to return false -> No metering config should get generated.
        when(mockTemplateProcessor.isRoleTypePresentInService(DLM_SERVICE, List.of(DLM_SERVER))).thenReturn(false);
        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertTrue(additionalServices.isEmpty());
    }

    @Test
    void getAdditionalServicesDLMIsPresentButMeteringIsNot() {
        ApiClusterTemplateService apiClusterTemplateService = mock(ApiClusterTemplateService.class);
        // This will cause isConfigurationNeeded to return true.
        when(mockTemplateProcessor.isRoleTypePresentInService(DLM_SERVICE, List.of(DLM_SERVER))).thenReturn(true);
        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);
        when(altusDatabusConfiguration.getAltusDatabusEndpoint()).thenReturn("endpoint");

        when(templatePreparationObject.getHostgroupViews()).thenReturn(Set.of(new HostgroupView("master", 0, InstanceGroupType.GATEWAY, 1)));
        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertFalse(additionalServices.isEmpty());
    }

    @Test
    void getAdditionalServicesButCLOIsNotPresent() {
        ApiClusterTemplateService apiClusterTemplateService = mock(ApiClusterTemplateService.class);
        // This will cause isConfigurationNeeded to return false -> No metering config should get generated.
        when(mockTemplateProcessor.isRoleTypePresentInService(DLM_SERVICE, Lists.newArrayList(DLM_SERVER))).thenReturn(false);
        when(mockTemplateProcessor.isRoleTypePresentInService(CLO_SERVICE, List.of(CLO_SERVER))).thenReturn(false);
        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertTrue(additionalServices.isEmpty());
    }

    @Test
    void getAdditionalServicesCLOIsPresentButMeteringIsNot() {
        ApiClusterTemplateService apiClusterTemplateService = mock(ApiClusterTemplateService.class);
        // This will cause isConfigurationNeeded to return true.
        when(mockTemplateProcessor.isRoleTypePresentInService(DLM_SERVICE, Lists.newArrayList(DLM_SERVER))).thenReturn(false);
        when(mockTemplateProcessor.isRoleTypePresentInService(CLO_SERVICE, List.of(CLO_SERVER))).thenReturn(true);
        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);
        when(altusDatabusConfiguration.getAltusDatabusEndpoint()).thenReturn("endpoint");

        when(templatePreparationObject.getHostgroupViews()).thenReturn(Set.of(new HostgroupView("master", 0, InstanceGroupType.GATEWAY, 1)));
        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertFalse(additionalServices.isEmpty());
    }

    @Test
    void testDbusHostName() {
        String expectedHost = "dbusapi.sigma-dev.cloudera.com";

        String inputHost = "https://dbusapi.sigma-dev.cloudera.com";
        String newHost = underTest.extractHost(inputHost);
        assertEquals(expectedHost, newHost);

        inputHost = "http://dbusapi.sigma-dev.cloudera.com";
        newHost = underTest.extractHost(inputHost);
        assertEquals(expectedHost, newHost);

        inputHost = "dbusapi.sigma-dev.cloudera.com";
        newHost = underTest.extractHost(inputHost);
        assertEquals(expectedHost, newHost);
    }

    @Test
    void getAdditionalServicesDLMIsPresentAndAlsoMetering() {
        ApiClusterTemplateService apiClusterTemplateService = mock(ApiClusterTemplateService.class);
        // This will cause isConfigurationNeeded to return true.
        when(mockTemplateProcessor.isRoleTypePresentInService(DLM_SERVICE, List.of(DLM_SERVER))).thenReturn(true);
        // This will list metering as present.
        when(altusDatabusConfiguration.getAltusDatabusEndpoint()).thenReturn("endpoint");
        when(mockTemplateProcessor.getServiceByType(METERINGV2_SERVICE)).thenReturn(Optional.of(apiClusterTemplateService));
        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);

        Map<String, ApiClusterTemplateService> additionalServices = underTest.getAdditionalServices(mockTemplateProcessor, templatePreparationObject);

        assertTrue(additionalServices.isEmpty());
    }

    @Test
    void configurationNeededIfDatalakeWithRightVersions() {
        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);
        when(altusDatabusConfiguration.getAltusDatabusEndpoint()).thenReturn("endpoint");

        ClouderaManagerRepo cm = new ClouderaManagerRepo();
        cm.setVersion("7.13.1.300-64531781");
        ProductDetailsView productDetailsView = new ProductDetailsView(cm, null);
        when(templatePreparationObject.getProductDetailsView()).thenReturn(productDetailsView);

        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of(CLOUDERA_STACK_VERSION_7_3_1.getVersion()));
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);

        assertTrue(underTest.isConfigurationNeeded(null, templatePreparationObject));
    }

    @Test
    void configurationNotNeededIfDatalakeWithWrongCMVersion() {
        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);

        ClouderaManagerRepo cm = new ClouderaManagerRepo();
        cm.setVersion(CLOUDERAMANAGER_VERSION_7_13_1.getVersion());
        ProductDetailsView productDetailsView = new ProductDetailsView(cm, null);
        when(templatePreparationObject.getProductDetailsView()).thenReturn(productDetailsView);

        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of(CLOUDERA_STACK_VERSION_7_3_1.getVersion()));
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);

        assertFalse(underTest.isConfigurationNeeded(null, templatePreparationObject));
    }

    @Test
    void configurationNotNeededIfDatalakeWithWrongCDHVersion() {
        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);

        ClouderaManagerRepo cm = new ClouderaManagerRepo();
        cm.setVersion(CLOUDERAMANAGER_VERSION_7_13_1_300.getVersion());
        ProductDetailsView productDetailsView = new ProductDetailsView(cm, null);
        when(templatePreparationObject.getProductDetailsView()).thenReturn(productDetailsView);

        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of(CLOUDERA_STACK_VERSION_7_2_18.getVersion()));
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);

        assertFalse(underTest.isConfigurationNeeded(null, templatePreparationObject));
    }

    @Test
    void configurationNotNeededIfDatalakeWithEmptyCDHVersion() {
        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);

        ClouderaManagerRepo cm = new ClouderaManagerRepo();
        cm.setVersion(CLOUDERAMANAGER_VERSION_7_13_1_300.getVersion());
        ProductDetailsView productDetailsView = new ProductDetailsView(cm, null);
        when(templatePreparationObject.getProductDetailsView()).thenReturn(productDetailsView);

        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.empty());
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);

        assertFalse(underTest.isConfigurationNeeded(null, templatePreparationObject));
    }

    @Test
    void configurationNotNeededIfDatalakeWithHighCDHVersion() {
        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);

        ClouderaManagerRepo cm = new ClouderaManagerRepo();
        cm.setVersion(CLOUDERAMANAGER_VERSION_7_13_1_300.getVersion());
        ProductDetailsView productDetailsView = new ProductDetailsView(cm, null);
        when(templatePreparationObject.getProductDetailsView()).thenReturn(productDetailsView);

        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of(CLOUDERA_STACK_VERSION_7_3_2.getVersion()));
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);

        assertFalse(underTest.isConfigurationNeeded(null, templatePreparationObject));
    }

    @Test
    void configurationNotNeededIfDatalakeWithHighCMVersion() {
        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);

        ClouderaManagerRepo cm = new ClouderaManagerRepo();
        cm.setVersion(CLOUDERAMANAGER_VERSION_7_13_2.getVersion());
        ProductDetailsView productDetailsView = new ProductDetailsView(cm, null);
        when(templatePreparationObject.getProductDetailsView()).thenReturn(productDetailsView);

        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of(CLOUDERA_STACK_VERSION_7_3_1.getVersion()));
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(blueprintTextProcessor);
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);

        assertFalse(underTest.isConfigurationNeeded(null, templatePreparationObject));
    }

    @Test
    void configurationNeededIfDatahubWithDLMRole() {
        when(mockTemplateProcessor.isRoleTypePresentInService(DLM_SERVICE, Lists.newArrayList(DLM_SERVER))).thenReturn(true);
        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);
        when(altusDatabusConfiguration.getAltusDatabusEndpoint()).thenReturn("endpoint");

        assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    void configurationNeededIfDatahubWithCLORole() {
        when(mockTemplateProcessor.isRoleTypePresentInService(DLM_SERVICE, Lists.newArrayList(DLM_SERVER))).thenReturn(false);
        when(mockTemplateProcessor.isRoleTypePresentInService(CLO_SERVICE, Lists.newArrayList(CLO_SERVER))).thenReturn(true);
        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);
        when(altusDatabusConfiguration.getAltusDatabusEndpoint()).thenReturn("endpoint");

        assertTrue(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }

    @Test
    void configurationNotNeededIfDatahubWithNoRole() {
        when(mockTemplateProcessor.isRoleTypePresentInService(DLM_SERVICE, Lists.newArrayList(DLM_SERVER))).thenReturn(false);
        when(mockTemplateProcessor.isRoleTypePresentInService(CLO_SERVICE, Lists.newArrayList(CLO_SERVER))).thenReturn(false);
        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);

        assertFalse(underTest.isConfigurationNeeded(mockTemplateProcessor, templatePreparationObject));
    }
}