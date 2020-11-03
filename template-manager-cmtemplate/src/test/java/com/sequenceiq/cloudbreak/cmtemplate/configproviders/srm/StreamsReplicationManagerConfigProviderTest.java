package com.sequenceiq.cloudbreak.cmtemplate.configproviders.srm;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_BROKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamsReplicationManagerConfigProviderTest {

    private StreamsReplicationManagerConfigProvider underTest = new StreamsReplicationManagerConfigProvider();

    @Test
    void getServiceConfigsPlain() {
        var source = source(false, "broker-1", "broker-2");
        var expected = List.of(
                config("streams.replication.manager.config", "bootstrap.servers=broker-1:9092,broker-2:9092"),
                config("clusters", "primary,secondary"));
        assertThat(underTest.getServiceConfigs(null, source)).hasSameElementsAs(expected);
    }

    @Test
    void getServiceConfigsSsl() {
        var source = source(true, "broker-1", "broker-2");
        var expected = List.of(
                config("streams.replication.manager.config", "bootstrap.servers=broker-1:9093,broker-2:9093"),
                config("clusters", "primary,secondary"));
        assertThat(underTest.getServiceConfigs(null, source)).hasSameElementsAs(expected);
    }

    @Test
    void getServiceConfigsNoBrokers() {
        var source = source(true);
        assertThat(underTest.getServiceConfigs(null, source)).isEmpty();
    }

    @Test
    void getRoleConfigs() {
        assertThat(underTest.getRoleConfigs("STREAMS_REPLICATION_MANAGER_DRIVER", null)).hasSameElementsAs(
                List.of(config("streams.replication.manager.driver.target.cluster", "primary")));
        assertThat(underTest.getRoleConfigs("STREAMS_REPLICATION_MANAGER_SERVICE", null)).isEqualTo(
                List.of(config("streams.replication.manager.service.target.cluster", "primary")));
    }

    private TemplatePreparationObject source(boolean sslEnabled, String... brokerHosts) {
        var generalClusterConfig = mock(GeneralClusterConfigs.class);
        when(generalClusterConfig.getAutoTlsEnabled()).thenReturn(sslEnabled);

        var hostGroup = mock(HostgroupView.class);
        when(hostGroup.getHosts()).thenReturn(Sets.newTreeSet(Arrays.asList(brokerHosts)));

        var source = mock(TemplatePreparationObject.class);
        when(source.getGeneralClusterConfigs()).thenReturn(generalClusterConfig);
        when(source.getHostGroupsWithComponent(KAFKA_BROKER)).thenReturn(Stream.of(hostGroup));

        return source;
    }

}