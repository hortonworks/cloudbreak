package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AzureInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.RootVolumeV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class InstanceTemplateV1ToInstanceTemplateV4ConverterTest {

    private static final String INSTANCE_TYPE = "instanceType";

    @Mock
    private VolumeConverter volumeConverter;

    @Mock
    private InstanceTemplateParameterConverter instanceTemplateParameterConverter;

    @InjectMocks
    private InstanceTemplateV1ToInstanceTemplateV4Converter underTest;

    private DetailedEnvironmentResponse environment;

    @BeforeEach
    void setUp() {
        environment = new DetailedEnvironmentResponse();
    }

    @Test
    void convertTestInstanceTemplateV1RequestToInstanceTemplateV4RequestWhenMinimal() {
        InstanceTemplateV1Request source = new InstanceTemplateV1Request();
        source.setAzure(null);
        source.setInstanceType(INSTANCE_TYPE);

        AzureInstanceTemplateV4Parameters azureInstanceTemplateV4Parameters = new AzureInstanceTemplateV4Parameters();
        when(instanceTemplateParameterConverter.convert(any(AwsInstanceTemplateV1Parameters.class), eq(environment)))
                .thenReturn(null);
        when(instanceTemplateParameterConverter.convert(any(AzureInstanceTemplateV1Parameters.class), eq(environment)))
                .thenReturn(azureInstanceTemplateV4Parameters);

        InstanceTemplateV4Request instanceTemplateV4Request = underTest.convert(source, environment, true);

        assertThat(instanceTemplateV4Request).isNotNull();
        assertThat(instanceTemplateV4Request.getRootVolume()).isNull();
        assertThat(instanceTemplateV4Request.getAzure()).isSameAs(azureInstanceTemplateV4Parameters);
        assertThat(instanceTemplateV4Request.getCloudPlatform()).isNull();
        assertThat(instanceTemplateV4Request.getInstanceType()).isEqualTo(INSTANCE_TYPE);
    }

    @Test
    void convertTestInstanceTemplateV1RequestToInstanceTemplateV4RequestWhenAzure() {
        InstanceTemplateV1Request source = new InstanceTemplateV1Request();
        RootVolumeV1Request rootVolumeV1Request = new RootVolumeV1Request();
        source.setRootVolume(rootVolumeV1Request);
        AzureInstanceTemplateV1Parameters azureInstanceTemplateV1Parameters = new AzureInstanceTemplateV1Parameters();
        source.setAzure(azureInstanceTemplateV1Parameters);
        source.setInstanceType(INSTANCE_TYPE);

        environment.setCloudPlatform("AZURE");
        RootVolumeV4Request rootVolumeV4Request = new RootVolumeV4Request();
        when(volumeConverter.convert(rootVolumeV1Request, "AZURE", true)).thenReturn(rootVolumeV4Request);

        AzureInstanceTemplateV4Parameters azureInstanceTemplateV4Parameters = new AzureInstanceTemplateV4Parameters();
        when(instanceTemplateParameterConverter.convert(any(AwsInstanceTemplateV1Parameters.class), eq(environment))).thenReturn(null);
        when(instanceTemplateParameterConverter.convert(azureInstanceTemplateV1Parameters, environment)).thenReturn(azureInstanceTemplateV4Parameters);

        InstanceTemplateV4Request instanceTemplateV4Request = underTest.convert(source, environment, true);

        assertThat(instanceTemplateV4Request).isNotNull();
        assertThat(instanceTemplateV4Request.getRootVolume()).isSameAs(rootVolumeV4Request);
        assertThat(instanceTemplateV4Request.getAzure()).isSameAs(azureInstanceTemplateV4Parameters);
        assertThat(instanceTemplateV4Request.getCloudPlatform()).isEqualTo(CloudPlatform.AZURE);
        assertThat(instanceTemplateV4Request.getInstanceType()).isEqualTo(INSTANCE_TYPE);
    }

    @Test
    void convertTestInstanceTemplateV4RequestToInstanceTemplateV1RequestWhenMinimal() {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        source.setInstanceType(INSTANCE_TYPE);

        InstanceTemplateV1Request instanceTemplateV1Request = underTest.convert(source, environment, true);

        assertThat(instanceTemplateV1Request).isNotNull();
        assertThat(instanceTemplateV1Request.getRootVolume()).isNull();
        assertThat(instanceTemplateV1Request.getAzure()).isNull();
        assertThat(instanceTemplateV1Request.getAws()).isNull();
        assertThat(instanceTemplateV1Request.getCloudPlatform()).isNull();
        assertThat(instanceTemplateV1Request.getInstanceType()).isEqualTo(INSTANCE_TYPE);
    }

    @Test
    void convertTestInstanceTemplateV4RequestToInstanceTemplateV1RequestWhenAzure() {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        RootVolumeV4Request rootVolumeV4Request = new RootVolumeV4Request();
        source.setRootVolume(rootVolumeV4Request);
        AzureInstanceTemplateV4Parameters azureInstanceTemplateV4Parameters = new AzureInstanceTemplateV4Parameters();
        source.setAzure(azureInstanceTemplateV4Parameters);
        source.setInstanceType(INSTANCE_TYPE);

        environment.setCloudPlatform("AZURE");
        RootVolumeV1Request rootVolumeV1Request = new RootVolumeV1Request();
        when(volumeConverter.convert(rootVolumeV4Request, "AZURE", true)).thenReturn(rootVolumeV1Request);

        AzureInstanceTemplateV1Parameters azureInstanceTemplateV1Parameters = new AzureInstanceTemplateV1Parameters();
        when(instanceTemplateParameterConverter.convert(azureInstanceTemplateV4Parameters)).thenReturn(azureInstanceTemplateV1Parameters);

        InstanceTemplateV1Request instanceTemplateV1Request = underTest.convert(source, environment, true);

        assertThat(instanceTemplateV1Request).isNotNull();
        assertThat(instanceTemplateV1Request.getRootVolume()).isSameAs(rootVolumeV1Request);
        assertThat(instanceTemplateV1Request.getAzure()).isSameAs(azureInstanceTemplateV1Parameters);
        assertThat(instanceTemplateV1Request.getCloudPlatform()).isEqualTo(CloudPlatform.AZURE);
        assertThat(instanceTemplateV1Request.getInstanceType()).isEqualTo(INSTANCE_TYPE);
    }

    @Test
    void testConvertInstanceTemplateV1RequestToInstanceTemplateV4RequestWhenAws() {
        InstanceTemplateV1Request source = new InstanceTemplateV1Request();
        RootVolumeV1Request rootVolumeV1Request = new RootVolumeV1Request();
        source.setRootVolume(rootVolumeV1Request);
        AwsInstanceTemplateV1Parameters awsParameters = new AwsInstanceTemplateV1Parameters();
        source.setAws(awsParameters);
        source.setInstanceType(INSTANCE_TYPE);

        environment.setCloudPlatform("AWS");
        RootVolumeV4Request rootVolumeV4Request = new RootVolumeV4Request();
        when(volumeConverter.convert(rootVolumeV1Request, "AWS", true)).thenReturn(rootVolumeV4Request);

        AwsInstanceTemplateV4Parameters awsInstanceTemplateV4Parameters = new AwsInstanceTemplateV4Parameters();
        when(instanceTemplateParameterConverter.convert(awsParameters, environment)).thenReturn(awsInstanceTemplateV4Parameters);

        InstanceTemplateV4Request instanceTemplateV4Request = underTest.convert(source, environment, true);

        assertThat(instanceTemplateV4Request).isNotNull();
        assertThat(instanceTemplateV4Request.getRootVolume()).isSameAs(rootVolumeV4Request);
        assertThat(instanceTemplateV4Request.getAws()).isSameAs(awsInstanceTemplateV4Parameters);
        assertThat(instanceTemplateV4Request.getCloudPlatform()).isEqualTo(CloudPlatform.AWS);
        assertThat(instanceTemplateV4Request.getInstanceType()).isEqualTo(INSTANCE_TYPE);
    }

    @Test
    void testConvertInstanceTemplateV1RequestToInstanceTemplateV4RequestWhenMinimal() {
        InstanceTemplateV1Request source = new InstanceTemplateV1Request();
        source.setAws(null);
        source.setInstanceType(INSTANCE_TYPE);

        AwsInstanceTemplateV4Parameters awsInstanceTemplateV4Parameters = new AwsInstanceTemplateV4Parameters();
        when(instanceTemplateParameterConverter.convert(any(AwsInstanceTemplateV1Parameters.class), eq(environment)))
                .thenReturn(awsInstanceTemplateV4Parameters);

        InstanceTemplateV4Request instanceTemplateV4Request = underTest.convert(source, environment, true);

        assertThat(instanceTemplateV4Request).isNotNull();
        assertThat(instanceTemplateV4Request.getRootVolume()).isNull();
        assertThat(instanceTemplateV4Request.getAws()).isSameAs(awsInstanceTemplateV4Parameters);
        assertThat(instanceTemplateV4Request.getCloudPlatform()).isNull();
        assertThat(instanceTemplateV4Request.getInstanceType()).isEqualTo(INSTANCE_TYPE);
    }

    @Test
    void convertTestInstanceTemplateV4RequestToInstanceTemplateV1RequestWhenAws() {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        RootVolumeV4Request rootVolumeV4Request = new RootVolumeV4Request();
        source.setRootVolume(rootVolumeV4Request);
        AwsInstanceTemplateV4Parameters awsInstanceTemplateV4Parameters = new AwsInstanceTemplateV4Parameters();
        source.setAws(awsInstanceTemplateV4Parameters);
        source.setInstanceType(INSTANCE_TYPE);

        environment.setCloudPlatform("AWS");
        RootVolumeV1Request rootVolumeV1Request = new RootVolumeV1Request();
        when(volumeConverter.convert(rootVolumeV4Request, "AWS", true)).thenReturn(rootVolumeV1Request);

        AwsInstanceTemplateV1Parameters awsInstanceTemplateV1Parameters = new AwsInstanceTemplateV1Parameters();
        when(instanceTemplateParameterConverter.convert(awsInstanceTemplateV4Parameters, environment)).thenReturn(awsInstanceTemplateV1Parameters);

        InstanceTemplateV1Request instanceTemplateV1Request = underTest.convert(source, environment, true);

        assertThat(instanceTemplateV1Request).isNotNull();
        assertThat(instanceTemplateV1Request.getRootVolume()).isSameAs(rootVolumeV1Request);
        assertThat(instanceTemplateV1Request.getAws()).isSameAs(awsInstanceTemplateV1Parameters);
        assertThat(instanceTemplateV1Request.getCloudPlatform()).isEqualTo(CloudPlatform.AWS);
        assertThat(instanceTemplateV1Request.getInstanceType()).isEqualTo(INSTANCE_TYPE);
    }

}