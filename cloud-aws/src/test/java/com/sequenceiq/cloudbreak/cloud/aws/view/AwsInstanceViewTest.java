package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.common.api.type.EncryptionType;

public class AwsInstanceViewTest {

    private static final String IMAGE_ID = "imageId";

    private static final String ENCRYPTION_KEY_ARN = "encryptionKeyArn";

    @Test
    public void testEncryptionParametersWhenDefaultKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true);
        map.put(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name());

        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                map, 0L, IMAGE_ID);

        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);

        assertThat(actual.isKmsEnabled()).isTrue();
        assertThat(actual.isKmsDefault()).isTrue();
        assertThat(actual.isKmsCustom()).isFalse();
        assertThat(actual.getKmsKey()).isNull();
        assertThat(actual.isFastEbsEncryptionEnabled()).isFalse();
    }

    @Test
    public void testEncryptionParametersWhenCustomKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true);
        map.put(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name());
        map.put(AwsInstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY_ARN);

        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                map, 0L, IMAGE_ID);

        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);

        assertThat(actual.isKmsEnabled()).isTrue();
        assertThat(actual.isKmsDefault()).isFalse();
        assertThat(actual.isKmsCustom()).isTrue();
        assertThat(actual.getKmsKey()).isEqualTo(ENCRYPTION_KEY_ARN);
        assertThat(actual.isFastEbsEncryptionEnabled()).isFalse();
    }

    @Test
    public void testEncryptionParametersWhenFast() {
        Map<String, Object> map = new HashMap<>();
        map.put(AwsInstanceTemplate.FAST_EBS_ENCRYPTION_ENABLED, true);

        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                map, 0L, IMAGE_ID);

        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);

        assertThat(actual.isFastEbsEncryptionEnabled()).isTrue();
    }

    @Test
    public void testOnDemand() {
        Map<String, Object> map = new HashMap<>();
        map.put(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, 30);
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                map, 0L, IMAGE_ID);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertThat(actual.getOnDemandPercentage()).isEqualTo(70);
    }

    @Test
    public void testOnDemandMissingPercentage() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                Map.of(), 0L, IMAGE_ID);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertThat(actual.getOnDemandPercentage()).isEqualTo(100);
    }

}
