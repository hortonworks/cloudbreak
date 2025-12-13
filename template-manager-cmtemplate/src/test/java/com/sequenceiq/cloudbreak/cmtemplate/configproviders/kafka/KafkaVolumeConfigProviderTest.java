package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_BROKER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class KafkaVolumeConfigProviderTest {

    private final KafkaVolumeConfigProvider provider = new KafkaVolumeConfigProvider();

    @Test
    void getKafkaVolumeConfigMinimum() {
        assertEquals(List.of(config("log.dirs", "/hadoopfs/fs1/kafka")),
                provider.getRoleConfigs(KAFKA_BROKER, null, source(4, 1, StackType.WORKLOAD))
        );
    }

    @Test
    void getKafkaVolumeConfigWithEqualVolume() {
        HostgroupView hostgroupView = new HostgroupView("test", 2, InstanceGroupType.CORE, 2);
        assertEquals(List.of(config("log.dirs", "/hadoopfs/fs1/kafka,/hadoopfs/fs2/kafka,/hadoopfs/fs3/kafka")),
                provider.getRoleConfigs(KAFKA_BROKER, hostgroupView, source(3, 3, StackType.WORKLOAD))
        );
    }

    @Test
    void getKafkaVolumeConfigWithZeroVolume() {
        assertEquals(List.of(config("log.dirs", "/hadoopfs/root1/kafka")),
                provider.getRoleConfigs(KAFKA_BROKER, null, source(0, 0, StackType.WORKLOAD))
        );
    }

    @Test
    void getKafkaVolumeConfigWithIncorrectRoleType() {
        assertEquals(List.of(),
                provider.getRoleConfigs(KAFKA_SERVICE, null, source(5, 2, StackType.WORKLOAD))
        );
    }

    @Test
    void getKafkaVolumeConfigWithDatalakeStackType() {
        HostgroupView hostgroupView = new HostgroupView("test", 3, InstanceGroupType.CORE, 2);
        assertEquals(List.of(config("log.dirs", "/hadoopfs/fs1/kafka,/hadoopfs/fs2/kafka,/hadoopfs/fs3/kafka")),
                provider.getRoleConfigs(KAFKA_BROKER, hostgroupView, source(5, 2, StackType.DATALAKE))
        );
    }

    @Test
    void getKafkaVolumeConfigWithNullStackType() {
        HostgroupView hostgroupView = new HostgroupView("test", 2, InstanceGroupType.CORE, 2);
        assertEquals(List.of(config("log.dirs", "/hadoopfs/fs1/kafka,/hadoopfs/fs2/kafka")),
                provider.getRoleConfigs(KAFKA_BROKER, hostgroupView, source(0, 0, null))
        );
    }

    private TemplatePreparationObject source(int brokerVolumeCount, int coreBrokerVolumeCount, StackType stackType) {
        HostgroupView broker = VolumeConfigProviderTestHelper.hostGroupWithVolumeCount(brokerVolumeCount);
        HostgroupView coreBroker = VolumeConfigProviderTestHelper.hostGroupWithVolumeCount(coreBrokerVolumeCount);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        when(source.getHostGroupsWithComponent(KAFKA_BROKER)).thenReturn(Stream.of(broker, coreBroker));
        lenient().when(source.getStackType()).thenReturn(stackType);

        return source;
    }
}
