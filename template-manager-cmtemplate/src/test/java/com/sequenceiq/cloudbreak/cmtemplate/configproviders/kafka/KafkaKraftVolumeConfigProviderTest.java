package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_BROKER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_KRAFT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class KafkaKraftVolumeConfigProviderTest {

    private final KafkaKraftVolumeConfigProvider provider = new KafkaKraftVolumeConfigProvider();

    @Test
    void getKraftVolumeConfigWithSingleVolume() {
        HostgroupView hostgroupView = new HostgroupView("kraft", 1, InstanceGroupType.CORE, 3);
        assertEquals(List.of(
                        config("metadata.log.dir", "/hadoopfs/fs1/kraft"),
                        config("kraft.properties_role_safety_valve", "log.dirs=/hadoopfs/fs1/kraft")),
                provider.getRoleConfigs(KAFKA_KRAFT, hostgroupView, null));
    }

    @Test
    void getKraftVolumeConfigWithMultipleVolumes() {
        HostgroupView hostgroupView = new HostgroupView("kraft", 3, InstanceGroupType.CORE, 3);
        assertEquals(List.of(
                        config("metadata.log.dir", "/hadoopfs/fs1/kraft"),
                        config("kraft.properties_role_safety_valve", "log.dirs=/hadoopfs/fs1/kraft,/hadoopfs/fs2/kraft,/hadoopfs/fs3/kraft")),
                provider.getRoleConfigs(KAFKA_KRAFT, hostgroupView, null));
    }

    @Test
    void getKraftVolumeConfigWithZeroVolumes() {
        assertEquals(List.of(
                        config("metadata.log.dir", "/hadoopfs/root1/kraft"),
                        config("kraft.properties_role_safety_valve", "log.dirs=/hadoopfs/root1/kraft")),
                provider.getRoleConfigs(KAFKA_KRAFT, null, null));
    }

    @Test
    void getKraftVolumeConfigWithIncorrectRoleType() {
        HostgroupView hostgroupView = new HostgroupView("kraft", 1, InstanceGroupType.CORE, 3);
        assertEquals(List.of(), provider.getRoleConfigs(KAFKA_BROKER, hostgroupView, null));
    }
}
