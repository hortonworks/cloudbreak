package com.sequenceiq.cloudbreak.cloud.aws.common.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.placement.AwsPlacementGroupStrategy;
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
                map, 0L, IMAGE_ID, TemporaryStorage.ATTACHED_VOLUMES, 0L);

        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);

        assertThat(actual.isKmsEnabled()).isTrue();
        assertThat(actual.isKmsDefault()).isTrue();
        assertThat(actual.isKmsCustom()).isFalse();
        assertThat(actual.getKmsKey()).isNull();
    }

    @Test
    public void testEncryptionParametersWhenCustomKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true);
        map.put(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name());
        map.put(AwsInstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY_ARN);

        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                map, 0L, IMAGE_ID, TemporaryStorage.ATTACHED_VOLUMES, 0L);

        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);

        assertThat(actual.isKmsEnabled()).isTrue();
        assertThat(actual.isKmsDefault()).isFalse();
        assertThat(actual.isKmsCustom()).isTrue();
        assertThat(actual.getKmsKey()).isEqualTo(ENCRYPTION_KEY_ARN);
    }

    @Test
    public void testOnDemand() {
        Map<String, Object> map = new HashMap<>();
        map.put(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, 30);
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                map, 0L, IMAGE_ID, TemporaryStorage.ATTACHED_VOLUMES, 0L);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertThat(actual.getOnDemandPercentage()).isEqualTo(70);
    }

    @Test
    public void testOnDemandMissingPercentage() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                Map.of(), 0L, IMAGE_ID, TemporaryStorage.ATTACHED_VOLUMES, 0L);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertThat(actual.getOnDemandPercentage()).isEqualTo(100);
    }

    @Test
    public void testSpotMaxPrice() {
        Map<String, Object> map = new HashMap<>();
        Double spotMaxPrice = 0.9;
        map.put(AwsInstanceTemplate.EC2_SPOT_MAX_PRICE, spotMaxPrice);
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                map, 0L, "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertEquals(spotMaxPrice, actual.getSpotMaxPrice());
    }

    @Test
    public void testMissingSpotMaxPrice() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                Map.of(), 0L, "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertNull(actual.getSpotMaxPrice());
    }

    @Test
    public void testPlacementGroupWhenPartition() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                Map.of(AwsInstanceTemplate.PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.PARTITION.name()),
                0L, "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertEquals(AwsPlacementGroupStrategy.PARTITION, actual.getPlacementGroupStrategy(), "Placement Group Strategy should be partition.");
    }

    @Test
    public void testPlacementGroupWhenSpread() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                Map.of(AwsInstanceTemplate.PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.SPREAD.name()),
                0L, "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertEquals(AwsPlacementGroupStrategy.SPREAD, actual.getPlacementGroupStrategy(), "Placement Group Strategy should be spread.");
    }

    @Test
    public void testPlacementGroupWhenCluster() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                Map.of(AwsInstanceTemplate.PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.CLUSTER.name()),
                0L, "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertEquals(AwsPlacementGroupStrategy.CLUSTER, actual.getPlacementGroupStrategy(), "Placement Group Strategy should be cluster.");
    }

    @Test
    public void testPlacementGroupWhenMissing() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                Map.of(), 0L, "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertEquals(AwsPlacementGroupStrategy.NONE, actual.getPlacementGroupStrategy(), "Placement Group Strategy should be none.");
    }

    @Test
    public void testPlacementGroupWhenNone() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("", "", 0L, Collections.emptyList(), InstanceStatus.STARTED,
                Map.of(AwsInstanceTemplate.PLACEMENT_GROUP_STRATEGY, AwsPlacementGroupStrategy.NONE.name()),
                0L, "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        AwsInstanceView actual = new AwsInstanceView(instanceTemplate);
        assertEquals(AwsPlacementGroupStrategy.NONE, actual.getPlacementGroupStrategy(), "Placement Group Strategy should be none.");
    }
}
