package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

@ExtendWith(MockitoExtension.class)
public class RangerRazBaseConfigProviderTest {

    @InjectMocks
    private TestRangerRazBaseConfigProvider underTest;

    @Test
    public void getServiceTypesConfigWheAWSAnd7210ShouldAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.10"));

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.10", "CDH", blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.AWS)
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false))
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(1, roleConfigs.size());
        assertEquals("<property><name>ranger.raz.bootstrap.servicetypes</name><value>s3</value></property>", roleConfigs.get(0).getValue());
    }

    @Test
    public void getServiceTypesConfigWheAzureAnd7210ShouldAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.10"));

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.10", "CDH", blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.AZURE)
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false))
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(1, roleConfigs.size());
        assertEquals("<property><name>ranger.raz.bootstrap.servicetypes</name><value>adls</value></property>", roleConfigs.get(0).getValue());
    }

    @Test
    public void getServiceTypesConfigWheAzureAnd729ShouldNOTAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.9"));

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.9", "CDH", blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.AZURE)
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false))
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(0, roleConfigs.size());
    }

    @Test
    public void getServiceTypesConfigWheAAWSAnd729ShouldNOTAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.9"));

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.9", "CDH", blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.AWS)
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false))
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(0, roleConfigs.size());
    }

    @Test
    public void getServiceTypesConfigWheAGCPAnd729ShouldNOTAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.9"));

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.9", "CDH", blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.GCP)
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false))
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(0, roleConfigs.size());
    }

    @Test
    public void getServiceTypesConfigWheAGCPAnd7210ShouldNOTAddProperty() {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessor.getVersion()).thenReturn(Optional.of("7.2.10"));

        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withStackType(StackType.WORKLOAD)
                .withBlueprintView(new BlueprintView("", "7.2.10", "CDH", blueprintTextProcessor))
                .withCloudPlatform(CloudPlatform.GCP)
                .withGeneralClusterConfigs(new GeneralClusterConfigs())
                .withDataLakeView(new DatalakeView(false))
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", preparationObject);

        assertEquals(0, roleConfigs.size());
    }
}