package com.sequenceiq.freeipa.converter.instance.template;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateSpotParameters;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceTypeProvider;

@ExtendWith(MockitoExtension.class)
class InstanceTemplateRequestToTemplateConverterTest {

    private static final CloudPlatform CLOUD_PLATFORM = CloudPlatform.AWS;

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @InjectMocks
    private InstanceTemplateRequestToTemplateConverter underTest;

    @Test
    void shouldSetInstanceTypeWhenProvided() {
        InstanceTemplateRequest source = new InstanceTemplateRequest();
        source.setInstanceType("m5.2xlarge");

        Template result = underTest.convert(source, CLOUD_PLATFORM);

        Assertions.assertThat(result.getInstanceType()).isEqualTo(source.getInstanceType());
    }

    @Test
    void shouldSetDefaultInstanceTypeWhenNotProvided() {
        String defaultInstanceType = "default";

        InstanceTemplateRequest source = new InstanceTemplateRequest();
        Mockito.when(defaultInstanceTypeProvider.getForPlatform(CLOUD_PLATFORM.name())).thenReturn(defaultInstanceType);

        Template result = underTest.convert(source, CLOUD_PLATFORM);

        Assertions.assertThat(result.getInstanceType()).isEqualTo(defaultInstanceType);
    }

    @Test
    void shouldSetSpotPercentagePropertyWhenProvided() {
        int spotPercentage = 100;

        AwsInstanceTemplateSpotParameters spot = new AwsInstanceTemplateSpotParameters();
        spot.setPercentage(spotPercentage);
        AwsInstanceTemplateParameters aws = new AwsInstanceTemplateParameters();
        aws.setSpot(spot);

        InstanceTemplateRequest source = new InstanceTemplateRequest();
        source.setInstanceType("m5.2xlarge");
        source.setAws(aws);

        Template result = underTest.convert(source, CLOUD_PLATFORM);

        Object resultSpotPercentage = result.getAttributes().getValue("spotPercentage");
        Assertions.assertThat(resultSpotPercentage).isEqualTo(spotPercentage);
    }

}