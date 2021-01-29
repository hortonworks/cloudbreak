package com.sequenceiq.freeipa.converter.instance.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateSpotParameters;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceTypeProvider;

@ExtendWith(MockitoExtension.class)
class InstanceTemplateRequestToTemplateConverterTest {

    private static final CloudPlatform CLOUD_PLATFORM = CloudPlatform.AWS;

    private static final String ACCOUNT_ID = "accountId";

    private static final String INSTANCE_TYPE = "m5.2xlarge";

    private static final int SPOT_PERCENTAGE = 100;

    private static final Double SPOT_MAX_PRICE = 1.0;

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private InstanceTemplateRequestToTemplateConverter underTest;

    @Test
    void shouldSetInstanceTypeWhenProvided() {
        InstanceTemplateRequest source = new InstanceTemplateRequest();
        source.setInstanceType(INSTANCE_TYPE);

        Template result = underTest.convert(source, CLOUD_PLATFORM, ACCOUNT_ID);

        assertThat(result.getInstanceType()).isEqualTo(source.getInstanceType());
    }

    @Test
    void shouldSetDefaultInstanceTypeWhenNotProvided() {
        String defaultInstanceType = "default";

        InstanceTemplateRequest source = new InstanceTemplateRequest();
        when(defaultInstanceTypeProvider.getForPlatform(CLOUD_PLATFORM.name())).thenReturn(defaultInstanceType);

        Template result = underTest.convert(source, CLOUD_PLATFORM, ACCOUNT_ID);

        assertThat(result.getInstanceType()).isEqualTo(defaultInstanceType);
    }

    @Test
    void shouldSetSpotPercentagePropertyWhenProvided() {
        InstanceTemplateRequest source = new InstanceTemplateRequest();
        source.setInstanceType(INSTANCE_TYPE);
        source.setAws(createAwsInstanceTemplateParameters(SPOT_PERCENTAGE, SPOT_MAX_PRICE));

        Template result = underTest.convert(source, CLOUD_PLATFORM, ACCOUNT_ID);

        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE)).isEqualTo(SPOT_PERCENTAGE);
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EC2_SPOT_MAX_PRICE)).isEqualTo(SPOT_MAX_PRICE);
    }

    @Test
    void shouldNotSetVolumeEncryptionWhenAzure() {
        InstanceTemplateRequest source = new InstanceTemplateRequest();
        source.setInstanceType(INSTANCE_TYPE);

        Template result = underTest.convert(source, CloudPlatform.AZURE, ACCOUNT_ID);

        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED)).isNull();
        assertThat(attributes.<Object>getValue(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isNull();
    }

    @Test
    void shouldSetVolumeEncryptionWhenAws() {
        InstanceTemplateRequest source = new InstanceTemplateRequest();
        source.setInstanceType(INSTANCE_TYPE);

        Template result = underTest.convert(source, CLOUD_PLATFORM, ACCOUNT_ID);

        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED)).isEqualTo(Boolean.TRUE);
        assertThat(attributes.<Object>getValue(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isEqualTo(EncryptionType.DEFAULT.name());
    }

    @Test
    void shouldSetVolumeEncryptionAndSpotValuesPropertyWhenAwsAndSpotValuesProvided() {
        InstanceTemplateRequest source = new InstanceTemplateRequest();
        source.setInstanceType(INSTANCE_TYPE);
        source.setAws(createAwsInstanceTemplateParameters(SPOT_PERCENTAGE, SPOT_MAX_PRICE));

        Template result = underTest.convert(source, CLOUD_PLATFORM, ACCOUNT_ID);

        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED)).isEqualTo(Boolean.TRUE);
        assertThat(attributes.<Object>getValue(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isEqualTo(EncryptionType.DEFAULT.name());
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE)).isEqualTo(SPOT_PERCENTAGE);
        assertThat(attributes.<Object>getValue(AwsInstanceTemplate.EC2_SPOT_MAX_PRICE)).isEqualTo(SPOT_MAX_PRICE);
    }

    private AwsInstanceTemplateParameters createAwsInstanceTemplateParameters(int spotPercentage, Double spotMaxPrice) {
        AwsInstanceTemplateSpotParameters spot = new AwsInstanceTemplateSpotParameters();
        spot.setPercentage(spotPercentage);
        spot.setMaxPrice(spotMaxPrice);
        AwsInstanceTemplateParameters aws = new AwsInstanceTemplateParameters();
        aws.setSpot(spot);
        return aws;
    }

}